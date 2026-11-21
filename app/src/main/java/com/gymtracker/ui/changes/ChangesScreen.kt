package com.gymtracker.ui.changes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.gymtracker.ui.components.BackHeader
import com.gymtracker.ui.components.DangerSmallButton
import com.gymtracker.ui.components.OutlinedButton
import com.gymtracker.ui.components.OutlinedSmallButton
import com.gymtracker.ui.components.PrimaryButton
import com.gymtracker.ui.components.ScreenTitle
import com.gymtracker.ui.components.TextConfirmDialog
import com.gymtracker.ui.theme.LocalGymPalette
import com.gymtracker.util.formatDateFull
import com.gymtracker.util.formatDateShort
import com.gymtracker.util.photosWord
import com.gymtracker.util.plural
import java.io.File
import java.time.Instant
import java.time.ZoneId

/** E1 — список групп; E2/E2a/E3/E4 — экран группы фото. */
@Composable
fun ChangesScreen(vm: ChangesViewModel) {
    val state by vm.uiState.collectAsState()
    val group = state.openGroup

    if (group == null) {
        GroupList(vm, state)
        if (state.showAdd) {
            AddGroupDialog(vm, state)
        }
    } else {
        GroupPhotos(vm, state, group.second)
    }
}

@Composable
private fun GroupList(vm: ChangesViewModel, state: ChangesUiState) {
    val palette = LocalGymPalette.current
    Column(Modifier.fillMaxWidth()) {
        ScreenTitle("Изменения")
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.groups.forEach { group ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.panel)
                        .clickable { vm.openGroupPhotos(group.id, group.name) }
                        .padding(12.dp),
                ) {
                    Text(group.name, fontSize = 17.sp, color = palette.text)
                    Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${group.photoCount} ${plural(group.photoCount, "фото", "фото", "фото")}",
                            fontSize = 13.sp,
                            color = palette.muted,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            group.lastLabel ?: "—",
                            fontSize = 13.sp,
                            color = palette.muted,
                        )
                    }
                }
            }
            OutlinedButton("Новая группа", onClick = { vm.openAdd() })
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AddGroupDialog(vm: ChangesViewModel, state: ChangesUiState) {
    val palette = LocalGymPalette.current
    androidx.compose.ui.window.Dialog(onDismissRequest = { vm.closeAdd() }) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.panel2)
                .padding(16.dp),
        ) {
            Text(
                "Новая группа",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.text,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            androidx.compose.foundation.text.BasicTextField(
                value = state.nameInput,
                onValueChange = { vm.setGroupName(it) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, color = palette.text),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(palette.accent),
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(palette.field)
                            .border(1.dp, palette.line, RoundedCornerShape(9.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (state.nameInput.isEmpty()) {
                            Text("Название группы", fontSize = 16.sp, color = palette.muted)
                        }
                        inner()
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Сохранить", onClick = { vm.saveGroup() })
            Spacer(Modifier.height(8.dp))
            OutlinedSmallButton("Отмена", onClick = { vm.closeAdd() })
        }
    }
}

/**
 * E2: сетка фото, дата сверху слева; долгий тап — режим выбора (акцентная рамка и галочка);
 * при выборе появляются «Сравнить» и «Удалить»; «Добавить фото» — нижний лист источника.
 */
@OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)
@Composable
private fun GroupPhotos(vm: ChangesViewModel, state: ChangesUiState, groupName: String) {
    val palette = LocalGymPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSheet by remember { mutableStateOf(false) }
    val pendingCameraFile = remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val file = pendingCameraFile.value
        if (success && file != null && file.exists()) {
            vm.addFromCamera(context.contentResolver, android.net.Uri.fromFile(file))
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri -> if (uri != null) vm.addFromGallery(context.contentResolver, uri) }

    Column(Modifier.fillMaxWidth()) {
        BackHeader("← Изменения", onClick = { vm.closeGroup() })
        ScreenTitle(groupName)

        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.photos.isEmpty()) {
                Text("Пока нет ни одной фотографии.", fontSize = 15.sp, color = palette.muted, modifier = Modifier.padding(top = 12.dp))
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height((110 * ((state.photos.size + 1) / 2)).dp.coerceIn(0.dp, 1200.dp)),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.photos) { photo ->
                    val isSelected = photo.id in state.selected
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.photo)
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.dp, palette.accent, RoundedCornerShape(10.dp))
                                } else {
                                    Modifier
                                }
                            )
                            .combinedClickable(
                                onClick = {
                                    if (state.selected.isNotEmpty()) {
                                        vm.toggleSelect(photo.id)
                                    } else {
                                        val idx = state.photos.indexOfFirst { it.id == photo.id }
                                        vm.openView(idx)
                                    }
                                },
                                onLongClick = { vm.toggleSelect(photo.id) },
                            ),
                    ) {
                        AsyncImage(
                            model = File(photo.imagePath),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp)),
                        )
                        // Дата сверху слева.
                        Text(
                            formatDateShort(photo.takenAt),
                            fontSize = 11.sp,
                            color = palette.text,
                            modifier = Modifier
                                .padding(6.dp)
                                .background(palette.panel.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                                .padding(3.dp),
                        )
                        if (isSelected) {
                            Box(
                                Modifier
                                    .padding(6.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(palette.accent),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("✓", fontSize = 13.sp, color = palette.onAccent)
                            }
                        }
                    }
                }
            }

            // Кнопки выбора: все одного размера, равной шириной. «Сравнить» —
            // только при двух и более фото; «Добавить фото» прячется,
            // пока что-то выбрано.
            if (state.selected.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.selected.size >= 2) {
                        Box(Modifier.weight(1f)) {
                            OutlinedSmallButton("Сравнить", onClick = { vm.openCompare() })
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        DangerSmallButton("Удалить", onClick = { vm.requestDelete() })
                    }
                    Box(Modifier.weight(1f)) {
                        OutlinedSmallButton("Отменить", onClick = { vm.clearSelection() })
                    }
                }
            } else {
                OutlinedButton("Добавить", onClick = { showSheet = true })
            }
            Spacer(Modifier.height(16.dp))
        }

        if (state.showDeleteConfirm) {
            val n = state.selected.size
            TextConfirmDialog(
                title = "Удаление изображений",
                message = "Вы точно хотите удалить $n ${photosWord(n)}?",
                primaryText = "Удалить",
                secondaryText = "Отмена",
                onPrimary = { vm.confirmDelete() },
                onSecondary = { vm.cancelDelete() },
            )
        }
    }

    if (showSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = palette.panel,
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            val dir = File(context.cacheDir, "camera").apply { mkdirs() }
                            val file = File(dir, "shot_${System.currentTimeMillis()}.jpg")
                            pendingCameraFile.value = file
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file,
                            )
                            cameraLauncher.launch(uri)
                            showSheet = false
                        }
                        .padding(vertical = 14.dp),
                ) {
                    Text("Снять фото камерой", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = palette.text)
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            galleryLauncher.launch("image/*")
                            showSheet = false
                        }
                        .padding(vertical = 14.dp),
                ) {
                    Text("Выбрать из галереи", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = palette.text)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // E3 — просмотр: снимок на весь экран, дата в левом верхнем углу, свайп влево/вправо
    // листает фото группы по дате добавления; свайп вверх или «назад» телефона закрывает.
    val viewIndex = state.viewIndex
    if (viewIndex != null && state.photos.isNotEmpty()) {
        var accUp by remember { mutableStateOf(0f) }
        val pager = androidx.compose.foundation.pager.rememberPagerState(
            initialPage = viewIndex.coerceIn(0, state.photos.size - 1),
        ) { state.photos.size }
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { vm.closeView() },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black)
                    .pointerInput(state.photos.size) {
                        detectVerticalDragGestures(
                            onDragStart = { accUp = 0f },
                            onVerticalDrag = { _, dy ->
                                accUp += dy
                                if (accUp < -300f) vm.closeView()
                            },
                        )
                    },
            ) {
                androidx.compose.foundation.pager.HorizontalPager(state = pager) { page ->
                    val photo = state.photos.getOrNull(page)
                    if (photo != null) {
                        Box(Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = File(photo.imagePath),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Text(
                                formatDateFull(photo.takenAt),
                                fontSize = 15.sp,
                                color = palette.text,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                                    .background(palette.panel.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                                    .padding(4.dp),
                            )
                        }
                    }
                }
                if (state.photos.size > 1) {
                    Row(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        repeat(state.photos.size) { i ->
                            Box(
                                Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (pager.currentPage == i) palette.accent else palette.line,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }

    // E4 — сравнение выделенных фото: снимок на весь экран, лента миниатюр внизу,
    // свайп по снимку и тап по миниатюре листают; порядок по дате добавления.
    val cmp = state.compare
    if (cmp.size == 2) {
        var accUpCmp by remember { mutableStateOf(0f) }
        val cmpPager = androidx.compose.foundation.pager.rememberPagerState(initialPage = 0) { cmp.size }
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { vm.closeCompare() },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black)
                    .pointerInput(cmp.size) {
                        detectVerticalDragGestures(
                            onDragStart = { accUpCmp = 0f },
                            onVerticalDrag = { _, dy ->
                                accUpCmp += dy
                                if (accUpCmp < -300f) vm.closeCompare()
                            },
                        )
                    },
            ) {
                androidx.compose.foundation.pager.HorizontalPager(state = cmpPager) { page ->
                    val photo = cmp.getOrNull(page)
                    if (photo != null) {
                        Box(Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = File(photo.imagePath),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Text(
                                formatDateFull(photo.takenAt),
                                fontSize = 15.sp,
                                color = palette.text,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                                    .background(palette.panel.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                                    .padding(4.dp),
                            )
                        }
                    }
                }
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 30.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    cmp.forEachIndexed { i, photo ->
                        Column {
                            Box(
                                Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .then(
                                        if (cmpPager.currentPage == i) {
                                            Modifier.border(2.dp, palette.accent, RoundedCornerShape(8.dp))
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { scope.launch { cmpPager.animateScrollToPage(i) } },
                            ) {
                                AsyncImage(
                                    model = File(photo.imagePath),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            Text(
                                formatDateShort(photo.takenAt),
                                fontSize = 10.sp,
                                color = palette.text,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(cmp.size) { i ->
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (cmpPager.currentPage == i) palette.accent else palette.line,
                                ),
                        )
                    }
                }
            }
        }
    }
}
