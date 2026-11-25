package com.gymtracker.ui.history

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.ui.components.BackHeader
import com.gymtracker.ui.components.DangerSmallButton
import com.gymtracker.ui.components.ExerciseThumb
import com.gymtracker.ui.components.OutlinedSmallButton
import com.gymtracker.ui.components.PrimaryButton
import com.gymtracker.ui.components.ScreenSub
import com.gymtracker.ui.components.ScreenTitle
import com.gymtracker.ui.components.SetChip
import com.gymtracker.ui.components.TextConfirmDialog
import com.gymtracker.ui.theme.LocalGymPalette

/** testTag для автотестов: строка журнала; порядок строк — по дате (свежая сверху). */
const val TAG_ROW = "journal_row"

/**
 * B1 — Журнал; B2 — детали тренировки.
 * onOpenDetail/onCloseDetail — синхронизация с историей навигации: экран
 * добавляется в стек и снимается с него, чтобы «Назад» возвращал на предыдущее окно.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    vm: HistoryViewModel,
    onOpenExercise: (Long) -> Unit,
    onOpenDetail: (Long) -> Unit = { },
    onCloseDetail: () -> Unit = { },
    onDeleted: () -> Unit = { },
) {
    val state by vm.uiState.collectAsState()
    val palette = LocalGymPalette.current

    if (state.detail == null) {
        Column(Modifier.fillMaxWidth()) {
            ScreenTitle("Журнал")
            FlowRow(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PeriodChip("Неделя", PeriodFilter.WEEK, state.filter, vm)
                PeriodChip("Месяц", PeriodFilter.MONTH, state.filter, vm)
                PeriodChip("Год", PeriodFilter.YEAR, state.filter, vm)
                PeriodChip("Всё", PeriodFilter.ALL, state.filter, vm)
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.rows.isEmpty()) {
                    Text(
                        "Пока нет ни одной записанной тренировки.",
                        fontSize = 15.sp,
                        color = palette.muted,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                state.rows.forEach { row ->
                    HistoryRowCard(row = row, onClick = {
                        vm.openDetail(row.sessionId)
                        onOpenDetail(row.sessionId)
                    })
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    } else {
        val detail = state.detail ?: return
        Column(Modifier.fillMaxWidth()) {
            BackHeader("← Журнал", onClick = {
                vm.closeDetail()
                onCloseDetail()
            })
            ScreenTitle(detail.dateLabel, Modifier.padding(top = 4.dp))
            ScreenSub(detail.subLabel)
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                detail.blocks.forEach { block ->
                    HistoryBlockCard(block = block, onOpenExercise = onOpenExercise)
                }
                DangerSmallButton("Удалить тренировку", onClick = { vm.requestDelete() })
                Spacer(Modifier.height(16.dp))
            }
        }
        if (state.showDeleteConfirm) {
            TextConfirmDialog(
                title = "Удалить тренировку?",
                message = "Запись и её подходы будут удалены.",
                primaryText = "Удалить",
                secondaryText = "Отмена",
                onPrimary = { vm.confirmDelete(); onDeleted() },
                onSecondary = { vm.cancelDelete() },
            )
        }
    }
}

@Composable
private fun PeriodChip(label: String, filter: PeriodFilter, current: PeriodFilter, vm: HistoryViewModel) {
    val palette = LocalGymPalette.current
    val active = filter == current
    Box(
        Modifier
            // Признак выбора — часть семантики: автотест проверяет выбранный фильтр.
            .semantics { selected = active }
            .clip(RoundedCornerShape(17.dp))
            .background(if (active) palette.accent else palette.panel2)
            .clickable { vm.setFilter(filter) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = if (active) palette.onAccent else palette.text,
        )
    }
}

@Composable
private fun HistoryRowCard(row: HistoryRowUi, onClick: () -> Unit) {
    val palette = LocalGymPalette.current
    Column(
        Modifier
            .testTag(TAG_ROW)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.panel)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(row.dateLabel, fontSize = 17.sp, color = palette.text)
        // Основные мышцы тренировки — по ним видно, чем тренировались.
        if (row.musclesLabel.isNotEmpty()) {
            Text(
                row.musclesLabel,
                fontSize = 12.sp,
                color = palette.muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(row.exercisesLabel, fontSize = 13.sp, color = palette.muted)
            Spacer(Modifier.weight(1f))
            Text(
                row.tonnageLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.accent,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryBlockCard(block: HistoryBlockUi, onOpenExercise: (Long) -> Unit) {
    val palette = LocalGymPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.panel),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 12.dp)
                .clickable { onOpenExercise(block.exerciseId) },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseThumb(block.imagePath)
                Spacer(Modifier.width(8.dp))
                Text(
                    block.name,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = palette.text,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            block.chips.forEach { label ->
                SetChip(label)
            }
        }
    }
}
