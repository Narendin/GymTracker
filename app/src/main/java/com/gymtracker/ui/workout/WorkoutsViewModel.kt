package com.gymtracker.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.BlockStatus
import com.gymtracker.data.BlockWithExercise
import com.gymtracker.data.GymRepository
import com.gymtracker.data.WorkoutSession
import com.gymtracker.util.formatDateFull
import com.gymtracker.util.formatDateShort
import com.gymtracker.util.formatWeight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

data class BlockUi(
    val blockId: Long,
    val exerciseId: Long,
    val name: String,
    val imagePath: String?,
    val sets: List<com.gymtracker.data.SetEntry>,
    val isActive: Boolean,
    /** «↻» видна только на последнем завершённом блоке при отсутствии активного упражнения (A3a). */
    val showResume: Boolean,
)

data class SessionUi(
    val sessionId: Long,
    val exerciseCount: Int,
    val activeBlockId: Long?,
    val blocks: List<BlockUi>,
    val repsInput: String,
    val weightInput: String,
    /** Подставленное значение (вес из прошлого подхода) — акцентная рамка поля. */
    val weightPrefilled: Boolean,
)

data class WorkoutUiState(
    val todayLabel: String,
    val lastWorkoutLabel: String? = null,
    /** id последней завершённой тренировки — счётчик A1 ведёт в её детали (B2). */
    val lastSessionId: Long? = null,
    val count30: Int = 0,
    val totalCount: Int = 0,
    val session: SessionUi? = null,
    val showFinishConfirm: Boolean = false,
    val noSetsBlockId: Long? = null,
    /** Issue #6: диалог правки подхода. */
    val editSetBlockId: Long? = null,
    val editSetId: Long? = null,
    val editWeight: String = "",
    val editReps: String = "",
)

private data class Base(
    val last: WorkoutSession?,
    val c30: Int,
    val total: Int,
    val session: WorkoutSession?,
)

private data class Quint(
    val blocks: List<BlockWithExercise>,
    val reps: String,
    val weight: String,
    val prefilled: Boolean,
    val confirm: Boolean,
)

