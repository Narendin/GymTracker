package com.gymtracker.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ChangeDao {
    @Insert
    suspend fun insertGroup(group: ChangeGroup): Long

    @Insert
    suspend fun insertPhoto(photo: ChangePhoto): Long

    @Query("DELETE FROM change_photo WHERE id = :id")
    suspend fun deletePhoto(id: Long)

    @Transaction
    @Query(
        "SELECT cg.*, " +
            "(SELECT COUNT(*) FROM change_photo cp WHERE cp.groupId = cg.id) AS photoCount, " +
            "(SELECT MAX(cp.takenAt) FROM change_photo cp WHERE cp.groupId = cg.id) AS lastPhotoAt " +
            "FROM change_group cg ORDER BY cg.id"
    )
    fun groups(): Flow<List<ChangeGroupRow>>

    @Query("SELECT * FROM change_photo WHERE groupId = :groupId ORDER BY takenAt DESC, id DESC")
    fun photosForGroup(groupId: Long): Flow<List<ChangePhoto>>
}

data class ChangeGroupRow(
    @Embedded val group: ChangeGroup,
    val photoCount: Int,
    val lastPhotoAt: Long?,
)
