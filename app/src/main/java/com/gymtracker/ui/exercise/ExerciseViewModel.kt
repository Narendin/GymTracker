package com.gymtracker.ui.exercise

import android.content.ContentResolver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.gymtracker.data.GymRepository
import com.gymtracker.data.Muscle
import com.gymtracker.data.WeightPoint
import com.gymtracker.util.formatDateFull
import com.gymtracker.util.formatWeight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExerciseMuscleUi(val muscleId: Long, val name: String, val role: String)

data class HistoryRowUi(val sessionId: Long, val dateLabel: String, val chips: List<String>)

data class ExerciseCardUiState(
    val exerciseId: Long = 0,
    val fromLabel: String = "← Мышцы",
    val name: String = "",
    val imagePath: String? = null,
    val muscles: List<ExerciseMuscleUi> = emptyList(),
    val points: List<WeightPoint> = emptyList(),
    val history: List<HistoryRowUi> = emptyList(),
    val isBodyweight: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val showPhotoSheet: Boolean = false,
)

data class ExerciseFormUiState(
    val open: Boolean = false,
    val editingId: Long? = null,
    val name: String = "",
    val photoPath: String? = null,
    val primaryIds: List<Long> = emptyList(),
    val secondaryIds: List<Long> = emptyList(),
    val allMuscles: List<Muscle> = emptyList(),
    val isBodyweight: Boolean = false,
)

