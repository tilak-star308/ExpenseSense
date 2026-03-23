package com.amshu.expensesense

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Expense(
    val name: String,
    val amount: Double,
    val date: String
) : Parcelable
