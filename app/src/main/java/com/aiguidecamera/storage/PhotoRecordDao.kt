package com.aiguidecamera.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** PhotoRecord 조회·추가. 목록은 항상 최신순. */
@Dao
interface PhotoRecordDao {

    @Insert
    suspend fun insert(record: PhotoRecord): Long

    @Query("SELECT * FROM photo_record ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<PhotoRecord>>

    @Query("SELECT * FROM photo_record ORDER BY createdAt DESC, id DESC LIMIT 1")
    fun observeLatest(): Flow<PhotoRecord?>

    @Query("SELECT * FROM photo_record WHERE id = :id")
    suspend fun getById(id: Long): PhotoRecord?
}
