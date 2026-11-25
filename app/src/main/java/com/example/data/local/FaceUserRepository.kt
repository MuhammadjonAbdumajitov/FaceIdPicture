package com.example.data.local

import kotlinx.coroutines.flow.Flow

class FaceUserRepository(private val faceUserDao: FaceUserDao) {

    val users: Flow<List<FaceUser>> = faceUserDao.observeUsers()

    suspend fun getPrimaryUser(): FaceUser? = faceUserDao.getPrimaryUser()

    suspend fun saveReferenceUser(fullName: String, embedding: FloatArray) {
        val existing = faceUserDao.getUserByName(fullName)
        val entity = if (existing != null) {
            existing.copy(
                embedding = embedding,
                updatedAt = System.currentTimeMillis(),
            )
        } else {
            FaceUser(
                fullName = fullName,
                embedding = embedding,
            )
        }
        faceUserDao.upsertUser(entity)
    }
}
