package com.example.es

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ExpenseAdapter(private val expenses: List<Expense>) :
    RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName:   TextView = itemView.findViewById(R.id.tvExpenseName)
        val tvAmount: TextView = itemView.findViewById(R.id.tvExpenseAmount)
        val tvDate:   TextView = itemView.findViewById(R.id.tvExpenseDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        holder.tvName.text   = expense.name
        holder.tvAmount.text = "₹%.2f".format(expense.amount)
        holder.tvDate.text   = expense.date
    }

    override fun getItemCount(): Int = expenses.size
}