private data class EditState(
    val blockId: Long?,
    val setId: Long?,
    val weight: String,
    val reps: String,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WorkoutsViewModel(private val repo: GymRepository) : ViewModel() {

    private val repsInput = MutableStateFlow("")
    private val weightInput = MutableStateFlow("")
    private val weightPrefilled = MutableStateFlow(true)
    private val finishConfirm = MutableStateFlow(false)
    private val noSetsBlockId = MutableStateFlow<Long?>(null)
    // Issue #6: состояние диалога правки подхода.
    private val editBlockId = MutableStateFlow<Long?>(null)
    private val editSetId = MutableStateFlow<Long?>(null)
    private val editWeight = MutableStateFlow("")
    private val editReps = MutableStateFlow("")

    private val bound30 = Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli()

    init {
        viewModelScope.launch {
            repo.pickerResult.collect { picked ->
                if (picked != null) {
                    val session = repo.activeSessionOnce()
                    if (session != null) {
                        repo.addExerciseToSession(session.id, picked)
                        prefillFor(picked)
                    }
                    repo.clearPickerResult()
                }
            }
        }
    }

    private suspend fun prefillFor(exerciseId: Long) {
        val lastW = repo.lastWeightForExercise(exerciseId)
        val lastR = repo.lastRepsForExercise(exerciseId)
        weightInput.value = lastW?.let { formatWeight(it) } ?: ""
        repsInput.value = lastR?.toString() ?: "10"
        weightPrefilled.value = true
    }

    val uiState: StateFlow<WorkoutUiState> = combine(
        repo.lastSession(),
        repo.countSince(bound30),
        repo.countAll(),
        repo.activeSession(),
    ) { last, c30, total, session -> Base(last, c30, total, session) }
        .flatMapLatest { base ->
            val session = base.session
            val today = formatDateFull(System.currentTimeMillis())
            val lastLabel = base.last?.let { formatDateShort(it.startMillis) }
            if (session == null) {
                flow {
                    emit(
                        WorkoutUiState(
                            todayLabel = today,
                            lastWorkoutLabel = lastLabel,
                            lastSessionId = base.last?.id,
                            count30 = base.c30,
                            totalCount = base.total,
                            session = null,
                        )
                    )
                }
            } else {
                combine(
                    combine(
                        repo.blocksForSession(session.id),
                        repsInput,
                        weightInput,
                        weightPrefilled,
                        finishConfirm,
                    ) { blocks, reps, weight, prefilled, confirm ->
                        Quint(blocks, reps, weight, prefilled, confirm)
                    },
                    noSetsBlockId,
                    combine(editBlockId, editSetId, editWeight, editReps) { b, s, w, r ->
                        EditState(b, s, w, r)
                    },
                ) { q, noSets, edit ->
                    WorkoutUiState(
                        todayLabel = today,
                        lastWorkoutLabel = lastLabel,
                        count30 = base.c30,
                        totalCount = base.total,
                        session = buildSessionUi(session, q.blocks, q.reps, q.weight, q.prefilled),
                        showFinishConfirm = q.confirm,
                        noSetsBlockId = noSets,
                        editSetBlockId = edit.blockId,
                        editSetId = edit.setId,
                        editWeight = edit.weight,
                        editReps = edit.reps,
                    )
                }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            WorkoutUiState(todayLabel = formatDateFull(System.currentTimeMillis())),
        )

    private fun buildSessionUi(
        session: WorkoutSession,
        blocks: List<BlockWithExercise>,
        reps: String,
        weight: String,
        prefilled: Boolean,
    ): SessionUi {
        val lastDoneId = blocks.filter { it.block.status == BlockStatus.DONE }.maxOfOrNull { it.block.id }
        val blocksUi = blocks.map { b ->
            BlockUi(
                blockId = b.block.id,
                exerciseId = b.block.exerciseId,
                name = b.exercise.name,
                imagePath = b.exercise.imagePath,
                sets = b.sets.sortedBy { it.orderInExercise },
                isActive = b.block.status == BlockStatus.ACTIVE,
                showResume = b.block.status == BlockStatus.DONE && b.block.id == lastDoneId,
            )
        }
        return SessionUi(
            sessionId = session.id,
            exerciseCount = blocksUi.size,
            activeBlockId = blocksUi.firstOrNull { it.isActive }?.blockId,
            blocks = blocksUi,
            repsInput = reps,
            weightInput = weight,
            weightPrefilled = prefilled,
        )
    }

    fun startSession() {
        viewModelScope.launch { repo.startSession() }
    }

    fun setRepsInput(text: String) {
        repsInput.value = text
    }

    fun setWeightInput(text: String) {
        weightInput.value = text
        weightPrefilled.value = false
    }

    fun addSet() {
        val active = uiState.value.session?.activeBlockId ?: return
        val reps = repsInput.value.toIntOrNull() ?: 10
        val weight = weightInput.value.replace(',', '.').toDoubleOrNull() ?: 0.0
        if (reps <= 0) return
        viewModelScope.launch {
            repo.addSet(active, reps, weight)
            // Следующий подход подставляет значение этого подхода.
            weightInput.value = formatWeight(weight)
            weightPrefilled.value = true
            repsInput.value = reps.toString()
        }
    }

    fun finishExercise(blockId: Long) {
        viewModelScope.launch {
            val session = uiState.value.session ?: return@launch
            val block = session.blocks.firstOrNull { it.blockId == blockId } ?: return@launch
            if (block.sets.isEmpty()) {
                noSetsBlockId.value = blockId
            } else {
                repo.finishBlock(blockId)
            }
        }
    }

    /** A6: «Завершить без записи» — упражнение в сессию не попадает. */
    fun confirmNoSets() {
        val blockId = noSetsBlockId.value ?: return
        viewModelScope.launch {
            repo.deleteBlock(blockId)
            noSetsBlockId.value = null
        }
    }

    fun continueExercise() {
        noSetsBlockId.value = null
    }

    fun resumeBlock(blockId: Long) {
        viewModelScope.launch {
            val session = uiState.value.session ?: return@launch
            val block = session.blocks.firstOrNull { it.blockId == blockId } ?: return@launch
            repo.resumeBlock(blockId)
            prefillFor(block.exerciseId)
        }
    }

    /** A5: подтверждение только при наличии упражнений; пустая сессия не порождает запись. */
    fun completeWorkout() {
        viewModelScope.launch {
            val session = uiState.value.session ?: return@launch
            if (session.blocks.isEmpty()) {
                repo.deleteSession(session.sessionId)
            } else {
                finishConfirm.value = true
            }
        }
    }

    fun confirmComplete() {
        val sessionId = uiState.value.session?.sessionId ?: return
        viewModelScope.launch {
            repo.completeSession(sessionId)
            finishConfirm.value = false
        }
    }

    fun cancelComplete() {
        finishConfirm.value = false
    }

    // ——— Issue #6: правка и удаление подхода ———

    /** Открыть диалог правки: подставить текущие значения подхода. */
    fun openEditSet(blockId: Long, setId: Long, weightKg: Double, reps: Int) {
        editBlockId.value = blockId
        editSetId.value = setId
        editWeight.value = formatWeight(weightKg)
        editReps.value = reps.toString()
    }

    fun setEditWeight(text: String) {
        editWeight.value = text
    }

    fun setEditReps(text: String) {
        editReps.value = text
    }

    /** «Сохранить»: обновить подход; пустые значения не сохраняются. */
    fun saveEditSet() {
        val setId = editSetId.value ?: return
        val reps = editReps.value.toIntOrNull() ?: return
        val weight = editWeight.value.replace(',', '.').toDoubleOrNull() ?: return
        if (reps <= 0) return
        viewModelScope.launch {
            repo.editSet(setId, weight, reps)
            closeEditDialog()
        }
    }

    /** «Удалить»: убрать подход; если блок остался без подходов — A6. */
    fun deleteSet() {
        val setId = editSetId.value ?: return
        val blockId = editBlockId.value ?: return
        viewModelScope.launch {
            repo.removeSet(setId)
            closeEditDialog()
            // Если удалён последний подход блока — показать A6.
            val session = uiState.value.session ?: return@launch
            val block = session.blocks.firstOrNull { it.blockId == blockId }
            if (block != null && block.sets.size == 1) {
                noSetsBlockId.value = blockId
            }
        }
    }

    fun cancelEditSet() {
        closeEditDialog()
    }

    private fun closeEditDialog() {
        editBlockId.value = null
        editSetId.value = null
        editWeight.value = ""
        editReps.value = ""
    }
}
