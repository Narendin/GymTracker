package com.gymtracker.ui.muscles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.GymRepository
import com.gymtracker.data.Muscle
import com.gymtracker.data.MuscleArea
import com.gymtracker.data.Exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MuscleChipUi(val id: Long, val name: String, val selected: Boolean)

data class AreaUi(val id: Long, val name: String, val open: Boolean, val muscles: List<MuscleChipUi>)

data class MuscleRowUi(val id: Long, val name: String, val imagePath: String?, val hidden: Boolean)

data class MusclesUiState(
    val areas: List<AreaUi> = emptyList(),
    /** Видимые упражнения выбранной мышцы, по алфавиту. */
    val exercises: List<MuscleRowUi> = emptyList(),
    /** Скрытые упражнения выбранной мышцы, тоже по алфавиту. */
    val hiddenExercises: List<MuscleRowUi> = emptyList(),
    /** Блок «Скрытое»: по умолчанию свёрнут. */
    val hiddenExpanded: Boolean = false,
)

/** Промежуточный срез состояния, чтобы у combine() осталось пять потоков. */
private data class Selection(
    val areas: List<MuscleArea>,
    val open: Long?,
    val muscles: List<Muscle>,
    val sel: Long?,
    val visible: List<Exercise>,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MusclesViewModel(private val repo: GymRepository) : ViewModel() {

    /** null — все аккордеоны свернуты: так экран открывается при запуске приложения. */
    private val openArea = MutableStateFlow<Long?>(null)
    private val selectedMuscle = MutableStateFlow<Long?>(null)
    private val hiddenExpanded = MutableStateFlow(false)

    private val musclesFlow: Flow<List<Muscle>> = openArea.flatMapLatest { area ->
        if (area == null) flowOf(emptyList()) else repo.musclesForArea(area)
    }

    /** Эффективная выбранная мышца: сохранённая, если она в открытой области, иначе первая. */
    private val selFlow: StateFlow<Long?> = combine(openArea, musclesFlow, selectedMuscle) { area, muscles, sel ->
        if (area == null) {
            null
        } else {
            sel?.takeIf { it in muscles.map(Muscle::id) } ?: muscles.firstOrNull()?.id
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val visibleFlow: Flow<List<Exercise>> = selFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repo.exercisesForMuscle(id, hidden = false)
    }

    private val hiddenFlow: Flow<List<Exercise>> = selFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repo.exercisesForMuscle(id, hidden = true)
    }

    init {
        // Блок «Скрытое» по умолчанию свёрнут. Если скрытых упражнений не осталось
        // (последнее вернули в общий список), состояние сбрасываем: следующее скрытое
        // упражнение появится в уже свёрнутом блоке, а не сразу раскрытым.
        viewModelScope.launch {
            combine(hiddenFlow, hiddenExpanded) { hidden, expanded -> hidden.isEmpty() }
                .collect { isEmpty -> if (isEmpty) hiddenExpanded.value = false }
        }
    }

    val uiState: StateFlow<MusclesUiState> = combine(
        combine(
            repo.areas(),
            openArea,
            musclesFlow,
            selFlow,
            visibleFlow,
        ) { areas, open, muscles, sel, visible ->
            Selection(areas, open, muscles, sel, visible)
        },
        hiddenFlow,
        hiddenExpanded,
    ) { selection, hidden, expanded ->
        MusclesUiState(
            areas = selection.areas.map { a ->
                AreaUi(
                    id = a.id,
                    name = a.name,
                    open = a.id == selection.open,
                    muscles = if (a.id == selection.open) {
                        selection.muscles.map { MuscleChipUi(it.id, it.name, it.id == selection.sel) }
                    } else {
                        emptyList()
                    },
                )
            },
            exercises = selection.visible.map { MuscleRowUi(it.id, it.name, it.imagePath, hidden = false) },
            hiddenExercises = hidden.map { MuscleRowUi(it.id, it.name, it.imagePath, hidden = true) },
            hiddenExpanded = expanded,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MusclesUiState())

    /**
     * Аккордеон: повторное нажатие на заголовок сворачивает область.
     * Открытая область и выбранная мышца живут всю сессию приложения —
     * возврат из вложенных экранов («Мышцы» → карточка/форма) их не сбрасывает,
     * пользователь видит, где произошло изменение.
     */
    fun openArea(id: Long) {
        val opening = openArea.value != id
        openArea.value = if (opening) id else null
        // При сворачивании выбор сбрасывается, чтобы при повторном открытии
        // активной стала первая мышца области.
        if (!opening) selectedMuscle.value = null
        // Блок «Скрытое» снова свёрнут: он свёрнут по умолчанию.
        hiddenExpanded.value = false
    }

    fun selectMuscle(id: Long) {
        selectedMuscle.value = id
    }

    /** Слово «Скрытое»: раскрыть/свернуть блок скрытых упражнений. */
    fun toggleHiddenSection() {
        hiddenExpanded.value = !hiddenExpanded.value
    }

    /** Иконка глаза: скрыть упражнение или вернуть его в общий список. */
    fun toggleExerciseHidden(id: Long) {
        viewModelScope.launch {
            val current = repo.exerciseByIdOnce(id) ?: return@launch
            repo.setExerciseHidden(id, !current.isHidden)
        }
    }

    /** Форма создания открывается с уже выбранной основной мышью. */
    fun selectedMuscleId(): Long? = selFlow.value
}
