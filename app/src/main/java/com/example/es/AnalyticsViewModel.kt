package com.example.es

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.Calendar

class AnalyticsViewModel(private val repository: TransactionRepository) : ViewModel() {

    private val _topSpending = MutableLiveData<List<Transaction>>()
    val topSpending: LiveData<List<Transaction>> = _topSpending

    private val _filteredTransactions = MutableLiveData<List<Transaction>>()
    val filteredTransactions: LiveData<List<Transaction>> = _filteredTransactions

    private val _heatmapData = MutableLiveData<List<Float>>()
    val heatmapData: LiveData<List<Float>> = _heatmapData

    fun fetchData(timeRange: String, selectedCalendar: Calendar) {
        viewModelScope.launch {
            val (startTime, endTime) = getTimeRange(timeRange, selectedCalendar)
            
            val top = repository.getTopSpending(startTime, endTime)
            _topSpending.postValue(top)

            val transactions = repository.getTransactionsInRange(startTime, endTime)
            _filteredTransactions.postValue(transactions)

            // Generate heatmap data for the current month
            val monthStart = selectedCalendar.clone() as Calendar
            monthStart.set(Calendar.DAY_OF_MONTH, 1)
            monthStart.set(Calendar.HOUR_OF_DAY, 0)
            monthStart.set(Calendar.MINUTE, 0)
            monthStart.set(Calendar.SECOND, 0)
            
            val monthEnd = selectedCalendar.clone() as Calendar
            monthEnd.set(Calendar.DAY_OF_MONTH, monthEnd.getActualMaximum(Calendar.DAY_OF_MONTH))
            monthEnd.set(Calendar.HOUR_OF_DAY, 23)
            
            val monthTransactions = repository.getTransactionsInRange(monthStart.timeInMillis, monthEnd.timeInMillis)
            _heatmapData.postValue(calculateHeatmapIntensity(monthTransactions, monthStart))
        }
    }

    private fun getTimeRange(timeRange: String, cal: Calendar): Pair<Long, Long> {
        val start = cal.clone() as Calendar
        val end = cal.clone() as Calendar

        when (timeRange.lowercase()) {
            "day" -> {
                start.set(Calendar.HOUR_OF_DAY, 0)
                end.set(Calendar.HOUR_OF_DAY, 23)
            }
            "week" -> {
                start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
                end.add(Calendar.DAY_OF_WEEK, 6)
            }
            "month" -> {
                start.set(Calendar.DAY_OF_MONTH, 1)
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            "year" -> {
                start.set(Calendar.DAY_OF_YEAR, 1)
                end.set(Calendar.DAY_OF_YEAR, end.getActualMaximum(Calendar.DAY_OF_YEAR))
            }
        }
        return Pair(start.timeInMillis, end.timeInMillis)
    }

    private fun calculateHeatmapIntensity(transactions: List<Transaction>, monthStart: Calendar): List<Float> {
        val daysInMonth = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dailyTotals = FloatArray(daysInMonth)
        var maxTotal = 0.1f

        transactions.forEach {
            val transCal = Calendar.getInstance()
            transCal.timeInMillis = it.timestamp
            val day = transCal.get(Calendar.DAY_OF_MONTH) - 1
            if (day in 0 until daysInMonth) {
                dailyTotals[day] += it.amount.toFloat()
                if (dailyTotals[day] > maxTotal) maxTotal = dailyTotals[day]
            }
        }

        return dailyTotals.map { it / maxTotal }
    }
}
