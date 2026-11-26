package com.gymtracker.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: WorkoutSession): Long

    @Update
    suspend fun updateSession(session: WorkoutSession)

    @Query("UPDATE workout_session SET finishedAtMillis = :ts WHERE id = :id")
    suspend fun finishSession(id: Long, ts: Long)

    @Query("UPDATE workout_session SET comment = :comment WHERE id = :id")
    suspend fun setSessionComment(id: Long, comment: String?)

    @Query("DELETE FROM workout_session WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("SELECT * FROM workout_session ORDER BY startMillis DESC LIMIT 1")
    fun lastSession(): Flow<WorkoutSession?>

    @Query("SELECT * FROM workout_session ORDER BY startMillis DESC LIMIT 1")
    suspend fun lastSessionOnce(): WorkoutSession?

    @Query("SELECT COUNT(*) FROM workout_session WHERE startMillis >= :since")
    fun countSince(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM workout_session")
    fun countAll(): Flow<Int>

    @Query("SELECT * FROM workout_session WHERE finishedAtMillis IS NULL ORDER BY startMillis DESC LIMIT 1")
    fun activeSession(): Flow<WorkoutSession?>

    @Query("SELECT * FROM workout_session WHERE finishedAtMillis IS NULL ORDER BY startMillis DESC LIMIT 1")
    suspend fun activeSessionOnce(): WorkoutSession?

    @Insert
    suspend fun insertBlock(block: WorkoutExercise): Long

    @Update
    suspend fun updateBlock(block: WorkoutExercise)

    @Query("UPDATE workout_exercise SET status = :status WHERE id = :id")
    suspend fun setBlockStatus(id: Long, status: BlockStatus)

    @Query("UPDATE workout_exercise SET comment = :comment WHERE id = :id")
    suspend fun setExerciseComment(id: Long, comment: String?)

    @Query("DELETE FROM workout_exercise WHERE id = :id")
    suspend fun deleteBlock(id: Long)

    @Query("SELECT MAX(orderInSession) FROM workout_exercise WHERE sessionId = :sessionId")
    suspend fun maxBlockOrder(sessionId: Long): Int?

    @Insert
    suspend fun insertSet(set: SetEntry): Long

    @Query("UPDATE set_entry SET weightKg = :weightKg, reps = :reps WHERE id = :id")
    suspend fun updateSet(id: Long, weightKg: Double, reps: Int)

    @Query("DELETE FROM set_entry WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query("SELECT MAX(orderInExercise) FROM set_entry WHERE workoutExerciseId = :blockId")
    suspend fun maxSetOrder(blockId: Long): Int?

    @Query(
        "SELECT se.weightKg FROM set_entry se " +
            "JOIN workout_exercise we ON we.id = se.workoutExerciseId " +
            "WHERE we.exerciseId = :exerciseId AND se.reps > 0 " +
            "ORDER BY se.id DESC LIMIT 1"
    )
    suspend fun lastWeightForExercise(exerciseId: Long): Double?

    @Query(
        "SELECT se.reps FROM set_entry se " +
            "JOIN workout_exercise we ON we.id = se.workoutExerciseId " +
            "WHERE we.exerciseId = :exerciseId AND se.reps > 0 " +
            "ORDER BY se.id DESC LIMIT 1"
    )
    suspend fun lastRepsForExercise(exerciseId: Long): Int?

    @Transaction
    @Query("SELECT * FROM workout_exercise WHERE sessionId = :sessionId ORDER BY orderInSession, id")
    fun blocksForSession(sessionId: Long): Flow<List<BlockWithExercise>>

    @Transaction
    @Query(
        "SELECT ws.*, " +
            "(SELECT COUNT(DISTINCT we.exerciseId) FROM workout_exercise we WHERE we.sessionId = ws.id) AS exerciseCount, " +
            "(SELECT SUM(se.weightKg * se.reps) FROM set_entry se JOIN workout_exercise we ON we.id = se.workoutExerciseId WHERE we.sessionId = ws.id) AS tonnage " +
            "FROM workout_session ws WHERE ws.finishedAtMillis IS NOT NULL " +
            "ORDER BY ws.startMillis DESC"
    )
    fun finishedSessions(): Flow<List<SessionRow>>

    @Transaction
    @Query(
        "SELECT ws.*, " +
            "(SELECT COUNT(DISTINCT we.exerciseId) FROM workout_exercise we WHERE we.sessionId = ws.id) AS exerciseCount, " +
            "(SELECT SUM(se.weightKg * se.reps) FROM set_entry se JOIN workout_exercise we ON we.id = se.workoutExerciseId WHERE we.sessionId = ws.id) AS tonnage " +
            "FROM workout_session ws WHERE ws.id = :sessionId"
    )
    fun sessionById(sessionId: Long): Flow<SessionRow>

    /**
     * Основные мышцы упражнений каждой завершённой тренировки — подписи строк журнала:
     * по ним видно, чем тренировались, без входа в детали.
     */
    @Transaction
    @Query(
        "SELECT DISTINCT we.sessionId AS sessionId, m.id AS muscleId, m.name AS name, m.areaId AS areaId " +
            "FROM workout_exercise we " +
            "JOIN exercise_muscle em ON em.exerciseId = we.exerciseId AND em.role = 'PRIMARY' " +
            "JOIN muscle m ON m.id = em.muscleId " +
            "JOIN workout_session ws ON ws.id = we.sessionId AND ws.finishedAtMillis IS NOT NULL"
    )
    fun sessionPrimaryMuscles(): Flow<List<SessionPrimaryMuscle>>

    /** График карточки: максимальный рабочий вес (или повторы) по датам завершённых тренировок. */
    @Query(
        "SELECT ws.startMillis AS dateMillis, MAX(se.weightKg) AS maxWeight, MAX(se.reps) AS maxReps " +
            "FROM set_entry se JOIN workout_exercise we ON we.id = se.workoutExerciseId " +
            "JOIN workout_session ws ON ws.id = we.sessionId " +
            "WHERE we.exerciseId = :exerciseId AND ws.finishedAtMillis IS NOT NULL " +
            "GROUP BY ws.id ORDER BY ws.startMillis"
    )
    fun maxWeightPoints(exerciseId: Long): Flow<List<WeightPoint>>

    /** «История упражнения»: тренировки, содержащие это упражнение. */
    @Query(
        "SELECT ws.id AS sessionId, ws.startMillis AS startMillis " +
            "FROM workout_session ws JOIN workout_exercise we ON we.sessionId = ws.id " +
            "WHERE we.exerciseId = :exerciseId AND ws.finishedAtMillis IS NOT NULL " +
            "ORDER BY ws.startMillis DESC"
    )
    fun exerciseSessions(exerciseId: Long): Flow<List<ExerciseSessionRow>>

    /** Подходы упражнения по сессиям — bubble-лента в истории карточки. */
    @Query(
        "SELECT ws.id AS sessionId, ws.startMillis AS startMillis, se.weightKg AS weightKg, " +
            "se.reps AS reps, se.orderInExercise AS orderInExercise " +
            "FROM set_entry se JOIN workout_exercise we ON we.id = se.workoutExerciseId " +
            "JOIN workout_session ws ON ws.id = we.sessionId " +
            "WHERE we.exerciseId = :exerciseId AND ws.finishedAtMillis IS NOT NULL " +
            "ORDER BY ws.startMillis DESC, se.orderInExercise"
    )
    fun exerciseHistorySets(exerciseId: Long): Flow<List<ExerciseHistorySet>>
}

data class ExerciseHistorySet(
    val sessionId: Long,
    val startMillis: Long,
    val weightKg: Double,
    val reps: Int,
    val orderInExercise: Int,
)

data class WeightPoint(val dateMillis: Long, val maxWeight: Double, val maxReps: Int)

data class ExerciseSessionRow(val sessionId: Long, val startMillis: Long)

/** Основная мышца упражнения завершённой тренировки (для подписи строки журнала). */
data class SessionPrimaryMuscle(
    val sessionId: Long,
    val muscleId: Long,
    val name: String,
    val areaId: Long,
)

data class BlockWithExercise(
    @Embedded val block: WorkoutExercise,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: Exercise,
    @Relation(parentColumn = "id", entityColumn = "workoutExerciseId")
    val sets: List<SetEntry>,
)

data class SessionRow(
    @Embedded val session: WorkoutSession,
    val exerciseCount: Int,
    val tonnage: Double?,
)
