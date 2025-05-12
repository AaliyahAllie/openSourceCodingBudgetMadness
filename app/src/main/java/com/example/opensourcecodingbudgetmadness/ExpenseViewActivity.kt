package com.example.opensourcecodingbudgetmadness

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*

class ExpenseViewActivity : AppCompatActivity() {

    private lateinit var dbHelper: BudgetDatabaseHelper
    private lateinit var textReceiptView: TextView
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_view)

        dbHelper = BudgetDatabaseHelper(this)
        val editStartDate = findViewById<EditText>(R.id.editStartDate)
        val editEndDate = findViewById<EditText>(R.id.editEndDate)
        val btnLoad = findViewById<Button>(R.id.btnLoadExpenses)
        textReceiptView = findViewById(R.id.textReceiptView)

        editStartDate.setOnClickListener { showDatePicker(editStartDate) }
        editEndDate.setOnClickListener { showDatePicker(editEndDate) }

        btnLoad.setOnClickListener {
            val startDate = editStartDate.text.toString()
            val endDate = editEndDate.text.toString()

            if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
                val expenses = dbHelper.getExpensesBetweenDates(startDate, endDate)
                textReceiptView.text = if (expenses.isNotEmpty()) {
                    buildReceipt(expenses)
                } else {
                    "No expenses found in this date range."
                }
            } else {
                Toast.makeText(this, "Please select both start and end dates", Toast.LENGTH_SHORT).show()
            }
        }

        setupBottomNav()
    }

    private fun buildReceipt(expenses: List<BudgetDatabaseHelper.ExpenseEntry>): String {
        val builder = StringBuilder()
        builder.append("========== EXPENSE RECEIPT ==========\n")
        builder.append("From ${expenses.first().date} to ${expenses.last().date}\n")
        builder.append("=====================================\n\n")

        var total = 0.0
        for (e in expenses) {
            builder.append("Name   : ${e.name}\n")
            builder.append("Amount : R${"%.2f".format(e.amount)}\n")
            builder.append("Date   : ${e.date}\n")
            builder.append("-------------------------------------\n")
            total += e.amount
        }

        builder.append("\nTOTAL SPENT: R${"%.2f".format(total)}\n")
        builder.append("=====================================\n")

        return builder.toString()
    }

    private fun showDatePicker(targetEditText: EditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            targetEditText.setText(dateFormat.format(calendar.time))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_income -> startActivity(Intent(this, IncomeActivity::class.java))
                R.id.nav_home -> startActivity(Intent(this, StarterPageActivity::class.java))
                R.id.nav_add -> startActivity(Intent(this, AddExpensesActivity::class.java))
                R.id.nav_open_menu -> startActivity(Intent(this, MenuActivity::class.java))
                else -> false
            }
            true
        }
    }
}
