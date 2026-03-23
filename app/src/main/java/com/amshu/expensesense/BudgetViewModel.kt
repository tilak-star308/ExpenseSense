package com.amshu.expensesense

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.*

class BudgetViewModel(private val repository: BudgetRepository) : ViewModel() {

    private val _budget = MutableLiveData<Budget?>()
    val budget: LiveData<Budget?> = _budget

    fun loadCurrentMonthBudget() {
        val currentMonthYear = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
        Thread {
            val currentBudget = repository.getBudget(currentMonthYear)
            _budget.postValue(currentBudget)
        }.start()
    }

    fun setMonthlyBudget(amount: Double) {
        val currentMonthYear = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
        repository.setBudget(amount, currentMonthYear)
        
        // Refresh after a short delay to allow calculation to finish
        Thread {
            Thread.sleep(500)
            loadCurrentMonthBudget()
        }.start()
    }
}
