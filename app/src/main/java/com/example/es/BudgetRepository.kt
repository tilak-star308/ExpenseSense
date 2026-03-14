package com.example.es

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class BudgetRepository(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao
) {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseDatabase = FirebaseDatabase.getInstance()

    private fun getUsername(): String? {
        return firebaseAuth.currentUser?.email?.substringBefore("@")
    }

    fun getBudget(monthYear: String): Budget? {
        return budgetDao.getBudget(monthYear)
    }

    fun setBudget(totalAmount: Double, monthYear: String) {
        Thread {
            // Calculate current spent for this month
            val allTransactions = transactionDao.getAllTransactions()
            val currentMonthSpent = allTransactions.filter {
                val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                val m = cal.get(Calendar.MONTH) + 1 // 0-indexed to 1-indexed
                val y = cal.get(Calendar.YEAR)
                val formatted = "%02d-%d".format(m, y)
                formatted == monthYear
            }.sumOf { it.amount }

            val remaining = totalAmount - currentMonthSpent
            val budget = Budget(monthYear, totalAmount, remaining)

            // Update local Room
            budgetDao.insertBudget(budget)
            
            // Sync to Firebase
            val username = getUsername()
            if (username != null) {
                firebaseDatabase.getReference("users/$username/budgets/$monthYear")
                    .setValue(budget)
            }
        }.start()
    }

    fun deductFromBudget(monthYear: String, amount: Double) {
        Thread {
            val currentBudget = budgetDao.getBudget(monthYear)
            if (currentBudget != null) {
                val newRemaining = currentBudget.remainingBudget - amount
                budgetDao.updateRemainingBudget(monthYear, newRemaining)
                
                // Sync updated remaining budget to Firebase
                val username = getUsername()
                if (username != null) {
                    firebaseDatabase.getReference("users/$username/budgets/$monthYear/remainingBudget")
                        .setValue(newRemaining)
                }
            }
        }.start()
    }

    fun reimburseBudget(monthYear: String, amount: Double) {
        Thread {
            val currentBudget = budgetDao.getBudget(monthYear)
            if (currentBudget != null) {
                val newRemaining = currentBudget.remainingBudget + amount
                budgetDao.updateRemainingBudget(monthYear, newRemaining)
                
                // Sync updated remaining budget to Firebase
                val username = getUsername()
                if (username != null) {
                    firebaseDatabase.getReference("users/$username/budgets/$monthYear/remainingBudget")
                        .setValue(newRemaining)
                }
            }
        }.start()
    }
}
