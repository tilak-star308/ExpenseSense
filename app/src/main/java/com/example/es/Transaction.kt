package com.example.es

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val accountName: String, // Associate with an account
    val timestamp: Long,
    val firebaseId: String? = null
)
