package com.gymtracker.ui.changes

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.ChangePhoto
import com.gymtracker.data.GymRepository
import com.gymtracker.util.formatDateFull
import com.gymtracker.util.formatDateShort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChangeGroupUi(
    val id: Long,
    val name: String,
    val photoCount: Int,
    val lastLabel: String?,
)

data class ChangesUiState(
    val groups: List<ChangeGroupUi> = emptyList(),
    val showAdd: Boolean = false,
    val nameInput: String = "",
    val openGroup: Pair<Long, String>? = null,
    val photos: List<ChangePhoto> = emptyList(),
    val selected: List<Long> = emptyList(),
    val viewIndex: Int? = null,
    val compare: List<ChangePhoto> = emptyList(),
    val showDeleteConfirm: Boolean = false,
)

class ChangesViewModel(private val repo: GymRepository) : ViewModel() {

    private val showAdd = MutableStateFlow(false)
    private val nameInput = MutableStateFlow("")
    private val openGroup = MutableStateFlow<Pair<Long, String>?>(null)
    private val selected = MutableStateFlow<List<Long>>(emptyList())
    private val viewIndex = MutableStateFlow<Int?>(null)
    private val compare = MutableStateFlow<List<ChangePhoto>>(emptyList())
    private val showDeleteConfirm = MutableStateFlow(false)

    private val photosFlow = openGroup.flatMapLatest { g ->
        if (g == null) kotlinx.coroutines.flow.flowOf(emptyList())
        else repo.photosForGroup(g.first)
    }

    val uiState: StateFlow<ChangesUiState> = combine(
        combine(repo.changeGroups(), showAdd, nameInput) { groups, add, name -> Triple(groups, add, name) },
        combine(openGroup, photosFlow, selected, viewIndex, compare) { group, photos, sel, view, cmp ->
            listOf(group, photos, sel, view, cmp)
        },
        showDeleteConfirm,
    ) { (groups, add, name), parts, del ->
        ChangesUiState(
            groups = groups.map {
                ChangeGroupUi(
                    id = it.group.id,
                    name = it.group.name,
                    photoCount = it.photoCount,
                    lastLabel = it.lastPhotoAt?.let { d -> formatDateShort(d) },
                )
            },
            showAdd = add,
            nameInput = name,
            openGroup = parts[0] as Pair<Long, String>?,
            photos = parts[1] as List<ChangePhoto>,
            selected = parts[2] as List<Long>,
            viewIndex = parts[3] as Int?,
            compare = parts[4] as List<ChangePhoto>,
            showDeleteConfirm = del,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ChangesUiState())

    fun openAdd() {
        nameInput.value = ""
        showAdd.value = true
    }

    fun closeAdd() {
        showAdd.value = false
    }

    fun setGroupName(value: String) {
        nameInput.value = value
    }

    fun saveGroup() {
        val name = nameInput.value.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            repo.createChangeGroup(name)
            showAdd.value = false
        }
    }

    // ——— E2 ———
    fun openGroupPhotos(id: Long, name: String) {
        openGroup.value = id to name
        selected.value = emptyList()
        viewIndex.value = null
        compare.value = emptyList()
    }

    fun closeGroup() {
        openGroup.value = null
        selected.value = emptyList()
        viewIndex.value = null
        compare.value = emptyList()
    }

    /** Долгий тап: режим выбора; обычный тап: просмотр. */
    fun toggleSelect(photoId: Long) {
        val list = selected.value
        selected.value = if (photoId in list) list - photoId else list + photoId
    }

    fun clearSelection() {
        selected.value = emptyList()
    }

    fun openView(index: Int) {
        viewIndex.value = index
    }

    fun closeView() {
        viewIndex.value = null
    }

    fun setViewIndex(index: Int) {
        viewIndex.value = index
    }

    /** E4: сравнение двух фото, слева — более ранняя дата. */
    fun openCompare() {
        val sel = selected.value.toSet()
        val picked = uiState.value.photos.filter { it.id in sel }.sortedBy { it.takenAt }
        if (picked.size == 2) {
            compare.value = picked
        }
    }

    fun closeCompare() {
        compare.value = emptyList()
    }

    fun addFromCamera(resolver: ContentResolver, uri: Uri) {
        addPhotoFromUri(resolver, uri)
    }

    fun addFromGallery(resolver: ContentResolver, uri: Uri) {
        addPhotoFromUri(resolver, uri)
    }

    private fun addPhotoFromUri(resolver: ContentResolver, uri: Uri) {
        val group = openGroup.value?.first ?: return
        viewModelScope.launch {
            val path = repo.importPhoto(resolver, uri)
            repo.addChangePhoto(group, path)
        }
    }

    /** Удаление фото — только после подтверждения с числом удаляемых снимков. */
    fun requestDelete() {
        if (selected.value.isNotEmpty()) showDeleteConfirm.value = true
    }

    fun cancelDelete() {
        showDeleteConfirm.value = false
    }

    fun confirmDelete() {
        val ids = selected.value
        showDeleteConfirm.value = false
        viewModelScope.launch {
            ids.forEach { repo.deleteChangePhoto(it) }
            selected.value = emptyList()
        }
    }

    fun formatDateForView(millis: Long): String = formatDateFull(millis)
}
