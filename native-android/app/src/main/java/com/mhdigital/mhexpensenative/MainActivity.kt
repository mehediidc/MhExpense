package com.mhdigital.mhexpensenative

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mhdigital.mhexpensenative.databinding.ActivityMainBinding
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: ExpenseDbHelper
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "BD"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = ExpenseDbHelper(this)
        setupRecyclerView()
        setupFab()
        refreshData()
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    private fun setupRecyclerView() {
        binding.transactionsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupFab() {
        binding.fabAddTransaction.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
    }

    private fun refreshData() {
        val transactions = dbHelper.getTransactions()
        val totals = dbHelper.getTotals()

        binding.totalBalanceText.text = formatCurrency(totals.balance)
        binding.incomeText.text = formatCurrency(totals.income)
        binding.expenseText.text = formatCurrency(totals.expense)

        if (transactions.isEmpty()) {
            binding.emptyStateText.visibility = View.VISIBLE
            binding.transactionsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateText.visibility = View.GONE
            binding.transactionsRecyclerView.visibility = View.VISIBLE
            binding.transactionsRecyclerView.adapter = TransactionAdapter(transactions)
        }
    }

    private fun formatCurrency(value: Double): String {
        return currencyFormatter.format(value)
    }
}

class TransactionAdapter(
    private val transactions: List<Transaction>
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val item = transactions[position]
        holder.titleView.text = item.category
        holder.metaView.text = "${item.type} • ${item.date}"
        val value = if (item.type == "Income") "+${formatCurrency(item.amount)}" else "-${formatCurrency(item.amount)}"
        holder.amountView.text = value
        holder.amountView.setTextColor(
            if (item.type == "Income") holder.itemView.context.getColor(R.color.brand_green)
            else holder.itemView.context.getColor(R.color.brand_red)
        )
    }

    override fun getItemCount(): Int = transactions.size

    private fun formatCurrency(value: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "BD"))
        return format.format(value)
    }

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleView: TextView = itemView.findViewById(R.id.transactionTitle)
        val metaView: TextView = itemView.findViewById(R.id.transactionMeta)
        val amountView: TextView = itemView.findViewById(R.id.transactionAmount)
    }
}

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var dbHelper: ExpenseDbHelper
    private lateinit var typeAdapter: ArrayAdapter<String>
    private lateinit var categoryAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        dbHelper = ExpenseDbHelper(this)

        val typeItems = listOf("Income", "Expense")
        val categoryItems = listOf(
            "Salary",
            "Freelance",
            "Food",
            "Transport",
            "Shopping",
            "Rent",
            "Utilities",
            "Health",
            "Education",
            "Other"
        )

        val typeSpinner = findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.typeSpinner)
        val categorySpinner = findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.categorySpinner)
        val amountInput = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.amountInput)
        val noteInput = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.noteInput)
        val dateInput = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.dateInput)
        val saveButton = findViewById<android.widget.Button>(R.id.saveButton)

        typeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, typeItems)
        categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryItems)

        typeSpinner.setAdapter(typeAdapter)
        categorySpinner.setAdapter(categoryAdapter)
        typeSpinner.setText(typeItems.first(), false)
        categorySpinner.setText(categoryItems.first(), false)

        val today = Calendar.getInstance()
        val dateString = String.format("%tY-%tm-%td", today, today, today)
        dateInput.setText(dateString)

        dateInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val selected = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, day)
                    dateInput.setText(selected)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        saveButton.setOnClickListener {
            val amountText = amountInput.text?.toString()?.trim().orEmpty()
            val note = noteInput.text?.toString()?.trim().orEmpty()
            val type = typeSpinner.text.toString().trim()
            val category = categorySpinner.text.toString().trim()
            val date = dateInput.text?.toString()?.trim().orEmpty()

            if (amountText.isEmpty()) {
                Toast.makeText(this, "Enter an amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull() ?: run {
                Toast.makeText(this, "Use a valid number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val transaction = Transaction(
                type = type,
                category = category,
                amount = amount,
                note = note.ifEmpty { "No note" },
                date = date.ifEmpty { dateString }
            )

            dbHelper.addTransaction(transaction)
            Toast.makeText(this, "Saved successfully", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
