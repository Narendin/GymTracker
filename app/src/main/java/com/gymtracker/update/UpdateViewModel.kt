package com.gymtracker.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Состояние проверки обновлений: от исходной строки до скачанного APK.
 */
sealed class UpdateState {
    /** Строка не тронута: показывать текущую версию. */
    object Idle : UpdateState()

    /** Запрос отправлен, ответ не пришёл. */
    object Checking : UpdateState()

    /** Есть версия новее текущей. */
    data class NewVersion(val release: Release) : UpdateState()

    /** Установлена последняя версия. */
    object UpToDate : UpdateState()

    /** Сеть недоступна: запрос не прошёл. */
    object NetworkUnavailable : UpdateState()

    /** Ответ пришёл, номер и ссылку разобрать не удалось. */
    object UnreadableResponse : UpdateState()

    /** APK скачивается. */
    object Downloading : UpdateState()

    /** Скачивание оборвалось. */
    object DownloadFailed : UpdateState()

    /**
     * APK скачан. [canInstall] — есть ли разрешение на установку;
     * [blockedByDebug] — отладочную сборку релизная версия не обновит,
     * ключи подписи разные, установку не запускаем.
     */
    data class ReadyToInstall(val canInstall: Boolean, val blockedByDebug: Boolean) : UpdateState()
}

/**
 * Экран проверки обновлений: состояние результата и открытость диалога.
 *
 * Диалог закрывается, результат остаётся: строка в настройках продолжает
 * показывать, чем закончилась последняя проверка.
 */
data class UpdateUiState(
    val state: UpdateState = UpdateState.Idle,
    val dialogOpen: Boolean = false,
)

/**
 * Проверка обновлений и установка новой версии.
 *
 * Проверка идёт по нажатию кнопки в настройках: приложение обращается к
 * релизной стороне один раз, без аккаунта и без данных журнала. Остальное
 * время приложение работает офлайн.
 */
class UpdateViewModel(
    private val checker: UpdateChecker,
    private val downloader: ApkDownloader,
    private val installer: ApkInstaller,
    private val currentVersion: String,
    private val isDebug: Boolean,
) : ViewModel() {

    private val _ui = MutableStateFlow(UpdateUiState())
    val ui: StateFlow<UpdateUiState> = _ui.asStateFlow()

    /** Скачанный APK: он нужен, чтобы повторить установку после настроек. */
    private var downloadedFile: File? = null

    /** Запросить последнюю версию и показать результат. */
    fun check() {
        viewModelScope.launch {
            _ui.value = UpdateUiState(UpdateState.Checking, dialogOpen = true)
            _ui.value = UpdateUiState(
                state = when (val result = checker.check(currentVersion)) {
                    is UpdateResult.NewVersion -> UpdateState.NewVersion(result.release)
                    UpdateResult.UpToDate -> UpdateState.UpToDate
                    UpdateResult.NetworkError -> UpdateState.NetworkUnavailable
                    UpdateResult.UnreadableResponse -> UpdateState.UnreadableResponse
                },
                dialogOpen = true,
            )
        }
    }

    /** Скачать APK и передать его системному установщику. */
    fun downloadAndInstall() {
        val release = (_ui.value.state as? UpdateState.NewVersion)?.release ?: return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(state = UpdateState.Downloading)
            val file = withContext(Dispatchers.IO) { downloader.download(release) }
            if (file == null) {
                _ui.value = _ui.value.copy(state = UpdateState.DownloadFailed)
                return@launch
            }
            downloadedFile = file
            val canInstall = installer.canInstall()
            _ui.value = _ui.value.copy(
                state = UpdateState.ReadyToInstall(canInstall = canInstall, blockedByDebug = isDebug),
            )
            if (canInstall && !isDebug) installer.install(file)
        }
    }

    /** Экран системных настроек с разрешением на установку. */
    fun openPermissionSettings() {
        installer.openPermissionSettings()
    }

    /** Повторить установку: после возвращения из системных настроек. */
    fun retryInstall() {
        val file = downloadedFile
        // Файл теряется, если приложение было закрыто: скачивание нужно заново.
        if (file == null) {
            _ui.value = _ui.value.copy(state = UpdateState.DownloadFailed)
            return
        }
        viewModelScope.launch {
            val canInstall = installer.canInstall()
            _ui.value = _ui.value.copy(
                state = UpdateState.ReadyToInstall(canInstall = canInstall, blockedByDebug = isDebug),
            )
            if (canInstall && !isDebug) installer.install(file)
        }
    }

    /** Закрыть диалог: результат остаётся в строке настроек. */
    fun dismiss() {
        _ui.value = _ui.value.copy(dialogOpen = false)
    }
}

/** Подпись строки «Проверка обновлений» по текущему состоянию. */
fun updateRowLabel(state: UpdateState, currentVersion: String): String = when (state) {
    UpdateState.Idle -> currentVersion
    UpdateState.Checking -> "проверка…"
    is UpdateState.NewVersion -> state.release.tag
    UpdateState.UpToDate -> "актуальна"
    UpdateState.NetworkUnavailable -> "сеть недоступна"
    UpdateState.UnreadableResponse -> "нет ответа"
    UpdateState.Downloading -> "скачивание…"
    UpdateState.DownloadFailed -> "не скачалось"
    is UpdateState.ReadyToInstall -> when {
        state.blockedByDebug -> "вручную"
        state.canInstall -> "готов"
        else -> "разрешение"
    }
}
