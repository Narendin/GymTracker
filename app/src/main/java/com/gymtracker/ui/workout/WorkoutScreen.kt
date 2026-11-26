package com.gymtracker.ui.workout

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gymtracker.ui.components.ConfirmDialog
import com.gymtracker.ui.components.CommentDialog
import com.gymtracker.ui.components.CommentRow
import com.gymtracker.ui.components.DangerSmallButton
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
import com.gymtracker.util.formatSet
import com.gymtracker.util.workoutsLabel

/**
 * testTag для автотестов: элементы находятся по тегу и тексту,
 * координаты не используются. Теги — часть основного кода.
 */
const val TAG_LAST = "last_workout"
const val TAG_MONTH = "count_30"
const val TAG_TOTAL = "total_workouts"
const val TAG_REPS = "reps_input"
const val TAG_WEIGHT = "weight_input"
const val TAG_EDIT_WEIGHT = "edit_weight"
const val TAG_EDIT_REPS = "edit_reps"
const val TAG_CHEVRON = "block_chevron"
const val TAG_BLOCK_ROW = "block_row"
/** Строка комментария тренировки (над блоками) и строка комментария блока упражнения. */
const val TAG_WORKOUT_COMMENT = "workout_comment"
const val TAG_BLOCK_COMMENT = "block_comment"

/** Экран «Тренировка»: A1 — нет активной сессии; A2/A3/A3a — сессия. */
@Composable
fun WorkoutScreen(
    vm: WorkoutsViewModel,
    onOpenPicker: () -> Unit,
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
                // A3/A3a. Блоки в порядке добавления; каждое раскрытие задаёт шеврон,
                // активный блок по умолчанию раскрыт. Комментарий тренировки — над блоками.
                CommentRow(
                    text = session.comment,
                    onClick = { vm.openComment(null) },
                    testTag = TAG_WORKOUT_COMMENT,
                )
                session.blocks.forEach { block ->
                    ExerciseBlockCard(
                        block = block,
                        session = session,
                        vm = vm,
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
    // Диалог правки подхода.
    if (state.editSetId != null) {
        EditSetDialog(vm, state)
    }
    // Диалог комментария: тренировка или блок упражнения.
    val comment = state.commentDialog
    if (comment != null) {
        // Строка читает наблюдаемое состояние экрана, как диалог правки подхода.
        val sessionState = state.session
        val saved = if (comment.isSession) {
            sessionState?.comment
        } else {
            sessionState?.blocks?.firstOrNull { it.blockId == comment.blockId }?.comment
        }
        CommentDialog(
            title = if (comment.isSession) "Комментарий тренировки" else "Комментарий упражнения",
            value = comment.text,
            onValueChange = { vm.setCommentText(it) },
            onSave = { vm.saveComment() },
            onClear = { vm.clearComment() },
            onCancel = { vm.cancelComment() },
            canDelete = saved != null,
        )
    }
}

/**
 * Карточка блока тренировки: свёрнутый — одна строка (миниатюра, название, шеврон),
 * у активного блока строка с акцентной рамкой. Раскрытый блок показывает подходы,
 * у активного ещё поля «Повторы»/«Вес» и кнопки.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseBlockCard(
    block: BlockUi,
    session: SessionUi,
    vm: WorkoutsViewModel,
) {
    val palette = LocalGymPalette.current
    val expanded = block.isExpanded
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        label = "chevron",
    )

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
                .testTag(TAG_BLOCK_ROW)
                .clickable {
                    // Строка блока переключает раскрытие — одинаково для
                    // активного и завершённого блока; подход тап по строке не добавляет.
                    vm.toggleExpand(block.blockId)
                },
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
            Text(
                "›",
                fontSize = 17.sp,
                color = palette.muted,
                modifier = Modifier
                    .testTag(TAG_CHEVRON)
                    .graphicsLayer(rotationZ = chevronRotation)
                    .clickable { vm.toggleExpand(block.blockId) },
            )
        }

        if (expanded) {
            if (block.sets.isNotEmpty()) {
                FlowRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    block.sets.forEach { set ->
                        val chipLabel = formatSet(set.weightKg, set.reps, block.isBodyweight)
                        SetChip(
                            chipLabel,
                            onClick = { vm.openEditSet(block.blockId, set.id, set.weightKg, set.reps, block.isBodyweight) },
                        )
                    }
                }
            }
            // Комментарий упражнения — под чипами; в свёрнутом блока его нет,
            // тап по строке открывает диалог правки.
            CommentRow(
                text = block.comment,
                onClick = { vm.openComment(block.blockId) },
                testTag = TAG_BLOCK_COMMENT,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
            )
            if (block.showResume) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                ) {
                    RoundIconButton("↻", onClick = { vm.resumeBlock(block.blockId) })
                }
            }
        }

        if (block.isActive && expanded) {
            // Раскрытый активный блок: поля ввода и кнопки. В свёрнутом они скрыты,
            // введенные значения при этом остаются в состоянии.
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
            ) {
                if (block.isBodyweight) {
                    NumericField(
                        label = "Повторы",
                        value = session.repsInput,
                        onValueChange = { vm.setRepsInput(it) },
                        accentBorder = false,
                        testTag = TAG_REPS,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
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
                }
                Spacer(Modifier.height(10.dp))
                PrimaryButton("Добавить подход", onClick = { vm.addSet() })
                Spacer(Modifier.height(8.dp))
                OutlinedSmallButton("Завершить упражнение", onClick = { vm.finishExercise(block.blockId) })
            }
        }
    }
}

/**
 * Диалог правки подхода: поля «Вес» и «Повторы», кнопки «Сохранить»,
 * «Удалить», «Отмена».
 *
 * Состояние приходит параметром из наблюдаемого `collectAsState()` экрана.
 * Читать `vm.uiState.value` внутри диалога нельзя: composable с неизменным
 * `vm` не пересобирается, поле ввода получает старое значение и набитый текст
 * исчезает (на клавиатуру «не реагирует»).
 */
@Composable
private fun EditSetDialog(vm: WorkoutsViewModel, state: WorkoutUiState) {
    val palette = LocalGymPalette.current
    Dialog(onDismissRequest = { vm.cancelEditSet() }) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.panel2)
                .padding(16.dp),
        ) {
            Text(
                "Правка подхода",
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = palette.text,
            )
            if (state.editIsBodyweight) {
                NumericField(
                    label = "Повторы",
                    value = state.editReps,
                    onValueChange = { vm.setEditReps(it) },
                    accentBorder = false,
                    testTag = TAG_EDIT_REPS,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NumericField(
                        label = "Вес",
                        value = state.editWeight,
                        onValueChange = { vm.setEditWeight(it) },
                        accentBorder = false,
                        testTag = TAG_EDIT_WEIGHT,
                        modifier = Modifier.weight(1f),
                    )
                    NumericField(
                        label = "Повторы",
                        value = state.editReps,
                        onValueChange = { vm.setEditReps(it) },
                        accentBorder = false,
                        testTag = TAG_EDIT_REPS,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            PrimaryButton("Сохранить", onClick = { vm.saveEditSet() })
            Spacer(Modifier.height(8.dp))
            DangerSmallButton("Удалить", onClick = { vm.deleteSet() })
            Spacer(Modifier.height(8.dp))
            OutlinedSmallButton("Отмена", onClick = { vm.cancelEditSet() })
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
