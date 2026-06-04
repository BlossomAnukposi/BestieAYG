package com.bayg.services.storage.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bayg.services.storage.entities.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE firebaseUid = :uid LIMIT 1")
    suspend fun getByFirebaseUid(uid: String): User?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getByRoomId(id: Long): User?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)

    @Query("UPDATE users SET firebaseUid = :uid WHERE email = :email")
    suspend fun linkFirebaseUid(email: String, uid: String)
}