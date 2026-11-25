package com.gymtracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gymtracker.BuildConfig
import com.gymtracker.settings.SettingsStore
import com.gymtracker.settings.ThemeMode
import com.gymtracker.ui.components.OutlinedSmallButton
import com.gymtracker.ui.components.PrimaryButton
import com.gymtracker.ui.components.ScreenTitle
import com.gymtracker.ui.theme.LocalGymPalette
import com.gymtracker.update.UpdateState
import com.gymtracker.update.UpdateViewModel
import com.gymtracker.update.formatReleaseDate
import com.gymtracker.update.updateRowLabel

/** testTag для автотестов: строка «Проверка обновлений». */
const val TAG_UPDATE_ROW = "update_row"

/** D1. Настройки: тема, серые пункты фазы 2, проверка обновлений, о приложении. */
@Composable
fun SettingsScreen(settings: SettingsStore, updateVm: UpdateViewModel) {
    val palette = LocalGymPalette.current
    val mode = settings.themeMode.collectAsState()
    val ui = updateVm.ui.collectAsState()

    Column(Modifier.fillMaxWidth()) {
        ScreenTitle("Настройки")
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            SectionLabel("Тема")
            ThemeOption("Тёмная", ThemeMode.DARK, mode.value, settings)
            ThemeOption("Светлая", ThemeMode.LIGHT, mode.value, settings)
            ThemeOption("Как в системе", ThemeMode.SYSTEM, mode.value, settings)

            SectionLabel("Данные")
            SettingsRow("Экспорт", "фаза 2", enabled = false)
            SettingsRow("Импорт архива", "фаза 2", enabled = false)
            SettingsRow(
                "Проверка обновлений",
                updateRowLabel(ui.value.state, BuildConfig.VERSION_NAME),
                enabled = true,
                onClick = { updateVm.check() },
                modifier = Modifier.testTag(TAG_UPDATE_ROW),
            )

            SectionLabel("О приложении")
            SettingsRow("Версия", BuildConfig.VERSION_NAME, enabled = true)
            Spacer(Modifier.height(16.dp))
        }
    }

    if (ui.value.dialogOpen) {
        UpdateDialog(ui.value.state, BuildConfig.DEBUG, updateVm)
    }
}

/**
 * Диалог результата проверки: состояние, описание релиза и действие.
 *
 * Кнопка меняет смысл по состоянию: повторить запрос, скачать APK, открыть
 * системные настройки разрешения или повторить установку после настроек.
 */
