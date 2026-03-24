package com.autexel.app.ui.invoice

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.autexel.app.R
import com.autexel.app.databinding.ActivityInvoiceBinding
import com.autexel.app.parser.TextParser
import com.autexel.app.utils.CurrencyManager
import com.autexel.app.utils.FileHelper
import com.autexel.app.utils.ReviewHelper
import com.autexel.app.utils.ShareHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class InvoiceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInvoiceBinding
    private val viewModel: InvoiceViewModel by viewModels()
    private var lastSavedFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.navigationContentDescription = "Navigate up"

        setupAccessibility()
        setupCurrencySpinner()
        setupTaxToggle()

        // On fresh launch only
        if (savedInstanceState == null) {
            val rawText = intent.getStringExtra("EXTRACTED_TEXT") ?: ""
            if (rawText.isNotEmpty()) {
                viewModel.rawText.value = rawText
                viewModel.parseItems(rawText)
                val pageCount = TextParser.splitPages(rawText).size
                if (pageCount > 1) {
                    Toast.makeText(this, "$pageCount pages detected - items combined from all pages", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Restore fields
        binding.etRawText.setText(viewModel.rawText.value)
        binding.etCompanyName.setText(viewModel.companyName.value)
        binding.etCustomerName.setText(viewModel.customerName.value)
        binding.etInvoiceNo.setText(viewModel.invoiceNo.value)
        binding.etDate.setText(viewModel.date.value)
        binding.etTaxRate?.setText(viewModel.taxRate.value.toInt().toString())
        binding.etTaxLabel?.setText(viewModel.taxLabel.value)

        // Two-way binding
        binding.etCompanyName.addTextChangedListener(simpleWatcher { viewModel.companyName.value = it })
        binding.etCustomerName.addTextChangedListener(simpleWatcher { viewModel.customerName.value = it })
        binding.etInvoiceNo.addTextChangedListener(simpleWatcher { viewModel.invoiceNo.value = it })
        binding.etDate.addTextChangedListener(simpleWatcher { viewModel.date.value = it })
        binding.etTaxRate?.addTextChangedListener(simpleWatcher {
            viewModel.taxRate.value = it.toFloatOrNull() ?: 0f
            updateTotal()
        })
        binding.etTaxLabel?.addTextChangedListener(simpleWatcher {
            viewModel.taxLabel.value = it
            updateTotal()
        })

        // Observe items
        lifecycleScope.launch { viewModel.items.collect { renderItems() } }

        // Observe export state
        lifecycleScope.launch {
            viewModel.exportState.collect { state ->
                when (state) {
                    is InvoiceViewModel.ExportState.Loading -> {
                        binding.btnGeneratePdf.isEnabled = false
                        binding.btnGeneratePdf.text = "Generating..."
                    }
                    is InvoiceViewModel.ExportState.Success -> {
                        binding.btnGeneratePdf.isEnabled = true
                        binding.btnGeneratePdf.text = "Generate PDF"
                        lastSavedFile = state.file
                        binding.btnOpenFile.visibility = View.VISIBLE
                        binding.btnShare.visibility = View.VISIBLE
                        Toast.makeText(this@InvoiceActivity, "Invoice saved: ${state.file.name}", Toast.LENGTH_LONG).show()
                        ReviewHelper.onExportSuccess(this@InvoiceActivity)
                        viewModel.resetExportState()
                    }
                    is InvoiceViewModel.ExportState.Error -> {
                        binding.btnGeneratePdf.isEnabled = true
                        binding.btnGeneratePdf.text = "Generate PDF"
                        Toast.makeText(this@InvoiceActivity, state.message, Toast.LENGTH_SHORT).show()
                        viewModel.resetExportState()
                    }
                    else -> { binding.btnGeneratePdf.isEnabled = true }
                }
            }
        }

        binding.btnParse.setOnClickListener {
            val raw = binding.etRawText.text.toString()
            viewModel.rawText.value = raw
            viewModel.parseItems(raw)
        }
        binding.btnAddItem.setOnClickListener { viewModel.addEmptyItem() }
        binding.btnGeneratePdf.setOnClickListener {
            if (binding.etCompanyName.text.isNullOrBlank()) {
                binding.etCompanyName.error = "Required"
                binding.etCompanyName.requestFocus()
                return@setOnClickListener
            }
            viewModel.generatePdf()
        }
        binding.btnOpenFile.setOnClickListener { lastSavedFile?.let { FileHelper.openFile(this, it) } }
        binding.btnShare.setOnClickListener {
            lastSavedFile?.let { ShareHelper.shareFile(this, it, "Invoice ${viewModel.invoiceNo.value}") }
        }
    }

    override fun onPause() { super.onPause(); viewModel.saveState() }

    // ── Currency Spinner ─────────────────────────────────────────────────────

    private fun setupCurrencySpinner() {
        val currencies = CurrencyManager.currencies
        val adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_item,
            currencies.map { "${it.symbol}  ${it.name}" })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCurrency.adapter = adapter

        // Restore or auto-detect
        if (viewModel.currencyCode.value.isEmpty()) {
            viewModel.currencyCode.value = CurrencyManager.getSelected(this).code
        }
        val idx = currencies.indexOfFirst { it.code == viewModel.currencyCode.value }.coerceAtLeast(0)
        binding.spinnerCurrency.setSelection(idx, false)

        binding.spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val selected = currencies[pos]
                viewModel.currencyCode.value = selected.code
                CurrencyManager.setSelected(this@InvoiceActivity, selected.code)
                updateTotal()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // ── Tax Toggle ────────────────────────────────────────────────────────────

    private fun setupTaxToggle() {
        binding.switchTax?.isChecked = viewModel.taxEnabled.value
        binding.taxFieldsContainer?.visibility =
            if (viewModel.taxEnabled.value) View.VISIBLE else View.GONE

        binding.switchTax?.setOnCheckedChangeListener { _, checked ->
            viewModel.taxEnabled.value = checked
            binding.taxFieldsContainer?.visibility = if (checked) View.VISIBLE else View.GONE
            updateTotal()
        }
    }

    // ── Accessibility ─────────────────────────────────────────────────────────

    private fun setupAccessibility() {
        binding.btnParse.contentDescription       = "Parse scanned text into invoice line items"
        binding.btnAddItem.contentDescription     = "Add a new line item to the invoice"
        binding.btnGeneratePdf.contentDescription = "Generate and save PDF invoice"
        binding.btnOpenFile.contentDescription    = "Open the saved PDF invoice"
        binding.btnShare.contentDescription       = "Share the saved PDF invoice"
        binding.etCompanyName.contentDescription  = "Enter your company name"
        binding.etCustomerName.contentDescription = "Enter the customer name"
        binding.etInvoiceNo.contentDescription    = "Enter the invoice number"
        binding.etDate.contentDescription         = "Enter the invoice date"
        binding.spinnerCurrency.contentDescription = "Select currency for invoice"
    }

    // ── Items ─────────────────────────────────────────────────────────────────

    private fun renderItems() {
        binding.itemsContainer.removeAllViews()
        viewModel.items.value.forEachIndexed { idx, _ -> addItemRowView(idx) }
        updateTotal()
    }

    private fun addItemRowView(idx: Int) {
        val rowView = LayoutInflater.from(this).inflate(R.layout.item_invoice_row, binding.itemsContainer, false)
        val etName      = rowView.findViewById<EditText>(R.id.etItemName)
        val etQty       = rowView.findViewById<EditText>(R.id.etQty)
        val etPrice     = rowView.findViewById<EditText>(R.id.etPrice)
        val tvLineTotal = rowView.findViewById<TextView>(R.id.tvLineTotal)
        val btnDelete   = rowView.findViewById<View>(R.id.btnDelete)

        val rowNum = idx + 1
        etName.contentDescription      = "Item name for row $rowNum"
        etQty.contentDescription       = "Quantity for row $rowNum"
        etPrice.contentDescription     = "Unit price for row $rowNum"
        tvLineTotal.contentDescription = "Line total for row $rowNum"
        btnDelete.contentDescription   = "Delete row $rowNum"

        val item = viewModel.items.value[idx]
        etName.setText(item.name)
        etQty.setText(if (item.quantity == item.quantity.toLong().toDouble()) item.quantity.toLong().toString() else item.quantity.toString())
        etPrice.setText(if (item.price > 0) String.format("%.2f", item.price) else "")
        updateLineTotal(tvLineTotal, item.quantity, item.price)

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(e: Editable?) {
                if (idx >= viewModel.items.value.size) return
                val name  = etName.text.toString()
                val qty   = etQty.text.toString().toDoubleOrNull() ?: 1.0
                val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
                viewModel.updateItem(idx, name, qty, price)
                updateLineTotal(tvLineTotal, qty, price)
                updateTotal()
            }
        }
        etName.addTextChangedListener(watcher)
        etQty.addTextChangedListener(watcher)
        etPrice.addTextChangedListener(watcher)

        btnDelete.setOnClickListener {
            if (viewModel.items.value.size <= 1) Toast.makeText(this, "At least one item is required", Toast.LENGTH_SHORT).show()
            else viewModel.deleteItem(idx)
        }
        binding.itemsContainer.addView(rowView)
    }

    private fun updateLineTotal(tv: TextView, qty: Double, price: Double) {
        tv.text = String.format("%.2f", qty * price)
    }

    private fun updateTotal() {
        val sym   = CurrencyManager.getSelected(this).symbol
        val grand = viewModel.getGrandTotal()
        val sub   = viewModel.getSubtotal()
        val tax   = viewModel.getTax()
        val label = viewModel.taxLabel.value.ifEmpty { "Tax" }
        val rate  = viewModel.taxRate.value

        if (viewModel.taxEnabled.value && rate > 0) {
            binding.tvTotal.text = "$sym ${String.format("%,.2f", grand)}  (${label} ${rate.toInt()}%: $sym ${String.format("%,.2f", tax)})"
        } else {
            binding.tvTotal.text = "$sym ${String.format("%,.2f", sub)}"
        }
    }

    private fun simpleWatcher(onChanged: (String) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) { onChanged(s.toString()) }
    }
}
