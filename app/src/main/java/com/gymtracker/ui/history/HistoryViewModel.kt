package com.gymtracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.GymRepository
import com.gymtracker.data.SessionPrimaryMuscle
import com.gymtracker.data.SessionRow
import com.gymtracker.ui.components.CommentUi
import com.gymtracker.ui.components.COMMENT_LIMIT
import com.gymtracker.ui.components.commentLength
import com.gymtracker.util.exercisesLabel
import com.gymtracker.util.formatDateFull
import com.gymtracker.util.formatSet
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
    /** Комментарий тренировки под датой; null — комментария нет, строка его не показывает. */
    val comment: String?,
)

data class HistoryBlockUi(
    val blockId: Long,
    val exerciseId: Long,
    val name: String,
    val imagePath: String?,
    /** bubble-подход: «5 × 85 кг» или «12 повт.». */
    val chips: List<String>,
    /** Комментарий упражнения под чипами; null — комментария нет. */
    val comment: String?,
)

data class DetailUi(
    val sessionId: Long,
    val dateLabel: String,
    val subLabel: String,
    val blocks: List<HistoryBlockUi>,
    /** Комментарий тренировки над блоками; null — комментария нет. */
    val comment: String?,
)

data class HistoryUiState(
    val filter: PeriodFilter = PeriodFilter.ALL,
    val rows: List<HistoryRowUi> = emptyList(),
    val detail: DetailUi? = null,
    val showDeleteConfirm: Boolean = false,
    /** Диалог комментария (тренировка или блок упражнения). */
    val commentDialog: CommentUi? = null,
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
    // Состояние диалога комментария: поле живёт в состоянии экрана, как поле диалога
    // правки — набранный текст должен появляться в нём.
    private val commentDialog = MutableStateFlow<CommentUi?>(null)

    // Диалог комментария вне ключа flatMapLatest: каждое нажатие клавиши в поле не
    // должна переподписывать запросы деталей (sessionById + blocksForSession).
    val uiState: StateFlow<HistoryUiState> = combine(
        combine(
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
                                            blockId = it.block.id,
                                            exerciseId = it.exercise.id,
                                            name = it.exercise.name,
                                            imagePath = it.exercise.imagePath,
                                            chips = it.sets
                                                .sortedBy { s -> s.orderInExercise }
                                                .map { s ->
                                                    formatSet(s.weightKg, s.reps, it.exercise.isBodyweight)
                                                },
                                            comment = it.block.comment,
                                        )
                                    },
                            comment = sessionRow.session.comment,
                        ),
                        showDeleteConfirm = base.showDelete,
                    )
                }
            }
            },
        commentDialog,
    ) { state, cd ->
        state.copy(commentDialog = cd)
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
        comment = sessionRow.session.comment,
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
        commentDialog.value = null
    }

    // ——— Комментарии тренировки и упражнения: правка из деталей записи ———

    /** Открыть диалог комментария: blockId = null — комментарий тренировки. */
    fun openComment(blockId: Long?) {
        val detail = uiState.value.detail ?: return
        val text = if (blockId == null) {
            detail.comment
        } else {
            detail.blocks.firstOrNull { it.blockId == blockId }?.comment
        }
        commentDialog.value = CommentUi(isSession = blockId == null, blockId = blockId, text = text ?: "")
    }

    /** Ввод поля диалога: изменение, выходящее за лимит, отклоняется — поле не
     * меняется, знак, набитый после 250-го, не попадает в комментарий. */
    fun setCommentText(text: String) {
        val dialog = commentDialog.value ?: return
        if (commentLength(text) > COMMENT_LIMIT) return
        commentDialog.value = dialog.copy(text = text)
    }

    /** «Сохранить»: пустое поле записывает отсутствие комментария. */
    fun saveComment() {
        val dialog = commentDialog.value ?: return
        val value = dialog.text.trim().ifEmpty { null }
        viewModelScope.launch {
            if (dialog.isSession) {
                val sessionId = uiState.value.detail?.sessionId ?: return@launch
                repo.setSessionComment(sessionId, value)
            } else {
                val blockId = dialog.blockId ?: return@launch
                repo.setExerciseComment(blockId, value)
            }
            commentDialog.value = null
        }
    }

    /** «Удалить»: очищает комментарий; строка остаётся с подсказом. */
    fun clearComment() {
        val dialog = commentDialog.value ?: return
        viewModelScope.launch {
            if (dialog.isSession) {
                val sessionId = uiState.value.detail?.sessionId ?: return@launch
                repo.setSessionComment(sessionId, null)
            } else {
                val blockId = dialog.blockId ?: return@launch
                repo.setExerciseComment(blockId, null)
            }
            commentDialog.value = null
        }
    }

    fun cancelComment() {
        commentDialog.value = null
    }

    /** Подтверждение удаления не открывается поверх открытого диалога комментария. */
    fun requestDelete() {
        if (commentDialog.value != null) return
        showDeleteConfirm.value = true
    }

    fun confirmDelete() {
        val id = detailId.value ?: return
        showDeleteConfirm.value = false
        detailId.value = null
        commentDialog.value = null
        viewModelScope.launch {
            repo.deleteSession(id)
        }
    }

    fun cancelDelete() {
        showDeleteConfirm.value = false
    }
}
