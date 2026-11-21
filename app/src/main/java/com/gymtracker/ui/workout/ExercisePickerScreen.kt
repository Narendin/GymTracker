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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.ui.components.BackHeader
import com.gymtracker.ui.components.ExerciseThumb
import com.gymtracker.ui.components.ScreenTitle
import com.gymtracker.ui.theme.LocalGymPalette

/** A4. Выбор упражнения (вложенный из A2/A3): тап возвращает активным блоком. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExercisePickerScreen(vm: PickerViewModel, onBack: () -> Unit) {
    val state by vm.uiState.collectAsState()
    val palette = LocalGymPalette.current

    Column(Modifier.fillMaxWidth()) {
        BackHeader("← Тренировка", onClick = onBack)
        ScreenTitle("Добавить упражнение", Modifier.padding(top = 4.dp))

        // Поиск находит и по названию, и по мышце.
        Box(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.field)
                .border(1.dp, palette.line, RoundedCornerShape(10.dp)),
        ) {
            BasicTextField(
                value = state.query,
                onValueChange = { vm.setQuery(it) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = palette.text),
                cursorBrush = SolidColor(palette.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .align(Alignment.CenterEnd)
                    .padding(top = 12.dp),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                        if (state.query.isEmpty()) {
                            Text("Поиск по названию", fontSize = 15.sp, color = palette.muted)
                        }
                        inner()
                    }
                },
            )
        }

        // Акцентный выбор фильтра: «Все» (по умолчанию) + чипы шести групп.
        FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PickerChip("Все", active = state.areaId == null, onClick = { vm.selectArea(null) })
            state.areas.forEach { area ->
                PickerChip(
                    area.name,
                    active = area.id == state.areaId,
                    onClick = { vm.selectArea(area.id) },
                )
            }
        }

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.rows, key = { it.id }) { row ->
                PickRowCard(row = row, onClick = { vm.pick(row.id); onBack() })
            }
        }
    }
}

@Composable
private fun PickerChip(label: String, active: Boolean, onClick: () -> Unit) {
    val palette = LocalGymPalette.current
    Box(
        Modifier
            .clip(RoundedCornerShape(17.dp))
            .background(if (active) palette.accent else palette.panel2)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = 13.sp, color = if (active) palette.onAccent else palette.text)
    }
}

@Composable
private fun PickRowCard(row: PickRowUi, onClick: () -> Unit) {
    val palette = LocalGymPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.panel)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExerciseThumb(row.imagePath)
        Column(Modifier.weight(1f)) {
            Text(
                row.name,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = palette.text,
            )
            Text(row.caption, fontSize = 12.sp, color = palette.muted)
        }
    }
}
