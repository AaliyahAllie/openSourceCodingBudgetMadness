package com.example.opensourcecodingbudgetmadness

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BudgetDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "budget.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_CATEGORIES = "Categories"
        const val COLUMN_ID = "id"
        const val COLUMN_CATEGORY_NAME = "name"

        const val TABLE_EXPENSES = "Expenses"
        const val EXPENSE_ID = "_id"
        const val EXPENSE_NAME = "name"
        const val EXPENSE_AMOUNT = "amount"
        const val EXPENSE_PAYMENT_METHOD = "payment_method"
        const val EXPENSE_CATEGORY = "category"
        const val EXPENSE_DATE = "date"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_CATEGORIES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CATEGORY_NAME TEXT UNIQUE
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_EXPENSES (
                $EXPENSE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $EXPENSE_NAME TEXT NOT NULL,
                $EXPENSE_AMOUNT REAL NOT NULL,
                $EXPENSE_PAYMENT_METHOD TEXT NOT NULL,
                $EXPENSE_CATEGORY TEXT NOT NULL,
                $EXPENSE_DATE TEXT NOT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EXPENSES")
        onCreate(db)
    }

    fun insertCategory(category: String): Boolean {
        if (category.isBlank()) return false
        val db = writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_CATEGORY_NAME, category.trim().lowercase())
        }

        return try {
            val result = db.insertOrThrow(TABLE_CATEGORIES, null, contentValues)
            result != -1L
        } catch (e: SQLiteConstraintException) {
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db.close()
        }
    }

    fun getAllCategories(): List<String> {
        val categories = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT $COLUMN_CATEGORY_NAME FROM $TABLE_CATEGORIES", null)

        cursor.use {
            while (it.moveToNext()) {
                categories.add(it.getString(0))
            }
        }

        return categories
    }

    fun insertExpense(name: String, amount: Double, paymentMethod: String, category: String, date: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(EXPENSE_NAME, name)
            put(EXPENSE_AMOUNT, amount)
            put(EXPENSE_PAYMENT_METHOD, paymentMethod)
            put(EXPENSE_CATEGORY, category)
            put(EXPENSE_DATE, date)
        }
        db.insert(TABLE_EXPENSES, null, values)
        db.close()
    }

    fun getAllExpenses(): List<ExpenseEntry> {
        val expenses = mutableListOf<ExpenseEntry>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $EXPENSE_NAME, $EXPENSE_AMOUNT, $EXPENSE_DATE FROM $TABLE_EXPENSES ORDER BY $EXPENSE_ID DESC",
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                expenses.add(
                    ExpenseEntry(
                        it.getString(0),
                        it.getDouble(1),
                        it.getString(2)
                    )
                )
            }
        }

        return expenses
    }

    fun getLatestExpense(): Double {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $EXPENSE_AMOUNT FROM $TABLE_EXPENSES ORDER BY $EXPENSE_ID DESC LIMIT 1",
            null
        )

        var latestExpense = 0.0
        cursor.use {
            if (it.moveToFirst()) {
                latestExpense = it.getDouble(0)
            }
        }

        return latestExpense
    }

    fun getTotalExpenses(): Double {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT SUM($EXPENSE_AMOUNT) FROM $TABLE_EXPENSES", null)

        var total = 0.0
        cursor.use {
            if (it.moveToFirst()) {
                total = it.getDouble(0)
            }
        }

        db.close()
        return total
    }

    fun deleteCategory(category: String): Boolean {
        val db = writableDatabase
        val rowsDeleted = db.delete(
            TABLE_CATEGORIES,
            "$COLUMN_CATEGORY_NAME = ?",
            arrayOf(category.trim().lowercase())
        )
        db.close()
        return rowsDeleted > 0
    }

    fun getExpensesBetweenDates(startDate: String, endDate: String): List<ExpenseEntry> {
        val expenses = mutableListOf<ExpenseEntry>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $EXPENSE_NAME, $EXPENSE_AMOUNT, $EXPENSE_DATE FROM $TABLE_EXPENSES WHERE $EXPENSE_DATE BETWEEN ? AND ? ORDER BY $EXPENSE_DATE ASC",
            arrayOf(startDate, endDate)
        )

        cursor.use {
            while (it.moveToNext()) {
                val name = it.getString(0)
                val amount = it.getDouble(1)
                val date = it.getString(2)
                expenses.add(ExpenseEntry(name, amount, date))
            }
        }

        return expenses
    }

    data class ExpenseEntry(val name: String, val amount: Double, val date: String)
}
