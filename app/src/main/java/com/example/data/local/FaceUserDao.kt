package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FaceUserDao {

    @Query("SELECT * FROM face_users ORDER BY updatedAt DESC")
    fun observeUsers(): Flow<List<FaceUser>>

    @Query("SELECT * FROM face_users ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getPrimaryUser(): FaceUser?

    @Query("SELECT * FROM face_users WHERE fullName = :fullName LIMIT 1")
    suspend fun getUserByName(fullName: String): FaceUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: FaceUser)

    @Query("DELETE FROM face_users")
    suspend fun clearAll()
}