/** C3 — карточка упражнения; C2/C3c — форма создания/редактирования; C3a — лист фото. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ExerciseViewModel(private val repo: GymRepository) : ViewModel() {

    private val card = MutableStateFlow(ExerciseCardUiState())
    private val form = MutableStateFlow(ExerciseFormUiState())

    val cardState: StateFlow<ExerciseCardUiState> = card
        .flatMapLatest { state ->
            if (state.exerciseId == 0L) {
                kotlinx.coroutines.flow.flow { emit(state) }
            } else {
                combine(
                    repo.exerciseById(state.exerciseId),
                    repo.musclesForExercise(state.exerciseId),
                    repo.maxWeightPoints(state.exerciseId),
                    repo.exerciseHistorySets(state.exerciseId),
                ) { exercise, muscles, points, sets ->
                    val bw = exercise?.isBodyweight ?: false
                    state.copy(
                        name = exercise?.name ?: state.name,
                        imagePath = exercise?.imagePath,
                        isBodyweight = bw,
                        muscles = muscles.map { ExerciseMuscleUi(it.muscleId, it.name, it.role.name) },
                        points = points,
                        history = sets
                            .groupBy { it.sessionId }
                            .entries
                            .sortedByDescending { (id, rows) -> rows.first().startMillis }
                            .map { (sessionId, rows) ->
                                HistoryRowUi(
                                    sessionId = sessionId,
                                    dateLabel = formatDateFull(rows.first().startMillis),
                                    chips = rows
                                        .sortedBy { it.orderInExercise }
                                        .map {
                                            if (bw) "${it.reps} повт."
                                            else "${formatWeight(it.weightKg)} кг × ${it.reps}"
                                        },
                                )
                            },
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ExerciseCardUiState())

    val formState: StateFlow<ExerciseFormUiState> = form
        .stateIn(viewModelScope, SharingStarted.Eagerly, ExerciseFormUiState())

    fun openCard(exerciseId: Long, fromLabel: String) {
        card.value = card.value.copy(exerciseId = exerciseId, fromLabel = fromLabel)
    }

    fun closeCard() {
        card.value = card.value.copy(exerciseId = 0L, showDeleteConfirm = false, showPhotoSheet = false)
    }

    // ——— C3b: удаление (мягкое) ———
    fun requestDelete() {
        card.value = card.value.copy(showDeleteConfirm = true)
    }

    fun cancelDelete() {
        card.value = card.value.copy(showDeleteConfirm = false)
    }

    fun confirmDelete() {
        val id = card.value.exerciseId
        if (id == 0L) return
        viewModelScope.launch {
            repo.softDeleteExercise(id)
            card.value = card.value.copy(showDeleteConfirm = false, exerciseId = 0L)
        }
    }

    // ——— C3a: лист источника фото ———
    fun openPhotoSheet() {
        card.value = card.value.copy(showPhotoSheet = true)
    }

    fun closePhotoSheet() {
        card.value = card.value.copy(showPhotoSheet = false)
    }

    /** Снимок камеры/галереи: копируется в каталог приложения, путь идёт в базу. */
    fun onPickedPhoto(resolver: ContentResolver, uri: Uri) {
        val id = card.value.exerciseId
        viewModelScope.launch {
            val path = repo.importPhoto(resolver, uri)
            if (id != 0L) {
                repo.setExercisePhoto(id, path)
            } else {
                form.value = form.value.copy(photoPath = path)
            }
            card.value = card.value.copy(showPhotoSheet = false)
        }
    }

    fun deletePhoto() {
        val id = card.value.exerciseId
        viewModelScope.launch {
            if (id != 0L) {
                repo.setExercisePhoto(id, null)
            }
            card.value = card.value.copy(showPhotoSheet = false)
        }
    }

    // ——— C2/C3c: форма ———
    fun onFormPickedPhoto(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val path = repo.importPhoto(resolver, uri)
            form.value = form.value.copy(photoPath = path)
        }
    }

    fun deleteFormPhoto() {
        form.value = form.value.copy(photoPath = null)
    }

    /** initialMuscleId — основная мышца из экрана «Мышцы»; null, если область не открыта. */
    fun openFormCreate(initialMuscleId: Long?) {
        viewModelScope.launch {
            val muscles = repo.allMuscles().first()
            form.value = ExerciseFormUiState(
                open = true,
                editingId = null,
                primaryIds = listOfNotNull(initialMuscleId),
                allMuscles = muscles,
            )
        }
    }

    fun openFormEdit(exerciseId: Long) {
        viewModelScope.launch {
            val exercise = repo.exerciseByIdOnce(exerciseId)
            val muscles = repo.allMuscles().first()
            val links = repo.linksForExercise(exerciseId).first()
            form.value = ExerciseFormUiState(
                open = true,
                editingId = exerciseId,
                name = exercise?.name ?: "",
                photoPath = exercise?.imagePath,
                primaryIds = links.filter { it.role.name == "PRIMARY" }.map { it.muscleId },
                secondaryIds = links.filter { it.role.name == "SECONDARY" }.map { it.muscleId },
                allMuscles = muscles,
                isBodyweight = exercise?.isBodyweight ?: false,
            )
        }
    }

    fun closeForm() {
        form.value = form.value.copy(open = false)
    }

    fun setFormName(value: String) {
        form.value = form.value.copy(name = value)
    }

    fun togglePrimary(muscleId: Long) {
        val list = form.value.primaryIds
        form.value = form.value.copy(
            primaryIds = if (muscleId in list) list - muscleId else list + muscleId,
            secondaryIds = form.value.secondaryIds - muscleId,
        )
    }

    fun toggleSecondary(muscleId: Long) {
        val list = form.value.secondaryIds
        form.value = form.value.copy(
            secondaryIds = if (muscleId in list) list - muscleId else list + muscleId,
            primaryIds = form.value.primaryIds - muscleId,
        )
    }

    fun setFormBodyweight(value: Boolean) {
        form.value = form.value.copy(isBodyweight = value)
    }

    fun saveForm() {
        val state = form.value
        val name = state.name.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            val id = state.editingId
            if (id == null) {
                repo.createExercise(name, state.primaryIds, state.secondaryIds, state.photoPath, state.isBodyweight)
            } else {
                repo.updateExercise(id, name, state.primaryIds, state.secondaryIds, state.photoPath, state.isBodyweight)
            }
            form.value = form.value.copy(open = false)
        }
    }
}
