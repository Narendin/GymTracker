package com.gymtracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ExerciseType { CATALOG, CUSTOM }

enum class MuscleRole { PRIMARY, SECONDARY }

enum class BlockStatus { ACTIVE, DONE }

@Entity(tableName = "muscle_area")
data class MuscleArea(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(
    tableName = "muscle",
    foreignKeys = [ForeignKey(
        entity = MuscleArea::class,
        parentColumns = ["id"],
        childColumns = ["areaId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("areaId")],
)
data class Muscle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val areaId: Long,
)

@Entity(tableName = "exercise")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: ExerciseType,
    val imagePath: String? = null,
    val isDeleted: Boolean = false,
    /** C1: упражнение скрыто с экрана «Мышцы» (в блоке «Скрытое» своей группы). */
    val isHidden: Boolean = false,
)

@Entity(
    tableName = "exercise_muscle",
    primaryKeys = ["exerciseId", "muscleId"],
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Muscle::class,
            parentColumns = ["id"],
            childColumns = ["muscleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("muscleId")],
)
data class ExerciseMuscleLink(
    val exerciseId: Long,
    val muscleId: Long,
    val role: MuscleRole,
)

@Entity(tableName = "workout_session")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMillis: Long,
    val finishedAtMillis: Long? = null,
    val comment: String? = null,
)

@Entity(
    tableName = "workout_exercise",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val orderInSession: Int,
    val status: BlockStatus,
)

@Entity(
    tableName = "set_entry",
    foreignKeys = [ForeignKey(
        entity = WorkoutExercise::class,
        parentColumns = ["id"],
        childColumns = ["workoutExerciseId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("workoutExerciseId")],
)
data class SetEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutExerciseId: Long,
    val orderInExercise: Int,
    val weightKg: Double,
    val reps: Int,
)

@Entity(tableName = "change_group")
data class ChangeGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val isPreset: Boolean,
)

@Entity(
    tableName = "change_photo",
    foreignKeys = [ForeignKey(
        entity = ChangeGroup::class,
        parentColumns = ["id"],
        childColumns = ["groupId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("groupId")],
)
data class ChangePhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val imagePath: String,
    val takenAt: Long,
)
