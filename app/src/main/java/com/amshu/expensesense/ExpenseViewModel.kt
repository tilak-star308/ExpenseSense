package com.amshu.expensesense

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ExpenseViewModel : ViewModel() {

    private val _expenseList = MutableLiveData<MutableList<Expense>>()
    val expenseList: LiveData<MutableList<Expense>> = _expenseList

    private val _totalBalance = MutableLiveData<Double>()
    val totalBalance: LiveData<Double> = _totalBalance

    fun updateExpenses(list: MutableList<Expense>) {
        _expenseList.postValue(list)
        calculateBalance(list)
    }

    fun removeExpense(expense: Expense) {
        val currentList = _expenseList.value ?: mutableListOf()
        if (currentList.remove(expense)) {
            _expenseList.postValue(currentList)
            calculateBalance(currentList)
        }
    }

    private fun calculateBalance(list: List<Expense>) {
        var total = 0.0
        for (expense in list) {
            total += expense.amount
        }
        _totalBalance.postValue(total)
    }
}
