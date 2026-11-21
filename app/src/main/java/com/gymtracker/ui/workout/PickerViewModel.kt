package com.gymtracker.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.GymRepository
import com.gymtracker.data.MuscleArea
import com.gymtracker.data.MuscleLabel
import com.gymtracker.data.MuscleRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class PickRowUi(
    val id: Long,
    val name: String,
    val imagePath: String?,
    /** Подпись: первичная мышца + вторичные, кириллица, строчные. */
    val caption: String,
)

data class PickerUiState(
    val areas: List<MuscleArea>,
    /** null — фильтр «Все» (выбран по умолчанию), иначе id выбранной области. */
    val areaId: Long?,
    val query: String,
    val rows: List<PickRowUi>,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PickerViewModel(private val repo: GymRepository) : ViewModel() {

    private val areaId = MutableStateFlow<Long?>(null)
    private val query = MutableStateFlow("")

    val uiState: StateFlow<PickerUiState> = combine(
        repo.areas(),
        areaId.flatMapLatest { area ->
            query.flatMapLatest { q ->
                val exercises = if (area == null) repo.exercisesAll(q) else repo.exercisesByArea(area, q)
                combine(exercises, repo.muscleLabels()) { exercises2, labels ->
                    val byId = labels.groupBy { it.exerciseId }
                    exercises2.map { e ->
                        PickRowUi(
                            id = e.id,
                            name = e.name,
                            imagePath = e.imagePath,
                            caption = caption(byId[e.id] ?: emptyList()),
                        )
                    }
                }
            }
        },
        areaId,
        query,
    ) { areas, rows, area, q ->
        PickerUiState(areas, area, q, rows)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        PickerUiState(emptyList(), null, "", emptyList()),
    )

    private fun caption(labels: List<MuscleLabel>): String =
        labels.joinToString(" · ") { it.muscle.lowercase() }

    /** null — «Все», иначе id области. */
    fun selectArea(id: Long?) {
        areaId.value = id
    }

    fun setQuery(q: String) {
        query.value = q
    }

    /** Тап по строке:id упражнения уходит в WorkoutsViewModel, который делает блок активным. */
    fun pick(exerciseId: Long) {
        repo.pickerResult.value = exerciseId
    }
}
