package com.bayg.services.storage.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index("firebaseUid", unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firebaseUid: String? = null,
    val email: String,
    val firstName: String,
    val lastName: String,
    val createdAt: Long = System.currentTimeMillis()
)