package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "face_users")
data class FaceUser(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val embedding: FloatArray,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceUser) return false
        return id == other.id &&
            fullName == other.fullName &&
            embedding.contentEquals(other.embedding) &&
            updatedAt == other.updatedAt
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + fullName.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}
