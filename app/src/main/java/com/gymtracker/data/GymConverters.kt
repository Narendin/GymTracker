package com.gymtracker.data

import androidx.room.TypeConverter

class GymConverters {
    @TypeConverter
    fun exerciseTypeToString(value: ExerciseType): String = value.name

    @TypeConverter
    fun stringToExerciseType(value: String): ExerciseType = ExerciseType.valueOf(value)

    @TypeConverter
    fun muscleRoleToString(value: MuscleRole): String = value.name

    @TypeConverter
    fun stringToMuscleRole(value: String): MuscleRole = MuscleRole.valueOf(value)

    @TypeConverter
    fun blockStatusToString(value: BlockStatus): String = value.name

    @TypeConverter
    fun stringToBlockStatus(value: String): BlockStatus = BlockStatus.valueOf(value)
}
