package com.gymtracker.ui.exercise

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gymtracker.data.WeightPoint
import com.gymtracker.ui.components.DangerSmallButton
import com.gymtracker.ui.components.SetChip
import com.gymtracker.ui.components.TextConfirmDialog
import com.gymtracker.ui.theme.LocalGymPalette
import com.gymtracker.util.formatDateFull
import com.gymtracker.util.formatWeight
import java.io.File
import kotlin.math.max

/** testTag для автотестов. */
const val TAG_EDIT = "exercise_edit"
const val TAG_CARD_HISTORY_ROW = "card_history_row"
const val TAG_DELETE = "exercise_delete"

/** C3 — карточка упражнения: фото, мышцы цветом, график, история, удаление. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseCardScreen(
    vm: ExerciseViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
    onOpenSession: (Long) -> Unit,
) {
    val state by vm.cardState.collectAsState()
    val palette = LocalGymPalette.current
    val context = LocalContext.current

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f).clickable(onClick = onBack)) {
                Text(state.fromLabel, fontSize = 15.sp, color = palette.muted)
            }
            // Карандаш в правом верхнем углу — редактирование (C3c).
            Box(
                Modifier
                    .size(40.dp)
                    .testTag(TAG_EDIT)
                    .clip(CircleShape)
                    .clickable(onClick = onEdit),
                contentAlignment = Alignment.Center,
            ) {
                Text("✎", fontSize = 20.sp, color = palette.accent)
            }
        }
        Text(
            state.name,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.text,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 4.dp),
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Фото с плюсом в круге: тап открывает лист источника (C3a).
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.photo)
                    .clickable { vm.openPhotoSheet() },
                contentAlignment = Alignment.Center,
            ) {
                if (state.imagePath != null) {
                    AsyncImage(
                        model = File(state.imagePath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                PlusInCircle()
            }
            // Под фото только подпись «Фото».
            Text("Фото", fontSize = 13.sp, color = palette.muted)

            // Цветовая разметка мышц: основные акцентом, вторичные контуром, подписей нет.
            if (state.muscles.isNotEmpty()) {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.muscles.forEach { muscle ->
                        val primary = muscle.role == "PRIMARY"
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (primary) palette.accent else Color.Transparent)
                                .border(1.dp, palette.accent, RoundedCornerShape(16.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                muscle.name,
                                fontSize = 13.sp,
                                color = if (primary) palette.onAccent else palette.accent,
                            )
                        }
                    }
                }
            }

            WeightGraph(state.points)

            Text("История упражнения", fontSize = 15.sp, color = palette.text, modifier = Modifier.padding(top = 4.dp))
            state.history.forEach { row ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .testTag(TAG_CARD_HISTORY_ROW)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.panel)
                        .clickable { onOpenSession(row.sessionId) }
                        .padding(12.dp),
                ) {
                    Text(row.dateLabel, fontSize = 15.sp, color = palette.text)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        row.chips.forEach { chip -> SetChip(chip) }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            DangerSmallButton(
                "Удалить упражнение",
                onClick = { vm.requestDelete() },
                modifier = Modifier.testTag(TAG_DELETE),
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    if (state.showDeleteConfirm) {
        TextConfirmDialog(
            title = "Удалить упражнение?",
            message = "Записи тренировок, подходы, график и история сохранятся; упражнение скроется с интерфейса.",
            primaryText = "Удалить",
            secondaryText = "Отмена",
            onPrimary = {
                vm.confirmDelete()
                onDeleted()
            },
            onSecondary = { vm.cancelDelete() },
        )
    }

    if (state.showPhotoSheet) {
        PhotoSourceSheet(
            context = context,
            hasPhoto = state.imagePath != null,
            onPicked = { uri -> vm.onPickedPhoto(context.contentResolver, uri) },
            onDelete = { vm.deletePhoto() },
            onDismiss = { vm.closePhotoSheet() },
        )
    }
}

/** Векторный плюс в круге (не текстовый «+»), центрируется по кругу. */
@Composable
private fun PlusInCircle() {
    val palette = LocalGymPalette.current
    Canvas(Modifier.size(64.dp)) {
        val r = size.minDimension / 2f
        drawCircle(color = palette.accent, radius = r, style = Stroke(width = 2.dp.toPx()))
        val cx = size.width / 2f
        val cy = size.height / 2f
        val arm = r * 0.45f
        drawLine(palette.accent, Offset(cx - arm, cy), Offset(cx + arm, cy), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(palette.accent, Offset(cx, cy - arm), Offset(cx, cy + arm), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    }
}

/**
 * График максимального веса по датам: окно по умолчанию 20 точек, листается свайпом,
 * y-ось подстраивается под видимое окно, тап по точке показывает значение и дату.
 */
@Composable
private fun WeightGraph(points: List<WeightPoint>) {
    val palette = LocalGymPalette.current
    val window = 20
    var offset by remember(points) { mutableStateOf(0) }
    var selected by remember(points) { mutableStateOf<WeightPoint?>(null) }
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()

    val desc = points.reversed()
    val start = offset.coerceIn(0, max(0, desc.size - window))
    val visible = desc.subList(start, minOf(desc.size, start + window)).reversed()

    Column(Modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.panel)
                .padding(8.dp)
                .pointerInput(points, offset) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        offset = (offset + (-dragAmount / 40f).toInt()).coerceIn(0, max(0, desc.size - window))
                    }
                }
                .pointerInput(visible) {
                    detectTapGestures { tap ->
                        if (visible.size >= 2) {
                            val x = (tap.x / size.width) * (visible.size - 1)
                            selected = visible[x.toInt().coerceIn(0, visible.size - 1)]
                        }
                    }
                },
        ) {
            val w = size.width
            val h = size.height
            if (visible.isEmpty()) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = "Нет записанных тренировок",
                    style = TextStyle(fontSize = 13.sp, color = palette.muted),
                    topLeft = Offset(8.dp.toPx(), h / 2f - 8.dp.toPx()),
                )
                return@Canvas
            }
            val minW = visible.minOf { it.maxWeight } * 0.92
            val maxW = visible.maxOf { it.maxWeight } * 1.08
            val spread = maxW - minW
            val stepX = if (visible.size > 1) w / (visible.size - 1) else w
            val pts = visible.mapIndexed { i, p ->
                Offset(
                    if (visible.size == 1) w / 2f else i * stepX,
                    h - ((p.maxWeight - minW) / spread).toFloat() * h,
                )
            }
            val line = Path().apply {
                moveTo(pts.first().x, pts.first().y)
                pts.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(line, palette.accent, style = Stroke(width = 2.dp.toPx()))
            pts.forEach { p -> drawCircle(palette.accent, radius = 4.dp.toPx(), center = p) }
            if (selected != null) {
                val idx = visible.indexOf(selected)
                if (idx >= 0) {
                    drawCircle(palette.text, radius = 6.dp.toPx(), center = pts[idx], style = Stroke(width = 1.dp.toPx()))
                }
            }
            drawText(
                textMeasurer = textMeasurer,
                text = formatWeight(maxW),
                style = TextStyle(fontSize = 11.sp, color = palette.muted),
                topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
            )
            drawText(
                textMeasurer = textMeasurer,
                text = formatWeight(minW),
                style = TextStyle(fontSize = 11.sp, color = palette.muted),
                topLeft = Offset(2.dp.toPx(), h - 14.dp.toPx()),
            )
        }
        val shown = selected
        if (shown != null) {
            Text(
                "${formatWeight(shown.maxWeight)} кг · ${formatDateFull(shown.dateMillis)}",
                fontSize = 13.sp,
                color = palette.text,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** C3a — нижний лист источника фото: камера / галерея / удаление. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PhotoSourceSheet(
    context: Context,
    hasPhoto: Boolean,
    onPicked: (Uri) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalGymPalette.current
    var cameraFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val file = cameraFile
        if (success && file != null && file.exists()) {
            onPicked(Uri.fromFile(file))
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri -> if (uri != null) onPicked(uri) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.panel) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            SheetRow("Снять фото камерой", palette) {
                val dir = File(context.cacheDir, "camera").apply { mkdirs() }
                val file = File(dir, "shot_${System.currentTimeMillis()}.jpg")
                cameraFile = file
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
                cameraLauncher.launch(uri)
            }
            SheetRow("Выбрать из галереи", palette) { galleryLauncher.launch("image/*") }
            if (hasPhoto) {
                SheetRow("Удалить фото", palette, danger = true, onClick = { onDelete() })
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SheetRow(label: String, palette: com.gymtracker.ui.theme.GymPalette, danger: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        Text(
            label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (danger) palette.danger else palette.text,
        )
    }
}
