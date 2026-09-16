package com.mhdigital.mhexpensenative

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Transaction(
    val id: Long = 0,
    val type: String,
    val category: String,
    val amount: Double,
    val note: String,
    val date: String
)

data class Totals(
    val income: Double,
    val expense: Double,
    val balance: Double
)

class ExpenseDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "expense_db"
        const val DATABASE_VERSION = 1
        const val TABLE_NAME = "transactions"
        const val COL_ID = "id"
        const val COL_TYPE = "type"
        const val COL_CATEGORY = "category"
        const val COL_AMOUNT = "amount"
        const val COL_NOTE = "note"
        const val COL_DATE = "date"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_NAME (" +
                "$COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COL_TYPE TEXT NOT NULL, " +
                "$COL_CATEGORY TEXT NOT NULL, " +
                "$COL_AMOUNT REAL NOT NULL, " +
                "$COL_NOTE TEXT, " +
                "$COL_DATE TEXT NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun addTransaction(transaction: Transaction): Long {
        val db = writableDatabase
        return db.insert(TABLE_NAME, null, ContentValues().apply {
            put(COL_TYPE, transaction.type)
            put(COL_CATEGORY, transaction.category)
            put(COL_AMOUNT, transaction.amount)
            put(COL_NOTE, transaction.note)
            put(COL_DATE, transaction.date)
        })
    }

    fun getTransactions(): List<Transaction> {
        val list = mutableListOf<Transaction>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_NAME ORDER BY $COL_ID DESC",
            null
        )

        while (cursor.moveToNext()) {
            list.add(
                Transaction(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                    type = cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE)),
                    category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT)),
                    note = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTE)),
                    date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE))
                )
            )
        }
        cursor.close()
        return list
    }

    fun getTotals(): Totals {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT " +
                "SUM(CASE WHEN type = 'Income' THEN amount ELSE 0 END) AS income, " +
                "SUM(CASE WHEN type = 'Expense' THEN amount ELSE 0 END) AS expense " +
                "FROM $TABLE_NAME",
            null
        )

        var income = 0.0
        var expense = 0.0

        if (cursor.moveToFirst()) {
            income = cursor.getDouble(cursor.getColumnIndexOrThrow("income"))
            expense = cursor.getDouble(cursor.getColumnIndexOrThrow("expense"))
        }
        cursor.close()

        return Totals(income = income, expense = expense, balance = income - expense)
    }
}
