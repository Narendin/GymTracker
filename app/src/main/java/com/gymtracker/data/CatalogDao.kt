package com.gymtracker.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExercise(exercise: Exercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: ExerciseMuscleLink)

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Query("UPDATE exercise SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteExercise(id: Long)

    @Query("SELECT * FROM muscle_area ORDER BY id")
    fun areas(): Flow<List<MuscleArea>>

    @Query("SELECT * FROM muscle WHERE areaId = :areaId ORDER BY id")
    fun musclesForArea(areaId: Long): Flow<List<Muscle>>

    @Query("SELECT * FROM exercise WHERE isDeleted = 0 ORDER BY name COLLATE NOCASE")
    fun activeExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercise WHERE id = :id")
    fun exerciseById(id: Long): Flow<Exercise?>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun exerciseByIdOnce(id: Long): Exercise?

    /**
     * A4: список упражнений области; пустой запрос = вся область.
     * Поиск находит и по названию, и по мышце (основная или вторичная).
     * Скрытые упражнения экрана «Мышцы» в выбор не попадают.
     */
    @Transaction
    @Query(
        "SELECT DISTINCT e.* FROM exercise e " +
            "JOIN exercise_muscle em ON em.exerciseId = e.id " +
            "JOIN muscle m ON m.id = em.muscleId " +
            "JOIN muscle_area a ON a.id = m.areaId " +
            "WHERE e.isDeleted = 0 AND e.isHidden = 0 AND a.id = :areaId AND (" +
            ":query = '' OR e.name LIKE '%' || :query || '%' COLLATE NOCASE " +
            "OR m.name LIKE '%' || :query || '%' COLLATE NOCASE) " +
            "ORDER BY e.name COLLATE NOCASE"
    )
    fun exercisesByArea(areaId: Long, query: String): Flow<List<Exercise>>

    /**
     * A4: фильтр «Все» — список всех упражнений без привязки к области;
     * тот же поиск по названию и по любой мышце; скрытые упражнения исключены.
     */
    @Transaction
    @Query(
        "SELECT DISTINCT e.* FROM exercise e " +
            "JOIN exercise_muscle em ON em.exerciseId = e.id " +
            "JOIN muscle m ON m.id = em.muscleId " +
            "WHERE e.isDeleted = 0 AND e.isHidden = 0 AND (" +
            ":query = '' OR e.name LIKE '%' || :query || '%' COLLATE NOCASE " +
            "OR m.name LIKE '%' || :query || '%' COLLATE NOCASE) " +
            "ORDER BY e.name COLLATE NOCASE"
    )
    fun exercisesAll(query: String): Flow<List<Exercise>>

    /** Подписи мышц для строк списка: первичные + вторичные. */
    @Transaction
    @Query(
        "SELECT em.exerciseId AS exerciseId, m.name AS muscle, em.role AS role " +
            "FROM exercise_muscle em JOIN muscle m ON m.id = em.muscleId " +
            "ORDER BY CASE em.role WHEN 'PRIMARY' THEN 0 ELSE 1 END, m.id"
    )
    fun muscleLabels(): Flow<List<MuscleLabel>>

    /**
     * C1: упражнения выбранной мышцы (основная или вторичная), по алфавиту;
     * скрытые и видимые — отдельными потоками (блок «Скрытое»).
     */
    @Transaction
    @Query(
        "SELECT DISTINCT e.* FROM exercise e " +
            "JOIN exercise_muscle em ON em.exerciseId = e.id " +
            "WHERE em.muscleId = :muscleId AND e.isDeleted = 0 AND e.isHidden = :hidden " +
            "ORDER BY e.name COLLATE NOCASE"
    )
    fun exercisesForMuscle(muscleId: Long, hidden: Boolean): Flow<List<Exercise>>

    /** Иконка глаза в строке упражнения: скрыть / вернуть. */
    @Query("UPDATE exercise SET isHidden = :hidden WHERE id = :id")
    suspend fun setExerciseHidden(id: Long, hidden: Boolean)

    @Transaction
    @Query("SELECT * FROM exercise_muscle WHERE exerciseId = :exerciseId")
    fun linksForExercise(exerciseId: Long): Flow<List<ExerciseMuscleLink>>

    @Query("DELETE FROM exercise_muscle WHERE exerciseId = :exerciseId")
    suspend fun deleteLinks(exerciseId: Long)

    @Query("SELECT * FROM muscle ORDER BY areaId, id")
    fun allMuscles(): Flow<List<Muscle>>

    /** Мышцы упражнения с названиями — для карточки и формы. */
    @Transaction
    @Query(
        "SELECT m.id AS muscleId, m.name AS name, em.role AS role " +
            "FROM exercise_muscle em JOIN muscle m ON m.id = em.muscleId " +
            "WHERE em.exerciseId = :exerciseId " +
            "ORDER BY CASE em.role WHEN 'PRIMARY' THEN 0 ELSE 1 END, m.id"
    )
    fun musclesForExercise(exerciseId: Long): Flow<List<MuscleWithRole>>
}

data class MuscleWithRole(val muscleId: Long, val name: String, val role: MuscleRole)

data class MuscleLabel(
    val exerciseId: Long,
    val muscle: String,
    val role: MuscleRole,
)