@Composable
private fun UpdateDialog(state: UpdateState, isDebug: Boolean, vm: UpdateViewModel) {
    val palette = LocalGymPalette.current

    val title: String
    val message: String
    val primaryText: String?
    val primaryAction: (() -> Unit)?
    val secondaryText: String
    val secondaryAction: () -> Unit
    val showDebugWarning: Boolean

    val dismiss: () -> Unit = { vm.dismiss() }
    val check: () -> Unit = { vm.check() }
    val download: () -> Unit = { vm.downloadAndInstall() }
    val openSettings: () -> Unit = { vm.openPermissionSettings() }
    val retry: () -> Unit = { vm.retryInstall() }

    when (state) {
        UpdateState.Checking -> {
            title = "Проверка обновлений"
            message = "Обращаемся к релизам…"
            primaryText = null
            primaryAction = null
            secondaryText = "Отмена"
            secondaryAction = dismiss
            showDebugWarning = false
        }
        UpdateState.UpToDate -> {
            title = "Версия актуальна"
            message = "Установлена последняя версия ${BuildConfig.VERSION_NAME}."
            primaryText = null
            primaryAction = null
            secondaryText = "Закрыть"
            secondaryAction = dismiss
            showDebugWarning = false
        }
        UpdateState.NetworkUnavailable -> {
            title = "Сеть недоступна"
            message = "Не удалось обратиться к релизам. Проверьте соединение."
            primaryText = "Повторить"
            primaryAction = check
            secondaryText = "Закрыть"
            secondaryAction = dismiss
            showDebugWarning = false
        }
        UpdateState.UnreadableResponse -> {
            title = "Ответ не читается"
            message = "Ответ релизной стороны не содержит номера версии и ссылки на APK."
            primaryText = "Повторить"
            primaryAction = check
            secondaryText = "Закрыть"
            secondaryAction = dismiss
            showDebugWarning = false
        }
        is UpdateState.NewVersion -> {
            title = "Есть версия ${state.release.tag}"
            val date = formatReleaseDate(state.release.publishedAt)
            message = if (date.isNotBlank()) "Опубликовано $date.\n\n${state.release.notes}" else state.release.notes
            primaryText = "Скачать и установить"
            primaryAction = download
            secondaryText = "Позже"
            secondaryAction = dismiss
            showDebugWarning = isDebug
        }
        UpdateState.Downloading -> {
            title = "Скачивание"
            message = "Скачиваем APK во внутренний каталог приложения…"
            primaryText = null
            primaryAction = null
            secondaryText = "Отмена"
            secondaryAction = dismiss
            showDebugWarning = false
        }
        UpdateState.DownloadFailed -> {
            title = "Скачивание не удалось"
            message = "Файл не скачался. Проверьте соединение и повторите."
            primaryText = "Повторить"
            primaryAction = download
            secondaryText = "Закрыть"
            secondaryAction = dismiss
            showDebugWarning = false
        }
        is UpdateState.ReadyToInstall -> {
            // Разрешения нет: установка невозможна, ведём человека в настройки.
            val needPermission = !state.canInstall && !state.blockedByDebug
            title = "APK скачан"
            message = when {
                state.blockedByDebug ->
                    "Отладочную сборку релизная версия не обновит: ключи подписи разные. " +
                        "Скачанный APK не устанавливается, установите его из GitHub Releases вручную."
                state.canInstall ->
                    "Открыт системный установщик. Подтвердите установку — журнал и настройки сохранятся."
                else ->
                    "Нужно разрешение на установку из приложения. Откройте системные настройки, " +
                        "включите его и повторите установку."
            }
            primaryText = if (needPermission) "Открыть настройки" else null
            primaryAction = if (needPermission) openSettings else null
            secondaryText = if (needPermission) "Повторить установку" else "Закрыть"
            secondaryAction = if (needPermission) retry else dismiss
            showDebugWarning = isDebug && !state.blockedByDebug
        }
        UpdateState.Idle -> {
            title = ""
            message = ""
            primaryText = null
            primaryAction = null
            secondaryText = "Закрыть"
            secondaryAction = dismiss
            showDebugWarning = false
        }
    }

    Dialog(onDismissRequest = dismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.panel2)
                .padding(16.dp),
        ) {
            Text(
                title,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.text,
            )
            Text(
                message,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                fontSize = 14.sp,
                color = palette.muted,
            )
            if (showDebugWarning) {
                Text(
                    "Отладочную сборку релизная версия не обновит: ключи подписи разные.",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    fontSize = 14.sp,
                    color = palette.danger,
                )
            }
            if (primaryText != null && primaryAction != null) {
                PrimaryButton(primaryText, onClick = primaryAction)
                Spacer(Modifier.height(8.dp))
            }
            OutlinedSmallButton(secondaryText, onClick = secondaryAction)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val palette = LocalGymPalette.current
    Text(
        text,
        fontSize = 13.sp,
        color = palette.muted,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun ThemeOption(label: String, mode: ThemeMode, current: ThemeMode, settings: SettingsStore) {
    val palette = LocalGymPalette.current
    val selected = mode == current
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.panel)
            .clickable { settings.setThemeMode(mode) }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 17.sp, color = palette.text)
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (selected) palette.accent else palette.panel2),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(palette.onAccent))
            }
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    value: String,
    enabled: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val palette = LocalGymPalette.current
    val clickable = onClick != null && enabled
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.panel)
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            fontSize = 17.sp,
            color = if (enabled) palette.text else palette.muted,
        )
        Spacer(Modifier.weight(1f))
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = if (enabled) FontWeight.SemiBold else FontWeight.Normal,
            color = if (enabled) palette.accent else palette.muted,
        )
    }
}
