package com.gymtracker.di

import android.content.Context
import com.gymtracker.data.GymDatabase
import com.gymtracker.data.GymRepository
import com.gymtracker.settings.SettingsStore
import com.gymtracker.ui.changes.ChangesViewModel
import com.gymtracker.ui.exercise.ExerciseViewModel
import com.gymtracker.ui.history.HistoryViewModel
import com.gymtracker.ui.muscles.MusclesViewModel
import com.gymtracker.ui.workout.PickerViewModel
import com.gymtracker.ui.workout.WorkoutsViewModel
import java.io.File

/** Простой ручной DI: один контейнер на всё приложение, без Hilt. */
class AppContainer(context: Context) {

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
}
