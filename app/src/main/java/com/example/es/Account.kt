package com.example.es

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey
    val name: String = "",
    val type: String? = null,
    val balance: Double = 0.0
)
