package com.gymtracker.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.gymtracker.di.AppContainer
import com.gymtracker.ui.changes.ChangesScreen
import com.gymtracker.ui.exercise.ExerciseCardScreen
import com.gymtracker.ui.exercise.ExerciseFormScreen
import com.gymtracker.ui.history.HistoryScreen
import com.gymtracker.ui.icons.muscleIcon
import com.gymtracker.ui.muscles.MusclesScreen
import com.gymtracker.ui.settings.SettingsScreen
import com.gymtracker.ui.theme.LocalGymPalette
import com.gymtracker.ui.workout.ExercisePickerScreen
import com.gymtracker.ui.workout.WorkoutScreen

/** Пять верхних экранов; пункты таб-панели — иконки без текста. */
enum class AppTab(val label: String) {
    WORKOUT("Тренировка"),
    HISTORY("Журнал"),
    MUSCLES("Мышцы"),
    CHANGES("Изменения"),
    SETTINGS("Настройки"),
}

/** Запись истории навигации: система окон работает как история браузера. */
private sealed class Nav {
    data class Tab(val tab: AppTab) : Nav()
    object Picker : Nav()
    data class Card(val id: Long, val backLabel: String) : Nav()
    object Form : Nav()
    data class HistoryDetail(val sessionId: Long) : Nav()
}

/** Окно сворачивается вторым нажатием «Назад»; первое даёт предупреждение. */
private const val DOUBLE_BACK_WINDOW_MS = 2000L

@Composable
fun GymApp(container: AppContainer) {
    val nav = remember { mutableStateListOf<Nav>(Nav.Tab(AppTab.WORKOUT)) }
    val snackbar = remember { SnackbarHostState() }
    val scope = remember { CoroutineScope(Dispatchers.Main.immediate + SupervisorJob()) }
    val activity = LocalContext.current as? Activity

    val top = nav.last()
    val currentTab = (nav.lastOrNull { it is Nav.Tab } as Nav.Tab).tab
    val lowerScreen = top !is Nav.Tab
    var lastBackAt by remember { mutableStateOf(0L) }

    fun pushCard(id: Long, label: String) {
        nav.add(Nav.Card(id, label))
        container.exerciseVm.openCard(id, label)
    }

    fun openDetail(sessionId: Long) {
        nav.add(Nav.HistoryDetail(sessionId))
        container.historyVm.openDetail(sessionId)
    }

    fun pop() {
        if (nav.last() is Nav.HistoryDetail) container.historyVm.closeDetail()
        nav.removeLast()
    }

    BackHandler(enabled = true) {
        if (nav.size == 1) {
            val now = System.currentTimeMillis()
            if (now - lastBackAt < DOUBLE_BACK_WINDOW_MS) {
                activity?.moveTaskToBack(true)
            } else {
                lastBackAt = now
                scope.launch { snackbar.showSnackbar("Повторите действие для сворачивания приложения") }
            }
        } else {
            if (nav.last() is Nav.HistoryDetail) container.historyVm.closeDetail()
            pop()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (!lowerScreen) {
                GymBottomBar(tab = currentTab, onSelect = { selected ->
                    // Таб — переход: добавляем в историю, а не заменяем текущий.
                    // Аккордеон «Мышцы» не сбрасываем: открытая область живёт всю сессию.
                    if (nav.last() != Nav.Tab(selected)) nav.add(Nav.Tab(selected))
                })
            }
        },
    ) { contentPadding ->
        Box(Modifier.padding(contentPadding)) {
            when (val entry = top) {
                is Nav.Form -> ExerciseFormScreen(
                    vm = container.exerciseVm,
                    onBack = ::pop,
                    onSaved = ::pop,
                )
                is Nav.Card -> ExerciseCardScreen(
                    vm = container.exerciseVm,
                    onBack = ::pop,
                    // Карандаш: форма редактирования — тот же экран, что при создании.
                    onEdit = {
                        container.exerciseVm.openFormEdit(entry.id)
                        nav.add(Nav.Form)
                    },
                    // Удаление (мягкое): карточка закрывается, возврат на прежний экран.
                    onDeleted = ::pop,
                    onOpenSession = { sessionId ->
                        pop()
                        openDetail(sessionId)
                    },
                )
                is Nav.Picker -> ExercisePickerScreen(
                    vm = container.pickerVm,
                    onBack = ::pop,
                )
                is Nav.HistoryDetail -> HistoryScreen(
                    vm = container.historyVm,
                    onOpenExercise = { id -> pushCard(id, "← Детали тренировки") },
                    onCloseDetail = ::pop,
                )
                is Nav.Tab -> when (entry.tab) {
                    AppTab.WORKOUT -> WorkoutScreen(
                        vm = container.workoutsVm,
                        onOpenPicker = { nav.add(Nav.Picker) },
                        onOpenExercise = { id -> pushCard(id, "← Тренировка") },
                        // Счётчики A1: таб «Журнал» добавляем в стек, фильтр выставляем до перехода,
                        // «Назад» возвращает на A1, состояние вкладки не сбрасывается.
                        onOpenLastWorkout = { sessionId ->
                            if (nav.last() != Nav.Tab(AppTab.HISTORY)) nav.add(Nav.Tab(AppTab.HISTORY))
                            openDetail(sessionId)
                        },
                        onOpenHistory = { filter ->
                            container.historyVm.setFilter(filter)
                            if (nav.last() != Nav.Tab(AppTab.HISTORY)) nav.add(Nav.Tab(AppTab.HISTORY))
                        },
                    )
                    AppTab.HISTORY -> HistoryScreen(
                        vm = container.historyVm,
                        onOpenExercise = { id -> pushCard(id, "← Детали тренировки") },
                        onOpenDetail = { sessionId -> nav.add(Nav.HistoryDetail(sessionId)) },
                        onCloseDetail = ::pop,
                    )
                    AppTab.MUSCLES -> MusclesScreen(
                        vm = container.musclesVm,
                        onAddExercise = {
                            container.exerciseVm.openFormCreate(container.musclesVm.selectedMuscleId())
                            nav.add(Nav.Form)
                        },
                        onOpenExercise = { id -> pushCard(id, "← Мышцы") },
                    )
                    AppTab.CHANGES -> ChangesScreen(container.changesVm)
                    AppTab.SETTINGS -> SettingsScreen(container.settings)
                }
            }
        }
    }
}

/** Таб-панель: индикатор активной вкладки — акцентная линия над иконкой. */
@Composable
fun GymBottomBar(tab: AppTab, onSelect: (AppTab) -> Unit) {
    val palette = LocalGymPalette.current
    val muscle = remember { muscleIcon() }
    val items = listOf(
        AppTab.WORKOUT to Icons.Outlined.FitnessCenter,
        AppTab.HISTORY to Icons.Outlined.CalendarMonth,
        AppTab.MUSCLES to muscle,
        AppTab.CHANGES to Icons.Outlined.Accessibility,
        AppTab.SETTINGS to Icons.Outlined.Settings,
    )
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(palette.line))
        Row(Modifier.fillMaxWidth().height(66.dp)) {
            items.forEach { (t, icon) ->
                val active = t == tab
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(t) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (active) {
                        Box(
                            Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .height(2.dp)
                                .background(palette.accent),
                        )
                    }
                    Icon(
                        icon,
                        contentDescription = t.label,
                        tint = if (active) palette.accent else palette.muted,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}
