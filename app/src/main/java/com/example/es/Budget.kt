package com.example.es

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val monthYear: String, // format: MM-yyyy, e.g. "03-2026"
    val totalBudget: Double,
    val remainingBudget: Double
)
