package com.amshu.expensesense

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var etName: EditText
    private lateinit var etAmount: EditText
    private lateinit var tvDate: TextView
    private lateinit var tvClear: TextView
    private lateinit var layoutDate: RelativeLayout
    private lateinit var ivCategoryIcon: ImageView
    private lateinit var pbCategorySelect: ProgressBar
    private var selectedCategory: String = "other"
    private lateinit var chipGroupPaymentMethod: com.google.android.material.chip.ChipGroup
    private lateinit var layoutDynamicSelector: LinearLayout
    private lateinit var tvSelectorLabel: TextView
    private lateinit var spinnerDynamic: Spinner
    private lateinit var btnAddExpense: MaterialButton

    private lateinit var paymentRepository: PaymentRepository
    private lateinit var cardRepository: CardRepository

    private val calendar = Calendar.getInstance()
    private lateinit var budgetRepository: BudgetRepository
    private lateinit var accountRepository: AccountRepository

    private val categorySelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val category = result.data?.getStringExtra("selected_category")
            if (!category.isNullOrEmpty()) {
                selectedCategory = category
                updateCategoryImage(selectedCategory)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        val database = AppDatabase.getDatabase(this)
        budgetRepository = BudgetRepository(database.budgetDao(), database.transactionDao())
        accountRepository = AccountRepository(database.accountDao())
        cardRepository = CardRepository(
            database.debitCardDao(),
            database.creditCardDao(),
            database.accountDao(),
            database.cardDao()
        )
        paymentRepository = PaymentRepository(
            database,
            database.transactionDao(),
            database.accountDao(),
            database.debitCardDao(),
            database.creditCardDao(),
            database.budgetDao()
        )

        btnBack       = findViewById(R.id.btnBack)
        etName        = findViewById(R.id.etName)
        etAmount      = findViewById(R.id.etAmount)
        tvDate        = findViewById(R.id.tvDate)
        tvClear       = findViewById(R.id.tvClear)
        layoutDate    = findViewById(R.id.layoutDate)
        ivCategoryIcon = findViewById(R.id.ivCategoryIcon)
        pbCategorySelect = findViewById(R.id.pbCategorySelect)
        
        // Payment Method Views
        chipGroupPaymentMethod = findViewById(R.id.chipGroupPaymentMethod)
        layoutDynamicSelector  = findViewById(R.id.layoutDynamicSelector)
        tvSelectorLabel        = findViewById(R.id.tvSelectorLabel)
        spinnerDynamic         = findViewById(R.id.spinnerDynamic)
        
        btnAddExpense = findViewById(R.id.btnAddExpense)

        // Initialize category UI
        updateCategoryImage(selectedCategory)
        
        ivCategoryIcon.setOnClickListener {
            val intent = Intent(this, CategorySelectionActivity::class.java)
            categorySelectionLauncher.launch(intent)
        }

        // Setup Payment Method Selection
        chipGroupPaymentMethod.setOnCheckedChangeListener { group, checkedId ->
            handlePaymentMethodChange(checkedId)
        }
        
        // Initialize default state
        handlePaymentMethodChange(R.id.chipCash)

        // Pre-fill today's date
        updateDateLabel()

        btnBack.setOnClickListener { finish() }

        tvClear.setOnClickListener {
            etAmount.setText("")
            etAmount.requestFocus()
        }

        layoutDate.setOnClickListener { showDatePicker() }
        tvDate.setOnClickListener    { showDatePicker() }

        btnAddExpense.setOnClickListener { submitExpense() }

        // Setup Keyword-based Auto-categorization
        etName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val expenseName = etName.text.toString().trim()
                if (expenseName.isNotEmpty()) {
                    pbCategorySelect.visibility = View.VISIBLE
                    ivCategoryIcon.visibility = View.INVISIBLE
                    
                    val detectedCategory = CategoryHelper.detectCategory(expenseName)
                    
                    // Small delay for visual feedback
                    etName.postDelayed({
                        selectedCategory = detectedCategory
                        updateCategoryImage(selectedCategory)
                        pbCategorySelect.visibility = View.GONE
                        ivCategoryIcon.visibility = View.VISIBLE
                    }, 400)
                } else {
                    selectedCategory = "other"
                    updateCategoryImage(selectedCategory)
                }
            }
        }

        // Handle Intent Extras from Bill Scanning
        intent.apply {
            val title = getStringExtra("title")
            val amount = getDoubleExtra("amount", -1.0)
            val category = getStringExtra("category")
            val timestamp = getLongExtra("timestamp", -1L)

            if (!title.isNullOrEmpty()) {
                etName.setText(title)
            }
            if (amount != -1.0) {
                etAmount.setText(amount.toString())
            }
            if (!category.isNullOrEmpty()) {
                selectedCategory = category
                updateCategoryImage(selectedCategory)
            }
            if (timestamp != -1L) {
                calendar.timeInMillis = timestamp
                updateDateLabel()
            }
        }
    }

    private fun updateCategoryImage(category: String) {
        val iconResId = CategoryAdapter.getCategoryIcon(category)
        ivCategoryIcon.setImageResource(iconResId)
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                updateDateLabel()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateLabel() {
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        tvDate.text = sdf.format(calendar.time)
    }

    private fun handlePaymentMethodChange(chipId: Int) {
        when (chipId) {
            R.id.chipCash -> {
                layoutDynamicSelector.visibility = View.GONE
            }
            R.id.chipUPI -> {
                layoutDynamicSelector.visibility = View.VISIBLE
                tvSelectorLabel.text = "SELECT BANK"
                loadAccounts()
            }
            R.id.chipDebit -> {
                layoutDynamicSelector.visibility = View.VISIBLE
                tvSelectorLabel.text = "SELECT DEBIT CARD"
                loadCards("Debit")
            }
            R.id.chipCredit -> {
                layoutDynamicSelector.visibility = View.VISIBLE
                tvSelectorLabel.text = "SELECT CREDIT CARD"
                loadCards("Credit")
            }
        }
    }

    private fun loadAccounts() {
        accountRepository.getAllAccounts { accounts ->
            val filtered = accounts.filter { it.name != "Cash" }
            runOnUiThread {
                val names = filtered.map { it.name }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerDynamic.adapter = adapter
            }
        }
    }

    private fun loadCards(type: String) {
        cardRepository.getAllCards { debits, credits ->
            val names = if (type == "Debit") {
                debits.map { it.cardName }
            } else {
                credits.map { it.cardName }
            }
            runOnUiThread {
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerDynamic.adapter = adapter
            }
        }
    }

    private fun submitExpense() {
        val name   = etName.text.toString().trim()
        val amtStr = etAmount.text.toString().trim()

        if (name.isEmpty()) {
            etName.error = "Enter expense name"
            etName.requestFocus()
            return
        }
        if (amtStr.isEmpty()) {
            etAmount.error = "Enter amount"
            etAmount.requestFocus()
            return
        }
        val amount = amtStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            etAmount.error = "Enter a valid amount"
            etAmount.requestFocus()
            return
        }

        val paymentMethod = when (chipGroupPaymentMethod.checkedChipId) {
            R.id.chipCash -> "Cash"
            R.id.chipUPI -> "UPI"
            R.id.chipDebit -> "Debit Card"
            R.id.chipCredit -> "Credit Card"
            else -> "Cash"
        }

        val referenceId = spinnerDynamic.selectedItem?.toString()
        if (paymentMethod != "Cash" && referenceId == null) {
            Toast.makeText(this, "Please select a ${tvSelectorLabel.text}", Toast.LENGTH_SHORT).show()
            return
        }

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || user.email == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val username  = user.email!!.substringBefore("@")
        val category  = selectedCategory
        val timestamp = calendar.timeInMillis
        val sanctionedName = name.replace(Regex("[.#$\\[\\]/]"), "-")
        val uniqueTitleKey = "$sanctionedName-$timestamp"

        val transaction = Transaction(
            title      = name,
            amount     = amount,
            category   = category,
            accountName = referenceId ?: "Cash",
            timestamp  = timestamp,
            paymentMethod = paymentMethod,
            referenceId = referenceId,
            firebaseId = uniqueTitleKey
        )

        btnAddExpense.isEnabled = false
        
        paymentRepository.saveExpense(transaction, paymentMethod, referenceId, username) { success, error ->
            runOnUiThread {
                btnAddExpense.isEnabled = true
                if (success) {
                    Toast.makeText(this@AddExpenseActivity, "Expense Saved", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@AddExpenseActivity, "Error: $error", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

