package com.example.opensourcecodingbudgetmadness

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddExpensesActivity : AppCompatActivity() {

    private lateinit var expenseNameInput: EditText
    private lateinit var expenseAmountInput: EditText
    private lateinit var paymentMethodInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var selectDateButton: Button
    private lateinit var uploadReceiptButton: Button
    private lateinit var addExpenseButton: Button

    private var selectedDate: String = ""
    private var imageUri: Uri? = null

    private val TAG = "AddExpensesActivity"
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_PICK = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_expenses)

        Log.d(TAG, "AddExpensesActivity started")
        logToFile("AddExpensesActivity started")

        val dbHelper = BudgetDatabaseHelper(this)

        // LINK VIEWS
        expenseNameInput = findViewById(R.id.expenseNameInput)
        expenseAmountInput = findViewById(R.id.expenseAmountInput)
        paymentMethodInput = findViewById(R.id.paymentMethodInput)
        categorySpinner = findViewById(R.id.categorySpinner)
        selectDateButton = findViewById(R.id.selectDateButton)
        uploadReceiptButton = findViewById(R.id.uploadReceiptButton)
        addExpenseButton = findViewById(R.id.addExpenseButton)

        // SPINNER SETUP
        val categories = dbHelper.getAllCategories().ifEmpty { listOf("Uncategorized") }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        // DATE PICKER
        selectDateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    selectDateButton.text = selectedDate
                    logToFile("Date selected: $selectedDate")
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        // UPLOAD RECEIPT
        uploadReceiptButton.setOnClickListener {
            val options = arrayOf("Camera", "Gallery")
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Choose Image Source")
            builder.setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            builder.show()
        }

        // ADD EXPENSE BUTTON
        addExpenseButton.setOnClickListener {
            val name = expenseNameInput.text.toString().trim()
            val amount = expenseAmountInput.text.toString().toDoubleOrNull() ?: 0.0
            val paymentMethod = paymentMethodInput.text.toString().trim()
            val category = categorySpinner.selectedItem?.toString() ?: "Uncategorized"
            val date = selectedDate

            val logMsg = "Attempting to add expense - Name: $name, Amount: $amount, Method: $paymentMethod, Category: $category, Date: $date"
            Log.d(TAG, logMsg)
            logToFile(logMsg)

            if (name.isNotEmpty() && amount > 0 && paymentMethod.isNotEmpty() && date.isNotEmpty()) {
                dbHelper.insertExpense(name, amount, paymentMethod, category, date)
                Toast.makeText(this, "Expense added!", Toast.LENGTH_SHORT).show()
                logToFile("Expense added successfully")
                clearInputs()
            } else {
                Toast.makeText(this, "Please fill in all fields correctly", Toast.LENGTH_SHORT).show()
                logToFile("Failed to add expense - missing or invalid input")
            }
        }

        // Bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_income -> {
                    startActivity(Intent(this, IncomeActivity::class.java))
                    true
                }
                R.id.nav_home -> {
                    startActivity(Intent(this, StarterPageActivity::class.java))
                    true
                }
                R.id.nav_add -> {
                    startActivity(Intent(this, AddExpensesActivity::class.java))
                    true
                }
                R.id.nav_open_menu -> {
                    startActivity(Intent(this, MenuActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun openCamera() {
        val imageFile = File.createTempFile("receipt_", ".jpg", externalCacheDir)
        imageUri = FileProvider.getUriForFile(this, "${packageName}.provider", imageFile)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    Toast.makeText(this, "Receipt captured", Toast.LENGTH_SHORT).show()
                    logToFile("Image captured: $imageUri")
                }
                REQUEST_IMAGE_PICK -> {
                    imageUri = data?.data
                    Toast.makeText(this, "Receipt selected", Toast.LENGTH_SHORT).show()
                    logToFile("Image selected: $imageUri")
                }
            }
        }
    }

    private fun clearInputs() {
        expenseNameInput.text.clear()
        expenseAmountInput.text.clear()
        paymentMethodInput.text.clear()
        categorySpinner.setSelection(0)
        selectDateButton.text = "Select Date"
        selectedDate = ""
        imageUri = null
        logToFile("Inputs cleared")
    }

    private fun logToFile(message: String) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val logFile = File(filesDir, "app_log.txt")
            logFile.appendText("[$timestamp] $message\n")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to log file", e)
        }
    }
}
