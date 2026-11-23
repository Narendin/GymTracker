package com.gymtracker.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.ui.components.ConfirmDialog
import com.gymtracker.ui.components.ExerciseThumb
import com.gymtracker.ui.components.OutlinedButton
import com.gymtracker.ui.components.OutlinedSmallButton
import com.gymtracker.ui.components.PrimaryButton
import com.gymtracker.ui.components.RoundIconButton
import com.gymtracker.ui.components.ScreenSub
import com.gymtracker.ui.components.ScreenTitle
import com.gymtracker.ui.components.SetChip
import com.gymtracker.ui.components.StatRow
import com.gymtracker.ui.history.PeriodFilter
import com.gymtracker.ui.theme.LocalGymPalette
import com.gymtracker.util.exercisesLabel
import com.gymtracker.util.formatWeight
import com.gymtracker.util.workoutsLabel

/**
 * testTag для автотестов (issue #11): элементы находятся по тегу и тексту,
 * координаты не используются. Теги — часть основного кода.
 */
const val TAG_LAST = "last_workout"
const val TAG_MONTH = "count_30"
const val TAG_TOTAL = "total_workouts"
const val TAG_REPS = "reps_input"
const val TAG_WEIGHT = "weight_input"

/** Экран «Тренировка»: A1 — нет активной сессии; A2/A3/A3a — сессия. */
@Composable
fun WorkoutScreen(
    vm: WorkoutsViewModel,
    onOpenPicker: () -> Unit,
    onOpenExercise: (Long) -> Unit,
    /** Счётчики A1 — переходы в Журнал: детали последней тренировки и фильтр периода. */
    onOpenLastWorkout: (Long) -> Unit = { },
    onOpenHistory: (PeriodFilter) -> Unit = { },
) {
    val state by vm.uiState.collectAsState()
    val palette = LocalGymPalette.current

    Column(Modifier.fillMaxWidth().fillMaxHeight()) {
        ScreenTitle("Тренировка")
        val session = state.session
        val sub = if (session == null || session.blocks.isEmpty()) {
            state.todayLabel
        } else {
            "${state.todayLabel} · ${exercisesLabel(session.exerciseCount)}"
        }
        ScreenSub(sub)

        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (session == null) {
                // A1. Нет активной сессии: счётчики, которые реально меняются, и кнопка старта.
                // Счётчики — кнопки перехода в Журнал; без тренировок «Последняя тренировка» неактивна.
                Spacer(Modifier.height(6.dp))
                val lastId = state.lastSessionId
                StatRow(
                    "Последняя тренировка",
                    state.lastWorkoutLabel ?: "—",
                    modifier = Modifier.testTag(TAG_LAST),
                    onClick = lastId?.let { id -> { onOpenLastWorkout(id) } },
                )
                StatRow(
                    "За 30 дней",
                    workoutsLabel(state.count30),
                    modifier = Modifier.testTag(TAG_MONTH),
                    onClick = { onOpenHistory(PeriodFilter.MONTH) },
                )
                StatRow(
                    "Всего тренировок",
                    state.totalCount.toString(),
                    modifier = Modifier.testTag(TAG_TOTAL),
                    onClick = { onOpenHistory(PeriodFilter.ALL) },
                )
                Spacer(Modifier.height(6.dp))
                PrimaryButton("Начать тренировку", onClick = { vm.startSession() })
            } else if (session.blocks.isEmpty()) {
                // A2. Сессия начата, упражнений нет: обе кнопки доступны.
                Text(
                    "Пока пусто. Добавьте первое упражнение.",
                    fontSize = 15.sp,
                    color = palette.text,
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                )
                PrimaryButton("Добавить упражнение", onClick = onOpenPicker)
                OutlinedButton("Завершить тренировку", onClick = { vm.completeWorkout() })
            } else {
                // A3/A3a. Блоки в порядке добавления; активный раскрыт, завершённые свёрнуты.
                session.blocks.forEach { block ->
                    ExerciseBlockCard(
                        block = block,
                        session = session,
                        vm = vm,
                        onOpenExercise = onOpenExercise,
                    )
                }
                if (session.activeBlockId == null) {
                    // A3a: нет активного упражнения — нижние кнопки возвращаются.
                    PrimaryButton("Добавить упражнение", onClick = onOpenPicker)
                    OutlinedButton("Завершить тренировку", onClick = { vm.completeWorkout() })
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    // A5: подтверждение завершения — только при наличии упражнений.
    if (state.showFinishConfirm) {
        ConfirmDialog(
            title = "Завершить тренировку?",
            primaryText = "Завершить",
            secondaryText = "Отмена",
            onPrimary = { vm.confirmComplete() },
            onSecondary = { vm.cancelComplete() },
        )
    }
    // A6: «Подходов нет».
    if (state.noSetsBlockId != null) {
        ConfirmDialog(
            title = "Подходов нет",
            primaryText = "Завершить без записи",
            secondaryText = "Продолжить упражнение",
            onPrimary = { vm.confirmNoSets() },
            onSecondary = { vm.continueExercise() },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseBlockCard(
    block: BlockUi,
    session: SessionUi,
    vm: WorkoutsViewModel,
    onOpenExercise: (Long) -> Unit,
) {
    val palette = LocalGymPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.panel)
            .then(
                if (block.isActive) Modifier.border(1.dp, palette.accent, RoundedCornerShape(14.dp))
                else Modifier
            )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp)
                .clickable { onOpenExercise(block.exerciseId) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ExerciseThumb(block.imagePath)
            Text(
                block.name,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = palette.text,
                modifier = Modifier.weight(1f),
            )
            if (block.showResume) {
                RoundIconButton("↻", onClick = { vm.resumeBlock(block.blockId) })
            }
        }

        if (block.sets.isNotEmpty()) {
            FlowRow(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                block.sets.forEach { set ->
                    SetChip("${formatWeight(set.weightKg)} кг × ${set.reps}")
                }
            }
        }

        if (block.isActive) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NumericField(
                        label = "Повторы",
                        value = session.repsInput,
                        onValueChange = { vm.setRepsInput(it) },
                        accentBorder = false,
                        testTag = TAG_REPS,
                        modifier = Modifier.weight(1f),
                    )
                    NumericField(
                        label = "Вес",
                        value = session.weightInput,
                        onValueChange = { vm.setWeightInput(it) },
                        accentBorder = session.weightPrefilled,
                        testTag = TAG_WEIGHT,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                PrimaryButton("Добавить подход", onClick = { vm.addSet() })
                Spacer(Modifier.height(8.dp))
                OutlinedSmallButton("Завершить упражнение", onClick = { vm.finishExercise(block.blockId) })
            }
        }
    }
}

/** Поле ввода: равной шириной, вся строка, высота 54; подставленный вес — акцентная рамка. */
@Composable
private fun NumericField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    accentBorder: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalGymPalette.current
    Column(modifier) {
        Text(
            label,
            fontSize = 12.sp,
            color = palette.muted,
            modifier = Modifier.padding(bottom = 3.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
            ),
            textStyle = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.text,
                textAlign = TextAlign.Center,
            ),
            cursorBrush = SolidColor(palette.accent),
            modifier = Modifier
                .testTag(testTag)
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(palette.field)
                .border(
                    1.dp,
                    if (accentBorder) palette.accent else palette.line,
                    RoundedCornerShape(9.dp),
                ),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize().padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                    inner()
                }
            },
        )
    }
}
