package com.amshu.expensesense

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class TopSpendingAdapter(private var transactions: List<Transaction>) :
    RecyclerView.Adapter<TopSpendingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgCategory: ImageView = view.findViewById(R.id.imgCategoryIcon)
        val tvMerchant: TextView = view.findViewById(R.id.tvMerchant)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_spending, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.tvMerchant.text = transaction.title
        
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        holder.tvDate.text = dateFormat.format(transaction.timestamp)
        
        holder.tvAmount.text = "- ₹${String.format("%.2f", transaction.amount)}"
        holder.tvAmount.setTextColor(Color.parseColor("#FF4444"))

        // Set category icon based on category name
        val iconRes = when (transaction.category.lowercase()) {
            "food" -> R.drawable.ic_analytics // Replace with actual food icon if available
            "travel" -> R.drawable.ic_wallet
            "shopping" -> R.drawable.ic_profile
            else -> R.drawable.ic_analytics
        }
        holder.imgCategory.setImageResource(iconRes)
    }

    override fun getItemCount() = transactions.size

    fun updateData(newList: List<Transaction>) {
        transactions = newList
        notifyDataSetChanged()
    }
}
