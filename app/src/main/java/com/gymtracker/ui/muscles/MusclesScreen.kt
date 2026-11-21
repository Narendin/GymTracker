package com.gymtracker.ui.muscles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.ui.components.ExerciseThumb
import com.gymtracker.ui.components.OutlinedButton
import com.gymtracker.ui.components.ScreenTitle
import com.gymtracker.ui.theme.LocalGymPalette

/** C1. Мышцы — аккордеон областей; открыта одна область, выбрана одна мышца. */
@Composable
fun MusclesScreen(
    vm: MusclesViewModel,
    onAddExercise: () -> Unit,
    onOpenExercise: (Long) -> Unit,
) {
    val state by vm.uiState.collectAsState()
    val palette = LocalGymPalette.current

    Column(Modifier.fillMaxWidth()) {
        ScreenTitle("Мышцы")
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.areas.forEach { area ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.panel),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(if (area.open) palette.accent else palette.panel)
                            .clickable { vm.openArea(area.id) },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            area.name,
                            fontSize = 17.sp,
                            fontWeight = if (area.open) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (area.open) palette.onAccent else palette.text,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                    if (area.open) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            // Формально FlowRow; чипов мышц в области немного (ряд в одну строку).
                            area.muscles.forEach { muscle ->
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(15.dp))
                                        .background(if (muscle.selected) palette.accent else palette.panel2)
                                        .clickable { vm.selectMuscle(muscle.id) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        muscle.name,
                                        fontSize = 13.sp,
                                        color = if (muscle.selected) palette.onAccent else palette.text,
                                    )
                                }
                            }
                        }
                        // Названия мышц у строк упражнений не подписываем.
                        // Строки и блок «Скрытое» приходят уже отсортированными по алфавиту.
                        state.exercises.forEach { row ->
                            ExerciseRow(
                                row = row,
                                onOpen = { onOpenExercise(row.id) },
                                onToggleHidden = { vm.toggleExerciseHidden(row.id) },
                            )
                        }
                        // Блок «Скрытое»: есть скрытые упражнения — показываем заголовок, свёрнут по умолчанию.
                        if (state.hiddenExercises.isNotEmpty()) {
                            HiddenSectionHeader(
                                count = state.hiddenExercises.size,
                                expanded = state.hiddenExpanded,
                                onToggle = vm::toggleHiddenSection,
                            )
                            if (state.hiddenExpanded) {
                                state.hiddenExercises.forEach { row ->
                                    ExerciseRow(
                                        row = row,
                                        onOpen = { onOpenExercise(row.id) },
                                        onToggleHidden = { vm.toggleExerciseHidden(row.id) },
                                    )
                                }
                            }
                        }
                        Column(Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp)) {
                            OutlinedButton("Добавить упражнение", onClick = onAddExercise)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Строка упражнения: миниатюра, название и справа полупрозрачная иконка глаза.
 * Тап по строке открывает карточку, тап по иконке скрывает упражнение
 * (или возвращает его из блока «Скрытое»).
 */
@Composable
private fun ExerciseRow(
    row: MuscleRowUi,
    onOpen: () -> Unit,
    onToggleHidden: () -> Unit,
) {
    val palette = LocalGymPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExerciseThumb(row.imagePath)
        Text(
            row.name,
            fontSize = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = if (row.hidden) palette.muted else palette.text,
            modifier = Modifier.weight(1f),
        )
        EyeButton(
            hidden = row.hidden,
            onClick = onToggleHidden,
            tint = if (row.hidden) palette.muted else palette.text.copy(alpha = 0.45f),
        )
    }
}

/** Иконка глаза / зачеркнутого глаза; тап меняет видимость упражнения. */
@Composable
private fun EyeButton(hidden: Boolean, onClick: () -> Unit, tint: Color) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (hidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = if (hidden) "Вернуть упражнение" else "Скрыть упражнение",
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Заголовок блока «Скрытое»: тап по слову раскрывает и сворачивает список. */
@Composable
private fun HiddenSectionHeader(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    val palette = LocalGymPalette.current
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(1.dp)
                .background(palette.line),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "Скрытое",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.muted,
            )
            Text("($count)", fontSize = 12.sp, color = palette.muted)
            Text(if (expanded) "▾" else "▸", fontSize = 12.sp, color = palette.muted)
        }
    }
}
