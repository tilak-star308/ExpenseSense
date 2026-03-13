package com.example.es

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HomeFragment : Fragment() {

    private val expenseList = mutableListOf<Expense>()
    private lateinit var adapter: ExpenseAdapter
    private lateinit var rvExpenses: RecyclerView
    private lateinit var tvEmpty: TextView

    // ActivityResultLauncher to get result from AddExpenseActivity
    private val addExpenseLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val expense = result.data?.getParcelableExtra<Expense>(AddExpenseActivity.EXTRA_EXPENSE)
            expense?.let {
                expenseList.add(0, it)   // newest first
                adapter.notifyItemInserted(0)
                rvExpenses.scrollToPosition(0)
                updateEmptyState()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvExpenses = view.findViewById(R.id.rvExpenses)
        tvEmpty    = view.findViewById(R.id.tvEmpty)

        adapter = ExpenseAdapter(expenseList)
        rvExpenses.layoutManager = LinearLayoutManager(requireContext())
        rvExpenses.adapter = adapter

        updateEmptyState()

        // Expose the launcher so MainActivity can trigger it via the FAB
        (activity as? MainActivity)?.setAddExpenseLauncher { openAddExpense() }
    }

    fun openAddExpense() {
        val intent = Intent(requireContext(), AddExpenseActivity::class.java)
        addExpenseLauncher.launch(intent)
    }

    private fun updateEmptyState() {
        if (expenseList.isEmpty()) {
            tvEmpty.visibility    = View.VISIBLE
            rvExpenses.visibility = View.GONE
        } else {
            tvEmpty.visibility    = View.GONE
            rvExpenses.visibility = View.VISIBLE
        }
    }
}
