package com.gymtracker.ui.exercise

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gymtracker.ui.components.OutlinedButton
import com.gymtracker.ui.components.PrimaryButton
import com.gymtracker.ui.components.ScreenTitle
import com.gymtracker.ui.theme.LocalGymPalette
import java.io.File

/** testTag для автотестов. */
const val TAG_FORM_NAME = "form_name"
const val TAG_FORM_CHIP = "form_chip"
const val TAG_ADD_PRIMARY = "add_primary"
const val TAG_ADD_SECONDARY = "add_secondary"
const val TAG_SAVE = "form_save"

/** C2/C3c — форма упражнения: фото, название, основные и вторичные мышцы, «Сохранить». */
@Composable
fun ExerciseFormScreen(
    vm: ExerciseViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val state by vm.formState.collectAsState()
    val palette = LocalGymPalette.current
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(Picker.None) }
    val editing = state.editingId != null

    Column(Modifier.fillMaxWidth()) {
        BackHeaderLocal(
            if (editing) "← Карточка упражнения" else "← Мышцы",
            onClick = onBack,
        )
        ScreenTitle(if (editing) "Редактировать упражнение" else "Новое упражнение")

        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.photo)
                    .clickable { showSheet = true },
                contentAlignment = Alignment.Center,
            ) {
                if (state.photoPath != null) {
                    AsyncImage(
                        model = File(state.photoPath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(1.dp, palette.accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.foundation.Canvas(Modifier.size(48.dp)) {
                        val r = size.minDimension / 2f
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val arm = r * 0.62f
                        drawLine(palette.accent, androidx.compose.ui.geometry.Offset(cx - arm, cy), androidx.compose.ui.geometry.Offset(cx + arm, cy), strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        drawLine(palette.accent, androidx.compose.ui.geometry.Offset(cx, cy - arm), androidx.compose.ui.geometry.Offset(cx, cy + arm), strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    }
                }
            }
            Text("Фото", fontSize = 13.sp, color = palette.muted)

            androidx.compose.foundation.text.BasicTextField(
                value = state.name,
                onValueChange = { vm.setFormName(it) },
                modifier = Modifier.testTag(TAG_FORM_NAME),
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, color = palette.text),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(palette.accent),
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(palette.field)
                            .border(1.dp, palette.line, RoundedCornerShape(9.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (state.name.isEmpty()) {
                            Text("Название", fontSize = 16.sp, color = palette.muted)
                        }
                        inner()
                    }
                },
            )

            MuscleSection(
                title = "Основные мышцы",
                selectedIds = state.primaryIds,
                allMuscles = state.allMuscles.map { it.id to it.name },
                accentFill = true,
                addTag = TAG_ADD_PRIMARY,
                onToggle = { vm.togglePrimary(it) },
                onAdd = { showPicker = Picker.Primary },
            )

            MuscleSection(
                title = "Вторичные мышцы",
                selectedIds = state.secondaryIds,
                allMuscles = state.allMuscles.map { it.id to it.name },
                accentFill = false,
                addTag = TAG_ADD_SECONDARY,
                onToggle = { vm.toggleSecondary(it) },
                onAdd = { showPicker = Picker.Secondary },
            )

            PrimaryButton(
                "Сохранить",
                onClick = {
                    vm.saveForm()
                    onSaved()
                },
                modifier = Modifier.testTag(TAG_SAVE),
                enabled = state.name.isNotBlank(),
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showSheet) {
        PhotoSourceSheet(
            context = context,
            hasPhoto = state.photoPath != null,
            onPicked = { uri -> vm.onFormPickedPhoto(context.contentResolver, uri) },
            onDelete = { vm.deleteFormPhoto() },
            onDismiss = { showSheet = false },
        )
    }

    // «+ добавить»: нижний лист мышц, тап по мышце добавляет её в секцию.
    when (showPicker) {
        Picker.Primary -> MusclePickerSheet(
            title = "Основная мышца",
            muscles = state.allMuscles.map { it.id to it.name }.filter { it.first !in state.primaryIds },
            onAdd = {
                vm.togglePrimary(it)
                showPicker = Picker.None
            },
            onDismiss = { showPicker = Picker.None },
        )
        Picker.Secondary -> MusclePickerSheet(
            title = "Вторичная мышца",
            muscles = state.allMuscles.map { it.id to it.name }.filter { it.first !in state.secondaryIds },
            onAdd = {
                vm.toggleSecondary(it)
                showPicker = Picker.None
            },
            onDismiss = { showPicker = Picker.None },
        )
        Picker.None -> Unit
    }
}

private enum class Picker { None, Primary, Secondary }

/**
 * Секция мышц формы по мокапу C2: чипы выбранных мышц и чип «+ добавить» в одном ряду.
 * Основные — акцентной заливкой, вторичные — серым; тап по чипу снимает мышцу.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MuscleSection(
    title: String,
    selectedIds: List<Long>,
    allMuscles: List<Pair<Long, String>>,
    accentFill: Boolean,
    addTag: String,
    onToggle: (Long) -> Unit,
    onAdd: () -> Unit,
) {
    val palette = LocalGymPalette.current
    // Порядок чипов — по каталогу мышц.
    val selected = allMuscles.filter { it.first in selectedIds }

    Text(title, fontSize = 12.sp, color = palette.muted, modifier = Modifier.padding(top = 4.dp))
    FlowRow(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        selected.forEach { (id, name) ->
            Box(
                Modifier
                    .height(34.dp)
                    .testTag(TAG_FORM_CHIP)
                    .clip(RoundedCornerShape(17.dp))
                    .background(if (accentFill) palette.accent else palette.panel2)
                    .clickable { onToggle(id) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name,
                    fontSize = 13.sp,
                    fontWeight = if (accentFill) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (accentFill) palette.onAccent else palette.text,
                )
            }
        }
        Box(
            Modifier
                .height(34.dp)
                .testTag(addTag)
                .clip(RoundedCornerShape(17.dp))
                .background(palette.panel2)
                .clickable(onClick = onAdd)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("+ добавить", fontSize = 13.sp, color = palette.text)
        }
    }
}

/** Лист выбора мышцы: список каталога, тап добавляет мышцу в секцию формы. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MusclePickerSheet(
    title: String,
    muscles: List<Pair<Long, String>>,
    onAdd: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalGymPalette.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.panel) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.text,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            muscles.forEach { (id, name) ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onAdd(id) }
                        .padding(vertical = 13.dp),
                ) {
                    Text(name, fontSize = 16.sp, color = palette.text)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BackHeaderLocal(text: String, onClick: () -> Unit) {
    val palette = LocalGymPalette.current
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 2.dp)
            .clickable(onClick = onClick),
    ) {
        Text(text, fontSize = 15.sp, color = palette.muted)
    }
}
