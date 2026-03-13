package com.example.es

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnAddExpense: MaterialButton

    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        btnBack       = findViewById(R.id.btnBack)
        etName        = findViewById(R.id.etName)
        etAmount      = findViewById(R.id.etAmount)
        tvDate        = findViewById(R.id.tvDate)
        tvClear       = findViewById(R.id.tvClear)
        layoutDate    = findViewById(R.id.layoutDate)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        btnAddExpense = findViewById(R.id.btnAddExpense)

        // Populate category spinner
        val categories = resources.getStringArray(R.array.transaction_categories)
        spinnerCategory.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

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

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val category  = spinnerCategory.selectedItem.toString()
        val timestamp = calendar.timeInMillis   // use selected date millis

        val databaseRef = FirebaseDatabase.getInstance()
            .getReference("users/$userId/transactions")
        val newRef     = databaseRef.push()
        val firebaseId = newRef.key

        val transaction = Transaction(
            title      = name,
            amount     = amount,
            category   = category,
            timestamp  = timestamp,
            firebaseId = firebaseId
        )

        // Save to Firebase with listeners to catch errors
        newRef.setValue(transaction)
            .addOnSuccessListener {
                // Save to Room on background thread, then close activity
                Thread {
                    AppDatabase.getDatabase(this@AddExpenseActivity)
                        .transactionDao()
                        .insertTransaction(transaction)
                    runOnUiThread {
                        Toast.makeText(this@AddExpenseActivity, "Expense Saved", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }.start()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Firebase Error: ${exception.message}", Toast.LENGTH_LONG).show()
                // Still save to Room so data isn't lost
                Thread {
                    AppDatabase.getDatabase(this@AddExpenseActivity)
                        .transactionDao()
                        .insertTransaction(transaction)
                    runOnUiThread {
                        finish()
                    }
                }.start()
            }
    }
}
