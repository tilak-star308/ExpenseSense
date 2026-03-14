package com.example.es

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class WalletAccountsFragment : Fragment() {

    private lateinit var accountViewModel: AccountViewModel
    private lateinit var tvCashBalance: TextView
    private lateinit var etCashInput: EditText
    private lateinit var layoutCashDisplay: View
    private lateinit var layoutCashInput: View
    private lateinit var btnEditCash: View
    private lateinit var btnSaveCash: View
    private lateinit var rvBankAccounts: RecyclerView
    private lateinit var btnAddBank: MaterialButton

    private var otherBanks = mutableListOf<Account>()
    private lateinit var adapter: AccountAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wallet_accounts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvCashBalance = view.findViewById(R.id.tvCashBalance)
        etCashInput = view.findViewById(R.id.etCashInput)
        layoutCashDisplay = view.findViewById(R.id.tvCashBalance) // Updated ID mapping
        layoutCashInput = view.findViewById(R.id.layoutCashInput)
        btnEditCash = view.findViewById(R.id.btnEditCash)
        btnSaveCash = view.findViewById(R.id.btnSaveCash)
        rvBankAccounts = view.findViewById(R.id.rvBankAccounts)
        btnAddBank = view.findViewById(R.id.btnAddBank)

        rvBankAccounts.layoutManager = LinearLayoutManager(requireContext())
        adapter = AccountAdapter(otherBanks)
        rvBankAccounts.adapter = adapter

        setupViewModel()

        btnEditCash.setOnClickListener { showCashInput(true) }
        btnSaveCash.setOnClickListener { saveCashBalance() }
        btnAddBank.setOnClickListener { showAddBankDialog() }
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(requireContext())
        val repository = AccountRepository(database.accountDao())
        val factory = AccountViewModelFactory(repository)
        accountViewModel = ViewModelProvider(this, factory).get(AccountViewModel::class.java)

        accountViewModel.accounts.observe(viewLifecycleOwner) { accounts ->
            // Separate Cash from Banks
            val cashAcc = accounts.find { it.name.lowercase() == "cash" }
            tvCashBalance.text = "₹%.2f".format(cashAcc?.balance ?: 0.0)
            
            val banks = accounts.filter { it.name.lowercase() != "cash" }
            otherBanks.clear()
            otherBanks.addAll(banks)
            adapter.notifyDataSetChanged()
        }

        accountViewModel.loadAccounts()
    }

    private fun showCashInput(show: Boolean) {
        layoutCashDisplay.visibility = if (show) View.GONE else View.VISIBLE
        layoutCashInput.visibility = if (show) View.VISIBLE else View.GONE
        btnEditCash.visibility = if (show) View.GONE else View.VISIBLE
        if (show) {
            etCashInput.requestFocus()
        }
    }

    private fun saveCashBalance() {
        val amount = etCashInput.text.toString().toDoubleOrNull()
        if (amount != null) {
            // Find current cash balance to find the difference for updateBalance
            // Or just add a 'setBalance' to repository. Let's use updateBalance for consistency or just re-save the entity.
            // Actually, Repository has saveAccount which uses REPLACE.
            val cashAcc = Account("Cash", "Wallet", amount)
            accountViewModel.addAccount(cashAcc.name, cashAcc.type, cashAcc.balance)
            showCashInput(false)
            Toast.makeText(requireContext(), "Cash balance updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddBankDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_account, null)
        val etName = dialogView.findViewById<EditText>(R.id.etAccountName)
        val etType = dialogView.findViewById<EditText>(R.id.etAccountType)
        val etBalance = dialogView.findViewById<EditText>(R.id.etAccountBalance)

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val type = etType.text.toString().trim()
                val balance = etBalance.text.toString().toDoubleOrNull() ?: 0.0

                if (name.isNotEmpty()) {
                    accountViewModel.addAccount(name, type, balance)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
