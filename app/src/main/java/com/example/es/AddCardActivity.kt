package com.example.es

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class AddCardActivity : AppCompatActivity() {

    private lateinit var etCardHolderName: EditText
    private lateinit var etCardNumber: EditText
    private lateinit var etCardName: EditText
    private lateinit var spinnerCardType: Spinner
    private lateinit var layoutCreditFields: View
    private lateinit var etCreditLimit: EditText
    private lateinit var etAvailableLimit: EditText
    private lateinit var layoutDebitFields: View
    private lateinit var cbAddBankAccount: CheckBox
    private lateinit var layoutNewAccount: View
    private lateinit var etAccountBalance: EditText
    private lateinit var spinnerAccountType: Spinner
    private lateinit var btnSaveCard: MaterialButton
    private lateinit var btnBack: View

    private lateinit var cardRepository: CardRepository
    private lateinit var accountRepository: AccountRepository
    private var editingCardId: Int = 0

    companion object {
        const val EXTRA_CARD_ID = "extra_card_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_card)

        val database = AppDatabase.getDatabase(this)
        cardRepository = CardRepository(
            database.debitCardDao(),
            database.creditCardDao(),
            database.accountDao(),
            database.cardDao()
        )
        accountRepository = AccountRepository(database.accountDao())

        btnBack = findViewById(R.id.btnBack)
        etCardHolderName = findViewById(R.id.etCardHolderName)
        etCardNumber = findViewById(R.id.etCardNumber)
        etCardName = findViewById(R.id.etCardName)
        spinnerCardType = findViewById(R.id.spinnerCardType)
        layoutCreditFields = findViewById(R.id.layoutCreditFields)
        etCreditLimit = findViewById(R.id.etCreditLimit)
        etAvailableLimit = findViewById(R.id.etAvailableLimit)
        layoutDebitFields = findViewById(R.id.layoutDebitFields)
        cbAddBankAccount = findViewById(R.id.cbAddBankAccount)
        layoutNewAccount = findViewById(R.id.layoutNewAccount)
        etAccountBalance = findViewById(R.id.etAccountBalance)
        spinnerAccountType = findViewById(R.id.spinnerAccountType)
        btnSaveCard = findViewById(R.id.btnSaveCard)

        btnBack.setOnClickListener { finish() }

        spinnerCardType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = parent?.getItemAtPosition(position).toString()
                if (selected == "Credit Card") {
                    layoutCreditFields.visibility = View.VISIBLE
                    layoutDebitFields.visibility = View.GONE
                } else {
                    layoutCreditFields.visibility = View.GONE
                    layoutDebitFields.visibility = View.VISIBLE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        cbAddBankAccount.setOnCheckedChangeListener { _, isChecked ->
            layoutNewAccount.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        btnSaveCard.setOnClickListener { saveCard() }

        // Check for Edit Mode
        editingCardId = intent.getIntExtra(EXTRA_CARD_ID, 0)
        val cardType = intent.getStringExtra("extra_card_type")
        if (editingCardId != 0) {
            loadCardData(editingCardId, cardType)
            btnSaveCard.text = "Update Card"
            findViewById<TextView>(R.id.headerTitle)?.text = "Edit Card"
        } else if (intent.getBooleanExtra("isScan", false)) {
            prefillScannedData()
        }
    }

    private fun prefillScannedData() {
        val holder = intent.getStringExtra("cardHolder")
        val number = intent.getStringExtra("cardNumber")
        val name = intent.getStringExtra("cardName")
        val type = intent.getStringExtra("cardType")
        val bankName = intent.getStringExtra("bankName") ?: ""

        etCardHolderName.setText(holder)
        etCardNumber.setText(number)
        etCardName.setText(name)

        // Set Card Type Spinner
        val typeAdapter = spinnerCardType.adapter
        if (typeAdapter != null && type != null) {
            for (i in 0 until typeAdapter.count) {
                if (typeAdapter.getItem(i).toString().contains(type, ignoreCase = true)) {
                    spinnerCardType.setSelection(i)
                    break
                }
            }
        }

        // Logic for Linked Bank Account
        if (bankName.isNotEmpty()) {
            cardRepository.checkAccountExists(bankName) { exists ->
                runOnUiThread {
                    cbAddBankAccount.isChecked = !exists
                }
            }
        }
    }

    private fun loadCardData(id: Int, type: String?) {
        if (type == "Credit") {
            cardRepository.getCreditCardById(id) { card ->
                if (card != null) {
                    runOnUiThread {
                        etCardHolderName.setText(card.cardHolderName ?: "")
                        etCardNumber.setText(card.cardNumber ?: "")
                        etCardName.setText(card.cardName ?: "")
                        spinnerCardType.setSelection(1) // Credit Card
                        etCreditLimit.setText(card.totalLimit?.toString() ?: "0.0")
                        etAvailableLimit.setText(card.availableLimit?.toString() ?: "0.0")
                    }
                }
            }
        } else {
            cardRepository.getDebitCardById(id) { card ->
                if (card != null) {
                    runOnUiThread {
                        etCardHolderName.setText(card.cardHolderName ?: "")
                        etCardNumber.setText(card.cardNumber ?: "")
                        etCardName.setText(card.cardName ?: "")
                        spinnerCardType.setSelection(0) // Debit Card
                        cbAddBankAccount.visibility = View.GONE
                        layoutNewAccount.visibility = View.VISIBLE
                        accountRepository.getAccountByName(card.bankName ?: "") { account ->
                            if (account != null) {
                                runOnUiThread {
                                    etAccountBalance.setText(account.balance.toString())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun saveCard() {
        val holder = etCardHolderName.text.toString().trim()
        val number = etCardNumber.text.toString().trim()
        val rawName = etCardName.text.toString().trim()
        val name = rawName.replace(Regex("[.#$\\[\\]/]"), "_") // Sanitize for Firebase Key
        val type = spinnerCardType.selectedItem.toString()

        if (holder.isEmpty() || number.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val last4 = if (number.length >= 4) number.takeLast(4) else number

        if (type == "Credit Card") {
            val limit = etCreditLimit.text.toString().toDoubleOrNull() ?: 0.0
            val available = etAvailableLimit.text.toString().toDoubleOrNull() ?: 0.0
            val card = CreditCard(
                id = editingCardId,
                cardHolderName = holder,
                cardNumber = number,
                cardName = name,
                bankName = rawName.split(" ").first(),
                last4Digits = last4,
                totalLimit = limit,
                availableLimit = available
            )
            cardRepository.saveCreditCard(card) { success ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, if (editingCardId == 0) "Credit Card Saved" else "Credit Card Updated", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Error saving card", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            val bankName = rawName.split(" ").firstOrNull()?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: rawName
            
            val saveAction = {
                val card = DebitCard(
                    id = editingCardId,
                    cardHolderName = holder,
                    cardNumber = number,
                    cardName = name,
                    bankName = bankName,
                    last4Digits = last4,
                    linkedBankAccountId = bankName
                )
                cardRepository.saveDebitCard(card) { success ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, if (editingCardId == 0) "Debit Card Saved" else "Debit Card Updated", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this, "Error saving card", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            if (cbAddBankAccount.isChecked) {
                val balance = etAccountBalance.text.toString().toDoubleOrNull() ?: 0.0
                val accType = spinnerAccountType.selectedItem.toString()
                val account = Account(name = bankName, type = accType, balance = balance)
                accountRepository.saveAccount(account) {
                    saveAction()
                }
            } else {
                saveAction()
            }
        }
    }
}
