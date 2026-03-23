package com.amshu.expensesense

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(
    private val records: List<Transaction>,
    private val onDeleteClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle:    TextView = itemView.findViewById(R.id.tvTransactionTitle)
        val tvCategory: TextView = itemView.findViewById(R.id.tvTransactionCategory)
        val tvAmount:   TextView = itemView.findViewById(R.id.tvTransactionAmount)
        val tvDate:     TextView = itemView.findViewById(R.id.tvTransactionDate)
        val btnDelete:  Button   = itemView.findViewById(R.id.btnDeleteTransaction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val record = records[position]
        holder.tvTitle.text    = record.title
        holder.tvCategory.text = record.category
        holder.tvAmount.text   = "₹%.2f".format(record.amount)
        holder.tvDate.text     = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            .format(Date(record.timestamp))
        holder.btnDelete.setOnClickListener { onDeleteClick(record) }
    }

    override fun getItemCount(): Int = records.size
}
