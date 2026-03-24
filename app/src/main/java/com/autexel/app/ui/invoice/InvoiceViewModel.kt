package com.autexel.app.ui.invoice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.autexel.app.parser.TextParser
import com.autexel.app.utils.PdfInvoiceGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class InvoiceViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    companion object {
        private const val KEY_ITEMS = "invoice_items_names"
        private const val KEY_ITEMS_QTY = "invoice_items_qty"
        private const val KEY_ITEMS_PRICE = "invoice_items_price"
        private const val KEY_COMPANY = "company_name"
        private const val KEY_CUSTOMER = "customer_name"
        private const val KEY_INVOICE_NO = "invoice_no"
        private const val KEY_DATE = "invoice_date"
        private const val KEY_RAW = "raw_text"
        private const val KEY_TAX_RATE    = "tax_rate"
        private const val KEY_TAX_ENABLED = "tax_enabled"
        private const val KEY_TAX_LABEL   = "tax_label"
        private const val KEY_CURRENCY = "currency_code"
    }

    private val _items = MutableStateFlow<MutableList<TextParser.InvoiceItem>>(mutableListOf())
    val items: StateFlow<MutableList<TextParser.InvoiceItem>> = _items

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState

    val companyName = MutableStateFlow(savedStateHandle.get<String>(KEY_COMPANY) ?: "")
    val customerName = MutableStateFlow(savedStateHandle.get<String>(KEY_CUSTOMER) ?: "")
    val invoiceNo = MutableStateFlow(
        savedStateHandle.get<String>(KEY_INVOICE_NO)
            ?: "INV-${System.currentTimeMillis() % 10000}"
    )
    val date = MutableStateFlow(
        savedStateHandle.get<String>(KEY_DATE)
            ?: SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    )
    val rawText = MutableStateFlow(savedStateHandle.get<String>(KEY_RAW) ?: "")
    val taxRate    = MutableStateFlow(savedStateHandle.get<Float>(KEY_TAX_RATE) ?: 0f)
    val taxEnabled = MutableStateFlow(savedStateHandle.get<Boolean>(KEY_TAX_ENABLED) ?: false)
    val taxLabel   = MutableStateFlow(savedStateHandle.get<String>(KEY_TAX_LABEL) ?: "Tax")
    val currencyCode = MutableStateFlow(savedStateHandle.get<String>(KEY_CURRENCY) ?: "INR")

    init {
        // Restore items on rotation
        val names = savedStateHandle.get<ArrayList<String>>(KEY_ITEMS) ?: return
        val qtys = savedStateHandle.get<FloatArray>(KEY_ITEMS_QTY) ?: return
        val prices = savedStateHandle.get<FloatArray>(KEY_ITEMS_PRICE) ?: return
        if (names.size == qtys.size && names.size == prices.size) {
            _items.value = names.indices.map { i ->
                TextParser.InvoiceItem(names[i], qtys[i].toDouble(), prices[i].toDouble())
            }.toMutableList()
        }
    }

    fun saveState() {
        savedStateHandle[KEY_COMPANY] = companyName.value
        savedStateHandle[KEY_CUSTOMER] = customerName.value
        savedStateHandle[KEY_INVOICE_NO] = invoiceNo.value
        savedStateHandle[KEY_DATE] = date.value
        savedStateHandle[KEY_RAW] = rawText.value
        savedStateHandle[KEY_TAX_RATE]    = taxRate.value
        savedStateHandle[KEY_TAX_ENABLED] = taxEnabled.value
        savedStateHandle[KEY_TAX_LABEL]   = taxLabel.value
        savedStateHandle[KEY_CURRENCY] = currencyCode.value
        val current = _items.value
        savedStateHandle[KEY_ITEMS] = ArrayList(current.map { it.name })
        savedStateHandle[KEY_ITEMS_QTY] = current.map { it.quantity.toFloat() }.toFloatArray()
        savedStateHandle[KEY_ITEMS_PRICE] = current.map { it.price.toFloat() }.toFloatArray()
    }

    fun parseItems(raw: String) {
        if (raw.isBlank()) return
        val corrected = TextParser.quickCorrect(raw)
        val invoiceData = TextParser.parseMultiPageToInvoice(corrected)
        if (companyName.value.isBlank() && invoiceData.companyHint.isNotEmpty())
            companyName.value = invoiceData.companyHint
        if (customerName.value.isBlank() && invoiceData.customerHint.isNotEmpty())
            customerName.value = invoiceData.customerHint
        if (invoiceData.dateHint.isNotEmpty()) date.value = invoiceData.dateHint
        if (invoiceData.invoiceNoHint.isNotEmpty()) invoiceNo.value = invoiceData.invoiceNoHint
        _items.value = invoiceData.items
        saveState()
    }

    fun updateItem(index: Int, name: String, qty: Double, price: Double) {
        val list = _items.value.toMutableList()
        if (index < list.size) {
            list[index] = TextParser.InvoiceItem(name, qty, price)
            _items.value = list
        }
    }

    fun addEmptyItem() {
        val list = _items.value.toMutableList()
        list.add(TextParser.InvoiceItem("", 1.0, 0.0))
        _items.value = list
        saveState()
    }

    fun deleteItem(index: Int) {
        val list = _items.value.toMutableList()
        if (list.size > 1 && index < list.size) {
            list.removeAt(index)
            _items.value = list
            saveState()
        }
    }

    fun getSubtotal(): Double = _items.value.sumOf { it.quantity * it.price }
    fun getTax(): Double = if (taxEnabled.value) getSubtotal() * (taxRate.value / 100.0) else 0.0
    fun getGrandTotal(): Double = getSubtotal() + getTax()

    fun generatePdf() {
        if (companyName.value.isBlank()) return
        _exportState.value = ExportState.Loading
        val config = PdfInvoiceGenerator.InvoiceConfig(
            companyName = companyName.value,
            customerName = customerName.value,
            invoiceNo = invoiceNo.value,
            date = date.value,
            items = _items.value.toList(),
            taxRate      = if (taxEnabled.value) taxRate.value.toDouble() else 0.0,
            currencyCode = currencyCode.value,
            taxLabel     = if (taxEnabled.value) taxLabel.value else ""
        )
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                PdfInvoiceGenerator.generate(getApplication(), config)
            }
            _exportState.value = if (file != null) ExportState.Success(file) else ExportState.Error("PDF generation failed")
        }
    }

    fun resetExportState() { _exportState.value = ExportState.Idle }

    sealed class ExportState {
        object Idle : ExportState()
        object Loading : ExportState()
        data class Success(val file: File) : ExportState()
        data class Error(val message: String) : ExportState()
    }
}
