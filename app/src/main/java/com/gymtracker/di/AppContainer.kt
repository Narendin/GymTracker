package com.gymtracker.di

import android.content.Context
import com.gymtracker.BuildConfig
import com.gymtracker.data.GymDatabase
import com.gymtracker.data.GymRepository
import com.gymtracker.settings.SettingsStore
import com.gymtracker.ui.changes.ChangesViewModel
import com.gymtracker.ui.exercise.ExerciseViewModel
import com.gymtracker.ui.history.HistoryViewModel
import com.gymtracker.ui.muscles.MusclesViewModel
import com.gymtracker.ui.workout.PickerViewModel
import com.gymtracker.ui.workout.WorkoutsViewModel
import com.gymtracker.update.ApkDownloader
import com.gymtracker.update.ApkInstaller
import com.gymtracker.update.GithubUpdateChecker
import com.gymtracker.update.UpdateChecker
import com.gymtracker.update.UpdateViewModel
import java.io.File

/**
 * Простой ручной DI: один контейнер на всё приложение, без Hilt.
 *
 * [updateChecker] передаётся извне, чтобы автотесты ставили фиксированный
 * ответ проверки обновлений: сеть в тесте недоступна, а состояния
 * «есть версия», «актуальна» и «сеть недоступна» должны проверяться.
 */
class AppContainer(
    context: Context,
    updateChecker: UpdateChecker = GithubUpdateChecker(),
) {

    val settings = SettingsStore(context)

    private val db = GymDatabase.get(context)

    val repository = GymRepository(
        sessionDao = db.sessionDao(),
        catalogDao = db.catalogDao(),
        changeDao = db.changeDao(),
        // Основной каталог фото — приватный каталог приложения, он переживает обновления.
        photosDir = File(context.filesDir, "photos"),
    )

    val workoutsVm = WorkoutsViewModel(repository)
    val pickerVm = PickerViewModel(repository)
    val historyVm = HistoryViewModel(repository)
    val musclesVm = MusclesViewModel(repository)
    val changesVm = ChangesViewModel(repository)
    val exerciseVm = ExerciseViewModel(repository)

    val updateVm = UpdateViewModel(
        checker = updateChecker,
        // Скачанные APK — в приватном кэше: он объявлен в путях FileProvider.
        downloader = ApkDownloader(File(context.cacheDir, "updates")),
        installer = ApkInstaller(context),
        currentVersion = BuildConfig.VERSION_NAME,
        isDebug = BuildConfig.DEBUG,
    )
}
