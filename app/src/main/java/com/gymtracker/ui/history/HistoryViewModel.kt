package com.gymtracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.GymRepository
import com.gymtracker.data.SessionPrimaryMuscle
import com.gymtracker.data.SessionRow
import com.gymtracker.util.exercisesLabel
import com.gymtracker.util.formatDateFull
import com.gymtracker.util.formatWeight
import com.gymtracker.util.groupNumber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PeriodFilter { WEEK, MONTH, YEAR, ALL }

data class HistoryRowUi(
    val sessionId: Long,
    val dateLabel: String,
    val exercisesLabel: String,
    val tonnageLabel: String,
    /** Основные мышцы тренировки: «грудь · трицепс» — по ним видно, чем тренировались. */
    val musclesLabel: String,
)

data class HistoryBlockUi(
    val exerciseId: Long,
    val name: String,
    val imagePath: String?,
    /** bubble-подход: «85 кг × 5». */
    val chips: List<String>,
)

data class DetailUi(
    val sessionId: Long,
    val dateLabel: String,
    val subLabel: String,
    val blocks: List<HistoryBlockUi>,
)

data class HistoryUiState(
    val filter: PeriodFilter = PeriodFilter.ALL,
    val rows: List<HistoryRowUi> = emptyList(),
    val detail: DetailUi? = null,
    val showDeleteConfirm: Boolean = false,
)

private data class Base(
    val filter: PeriodFilter,
    val detailId: Long?,
    val showDelete: Boolean,
    val sessions: List<SessionRow>,
    /** Основные мышцы по тренировкам, порядок — по каталогу мышц (область, id). */
    val musclesBySession: Map<Long, List<SessionPrimaryMuscle>>,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HistoryViewModel(private val repo: GymRepository) : ViewModel() {

    private val filter = MutableStateFlow(PeriodFilter.ALL)
    private val detailId = MutableStateFlow<Long?>(null)
    private val showDeleteConfirm = MutableStateFlow(false)

    val uiState: StateFlow<HistoryUiState> = combine(
        filter,
        detailId,
        showDeleteConfirm,
        repo.finishedSessions(),
        repo.sessionPrimaryMuscles(),
    ) { f, d, del, sessions, muscles ->
        Base(f, d, del, sessions, muscles.groupBy { it.sessionId })
    }
        .flatMapLatest { base ->
            val rows = base.sessions
                .filter { inPeriod(it.session.startMillis, base.filter) }
                .map { rowUi(it, base.musclesBySession[it.session.id] ?: emptyList()) }
            if (base.detailId == null) {
                flow {
                    emit(
                        HistoryUiState(
                            base.filter,
                            rows,
                            null,
                            base.showDelete,
                        )
                    )
                }
            } else {
                combine(
                    repo.sessionById(base.detailId),
                    repo.blocksForSession(base.detailId),
                ) { sessionRow, blocks ->
                    HistoryUiState(
                        filter = base.filter,
                        rows = rows,
                        detail = DetailUi(
                            sessionId = base.detailId,
                            dateLabel = formatDateFull(sessionRow.session.startMillis),
                            subLabel = "${exercisesLabel(sessionRow.exerciseCount)} · ${tonnage(sessionRow.tonnage)}",
                            blocks = blocks
                                .sortedBy { it.block.orderInSession }
                                .map {
                                    HistoryBlockUi(
                                        exerciseId = it.exercise.id,
                                        name = it.exercise.name,
                                        imagePath = it.exercise.imagePath,
                                        chips = it.sets
                                            .sortedBy { s -> s.orderInExercise }
                                            .map { s -> "${formatWeight(s.weightKg)} кг × ${s.reps}" },
                                    )
                                },
                        ),
                        showDeleteConfirm = base.showDelete,
                    )
                }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            HistoryUiState(),
        )

    private fun inPeriod(startMillis: Long, filter: PeriodFilter): Boolean {
        val day = 24L * 60 * 60 * 1000
        val limit = when (filter) {
            PeriodFilter.WEEK -> System.currentTimeMillis() - 7 * day
            PeriodFilter.MONTH -> System.currentTimeMillis() - 30 * day
            PeriodFilter.YEAR -> System.currentTimeMillis() - 365 * day
            PeriodFilter.ALL -> Long.MIN_VALUE
        }
        return startMillis >= limit
    }

    private fun rowUi(sessionRow: SessionRow, muscles: List<SessionPrimaryMuscle>): HistoryRowUi = HistoryRowUi(
        sessionId = sessionRow.session.id,
        dateLabel = formatDateFull(sessionRow.session.startMillis),
        exercisesLabel = exercisesLabel(sessionRow.exerciseCount),
        tonnageLabel = tonnage(sessionRow.tonnage),
        musclesLabel = muscles
            .sortedWith(compareBy({ it.areaId }, { it.muscleId }))
            .joinToString(" · ") { it.name.lowercase() },
    )

    private fun tonnage(tonnage: Double?): String {
        val value = Math.round((tonnage ?: 0.0)).toInt()
        return "${groupNumber(value)} кг"
    }

    fun setFilter(f: PeriodFilter) {
        filter.value = f
    }

    fun openDetail(sessionId: Long) {
        detailId.value = sessionId
    }

    fun closeDetail() {
        detailId.value = null
        showDeleteConfirm.value = false
    }

    fun requestDelete() {
        showDeleteConfirm.value = true
    }

    fun confirmDelete() {
        val id = detailId.value ?: return
        showDeleteConfirm.value = false
        detailId.value = null
        viewModelScope.launch {
            repo.deleteSession(id)
        }
    }

    fun cancelDelete() {
        showDeleteConfirm.value = false
    }
}
