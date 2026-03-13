package com.example.es

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class HomeFragment : Fragment() {

    private lateinit var rvTransactions: RecyclerView
    private lateinit var tvTotal: TextView
    private lateinit var tvMonthTotal: TextView
    private lateinit var tvEmpty: TextView

    private var transactionList = mutableListOf<Transaction>()
    private lateinit var adapter: TransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvTransactions = view.findViewById(R.id.rvTransactions)
        tvTotal        = view.findViewById(R.id.tvTotal)
        tvMonthTotal   = view.findViewById(R.id.tvMonthTotal)
        tvEmpty        = view.findViewById(R.id.tvEmpty)

        rvTransactions.layoutManager = LinearLayoutManager(requireContext())

        // Expose the launcher so MainActivity can trigger it via the FAB
        (activity as? MainActivity)?.setAddExpenseLauncher {
            startActivity(Intent(requireContext(), AddExpenseActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadFromRoom()      // offline-first: instant display
        syncWithFirebase()  // background sync, non-blocking
    }

    // ── Room load ─────────────────────────────────────────────────────────────

    private fun loadFromRoom() {
        Thread {
            val records = AppDatabase.getDatabase(requireContext())
                .transactionDao()
                .getAllTransactions()

            activity?.runOnUiThread {
                transactionList.clear()
                transactionList.addAll(records)
                updateSummaryViews(records)
                updateEmptyState()

                if (!::adapter.isInitialized) {
                    adapter = TransactionAdapter(transactionList) { deleteTransaction(it) }
                    rvTransactions.adapter = adapter
                } else {
                    adapter.notifyDataSetChanged()
                }
            }
        }.start()
    }

    // ── Firebase sync ─────────────────────────────────────────────────────────

    private fun syncWithFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance()
            .getReference("users/$userId/transactions")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isAdded || context == null) return
                    val safeContext = requireContext()
                    
                    Thread {
                        val dao = AppDatabase.getDatabase(safeContext).transactionDao()
                        val localMap = dao.getAllTransactions().associateBy { it.firebaseId }
                        var hasNew = false

                        for (child in snapshot.children) {
                            val fid = child.key
                            if (fid != null && !localMap.containsKey(fid)) {
                                val record = Transaction(
                                    title      = child.child("title").getValue(String::class.java) ?: "",
                                    amount     = child.child("amount").getValue(Double::class.java) ?: 0.0,
                                    category   = child.child("category").getValue(String::class.java) ?: "Other",
                                    timestamp  = child.child("timestamp").getValue(Long::class.java) ?: 0L,
                                    firebaseId = fid
                                )
                                dao.insertTransaction(record)
                                hasNew = true
                            }
                        }
                        if (hasNew && isAdded) loadFromRoom()
                    }.start()
                }

                override fun onCancelled(error: DatabaseError) { /* silent fail */ }
            })
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    private fun deleteTransaction(record: Transaction) {
        // Remove from Firebase
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null && record.firebaseId != null) {
            FirebaseDatabase.getInstance()
                .getReference("users/$userId/transactions/${record.firebaseId}")
                .removeValue()
        }
        // Remove from Room then refresh
        Thread {
            AppDatabase.getDatabase(requireContext())
                .transactionDao()
                .deleteTransaction(record)
            activity?.runOnUiThread { loadFromRoom() }
        }.start()
    }

    // ── Summary helpers ───────────────────────────────────────────────────────

    private fun updateSummaryViews(records: List<Transaction>) {
        val total = records.sumOf { it.amount }

        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear  = cal.get(Calendar.YEAR)
        val monthTotal = records.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
        }.sumOf { it.amount }

        tvTotal.text      = "Total: ₹%.2f".format(total)
        tvMonthTotal.text = "This Month: ₹%.2f".format(monthTotal)
    }

    private fun updateEmptyState() {
        if (transactionList.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvTransactions.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvTransactions.visibility = View.VISIBLE
        }
    }
}
