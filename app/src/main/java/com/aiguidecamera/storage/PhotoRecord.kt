package com.aiguidecamera.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 저장된 사진 한 장(필터본)과 그 원본을 잇는 기록.
 * 사후 편집으로 만든 필터본도 새 레코드가 되며, 같은 originalUri를 공유한다.
 */
@Entity(tableName = "photo_record")
data class PhotoRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filteredUri: String,
    val originalUri: String?,
    val filterId: String,
    val filterIntensity: Float,
    val skinSmoothLevel: Float,
    val mode: String,
    val createdAt: Long,
)
