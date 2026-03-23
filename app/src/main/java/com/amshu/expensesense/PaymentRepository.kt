package com.amshu.expensesense

import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class PaymentRepository(
    private val database: AppDatabase,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val debitCardDao: DebitCardDao,
    private val creditCardDao: CreditCardDao,
    private val budgetDao: BudgetDao
) {

    fun saveExpense(
        transaction: Transaction,
        paymentMethod: String,
        referenceId: String?,
        username: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val amount = transaction.amount
        val timestamp = transaction.timestamp
        val monthYear = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date(timestamp))

        // 1. Perform Room Transaction for local data integrity
        Thread {
            try {
                database.runInTransaction {
                    // Deduct from correct source
                    when (paymentMethod) {
                        "Cash" -> {
                            val cashAccount = accountDao.getAccountByName("Cash")
                                ?: throw Exception("Cash account not found")
                            accountDao.updateBalance("Cash", cashAccount.balance - amount)
                        }
                        "UPI" -> {
                            val bankAccount = accountDao.getAccountByName(referenceId!!)
                                ?: throw Exception("Bank account $referenceId not found")
                            accountDao.updateBalance(referenceId, bankAccount.balance - amount)
                        }
                        "Debit Card" -> {
                            val card = debitCardDao.getAllDebitCards().find { it.cardName == referenceId }
                                ?: throw Exception("Debit Card $referenceId not found")
                            val linkedAccountName = card.linkedBankAccountId ?: ""
                            val bankAccount = accountDao.getAccountByName(linkedAccountName)
                                ?: throw Exception("Linked Bank account $linkedAccountName not found")
                            accountDao.updateBalance(linkedAccountName, bankAccount.balance - amount)
                        }
                        "Credit Card" -> {
                            val card = creditCardDao.getAllCreditCards().find { it.cardName == referenceId }
                                ?: throw Exception("Credit Card $referenceId not found")
                            val newAvailableLimit = (card.availableLimit ?: 0.0) - amount
                            if (newAvailableLimit < 0) throw Exception("Credit limit exceeded")
                            
                            val updatedCard = card.copy(availableLimit = newAvailableLimit)
                            creditCardDao.updateCreditCard(updatedCard)
                        }
                    }

                    // Insert Transaction record
                    transactionDao.insertTransaction(transaction)
                    
                    // Deduct from budget (Directly in Transaction)
                    val budget = budgetDao.getBudget(monthYear)
                    if (budget != null) {
                        budgetDao.updateRemainingBudget(monthYear, budget.remainingBudget - amount)
                    }
                }

                // 2. Sync to Firebase (Non-blocking)
                syncToFirebase(transaction, username)

                callback(true, null)
            } catch (e: Exception) {
                callback(false, e.message)
            }
        }.start()
    }

    private fun syncToFirebase(transaction: Transaction, username: String) {
        val firebaseId = transaction.firebaseId ?: return
        val category = transaction.category
        
        val firebaseExpenseData = mapOf(
            "timestamp" to transaction.timestamp,
            "amount" to transaction.amount,
            "account" to (transaction.referenceId ?: transaction.accountName),
            "paymentMethod" to transaction.paymentMethod
        )

        FirebaseDatabase.getInstance()
            .getReference("users/$username/expenses/$category/$firebaseId")
            .setValue(firebaseExpenseData)
    }
}
