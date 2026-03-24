package com.autexel.app.utils

import android.content.Context
import com.autexel.app.utils.HistoryManager
import com.autexel.app.utils.CurrencyManager
import android.os.Environment
import com.autexel.app.parser.TextParser.InvoiceItem
import com.itextpdf.text.*
import com.itextpdf.text.pdf.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfInvoiceGenerator {

 private val BLUE = BaseColor(26, 115, 232)
 private val ORANGE = BaseColor(255, 109, 0)
 private val LIGHT_GRAY = BaseColor(248, 249, 250)
 private val DARK = BaseColor(33, 33, 33)
 private val MID = BaseColor(95, 99, 104)
 private val WHITE = BaseColor.WHITE
 private val DIVIDER = BaseColor(224, 224, 224)

 data class InvoiceConfig(
        val companyName: String,
        val customerName: String,
        val invoiceNo: String,
        val date: String,
        val items: List<InvoiceItem>,
        val taxRate: Double = 0.0,
        val currencyCode: String = "INR",
        val taxLabel: String = ""
    )

 fun generate(context: Context, config: InvoiceConfig, fileName: String? = null): File? {
 return try {
 val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
 val name = fileName ?: "Invoice_${config.invoiceNo.ifEmpty { timestamp }}.pdf"
 val file = getDownloadsFile(context, name)

 val document = Document(PageSize.A4, 40f, 40f, 40f, 40f)
 PdfWriter.getInstance(document, FileOutputStream(file))
 document.open()

 // ── Header ───────────────────────────────────────
 addHeader(document, config)

 // ── Divider ──────────────────────────────────────
 addDivider(document)

 // ── Bill To / Invoice Info ────────────────────────
 addBillingInfo(document, config)

 document.add(Paragraph(" "))

 // ── Items Table ───────────────────────────────────
        // Currency lookup
        val currency = CurrencyManager.currencies.find { it.code == config.currencyCode } ?: CurrencyManager.currencies[0]
        val sym = currency.symbol
        val taxLabel = currency.taxLabel

        addItemsTable(document, config.items, sym)

 // ── Total ─────────────────────────────────────────
        addTotals(document, config.items, config.taxRate, sym, taxLabel)

 // ── Footer ────────────────────────────────────────
 addFooter(document)

 document.close()
 file
 } catch (e: Exception) {
 e.printStackTrace()
 null
 }
 }

 private fun addHeader(document: Document, config: InvoiceConfig) {
 val headerTable = PdfPTable(2).apply {
 widthPercentage = 100f
 setWidths(floatArrayOf(1.5f, 1f))
 }

 // Company name cell
 val companyFont = Font(Font.FontFamily.HELVETICA, 22f, Font.BOLD, BLUE)
 val companyCell = PdfPCell(Phrase(config.companyName.ifEmpty { "Company Name" }, companyFont)).apply {
 border = Rectangle.NO_BORDER
 verticalAlignment = Element.ALIGN_MIDDLE
 }
 headerTable.addCell(companyCell)

 // Invoice title cell
 val invoiceFont = Font(Font.FontFamily.HELVETICA, 28f, Font.BOLD, ORANGE)
 val invoiceCell = PdfPCell(Phrase("INVOICE", invoiceFont)).apply {
 border = Rectangle.NO_BORDER
 horizontalAlignment = Element.ALIGN_RIGHT
 verticalAlignment = Element.ALIGN_MIDDLE
 }
 headerTable.addCell(invoiceCell)
 document.add(headerTable)

 // Invoice meta info
 val metaTable = PdfPTable(2).apply {
 widthPercentage = 100f
 setWidths(floatArrayOf(1.5f, 1f))
 spacingBefore = 8f
 }
 val emptyCell = PdfPCell(Phrase("")).apply { border = Rectangle.NO_BORDER }
 metaTable.addCell(emptyCell)

 val metaFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, MID)
 val metaBoldFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, DARK)
 val metaText = buildString {
 append("Invoice No: "); append(config.invoiceNo.ifEmpty { "001" }); append("\n")
 append("Date: "); append(config.date.ifEmpty {
 SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
 })
 }
 val metaCell = PdfPCell(Phrase(metaText, metaFont)).apply {
 border = Rectangle.NO_BORDER
 horizontalAlignment = Element.ALIGN_RIGHT
 }
 metaTable.addCell(metaCell)
 document.add(metaTable)
 }

 private fun addDivider(document: Document) {
 val line = LineSeparator(1f, 100f, BLUE, Element.ALIGN_CENTER, -2f)
 document.add(Chunk(line))
 document.add(Paragraph(" "))
 }

 private fun addBillingInfo(document: Document, config: InvoiceConfig) {
 val table = PdfPTable(2).apply {
 widthPercentage = 100f
 setWidths(floatArrayOf(1f, 1f))
 }

 val labelFont = Font(Font.FontFamily.HELVETICA, 9f, Font.BOLD, MID)
 val valueFont = Font(Font.FontFamily.HELVETICA, 11f, Font.BOLD, DARK)

 fun billingCell(label: String, value: String): PdfPCell {
 val inner = Paragraph().apply {
 add(Phrase("$label\n", labelFont))
 add(Phrase(value.ifEmpty { "N/A" }, valueFont))
 }
 return PdfPCell(inner).apply {
 border = Rectangle.NO_BORDER
 backgroundColor = LIGHT_GRAY
 padding = 10f
 }
 }

 table.addCell(billingCell("BILLED BY", config.companyName.ifEmpty { "Your Company" }))
 table.addCell(billingCell("BILLED TO", config.customerName.ifEmpty { "Customer" }))
 document.add(table)
 }

    private fun addItemsTable(document: Document, items: List<InvoiceItem>, sym: String) {
 document.add(Paragraph(" "))
 val table = PdfPTable(4).apply {
 widthPercentage = 100f
 setWidths(floatArrayOf(3f, 1f, 1.5f, 1.5f))
 }

 val headerFont = Font(Font.FontFamily.HELVETICA, 11f, Font.BOLD, WHITE)
 val headers = listOf("DESCRIPTION", "QTY", "UNIT PRICE", "TOTAL")
 headers.forEach { header ->
 val cell = PdfPCell(Phrase(header, headerFont)).apply {
 backgroundColor = BLUE
 padding = 10f
 horizontalAlignment = if (header == "DESCRIPTION") Element.ALIGN_LEFT else Element.ALIGN_RIGHT
 border = Rectangle.NO_BORDER
 }
 table.addCell(cell)
 }

 val bodyFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, DARK)
 items.forEachIndexed { idx, item ->
 val bg = if (idx % 2 == 0) WHITE else LIGHT_GRAY
 val total = item.quantity * item.price

 fun dataCell(text: String, align: Int = Element.ALIGN_LEFT) =
 PdfPCell(Phrase(text, bodyFont)).apply {
 backgroundColor = bg
 padding = 9f
 horizontalAlignment = align
 borderColor = DIVIDER
 borderWidth = 0.5f
 }

 table.addCell(dataCell(item.name))
 table.addCell(dataCell(formatNum(item.quantity), Element.ALIGN_RIGHT))
 table.addCell(dataCell("${sym}${formatAmount(item.price)}", Element.ALIGN_RIGHT))
 table.addCell(dataCell("${sym}${formatAmount(total)}", Element.ALIGN_RIGHT))
 }

 document.add(table)
 }

    private fun addTotals(document: Document, items: List<InvoiceItem>, taxRateVal: Double = 18.0, sym: String = "Rs.", taxLabel: String = "GST") {
 val subtotal = items.sumOf { it.quantity * it.price }
 val tax = subtotal * (taxRateVal / 100.0)
 val grandTotal = subtotal + tax

 val table = PdfPTable(2).apply {
 widthPercentage = 45f
 horizontalAlignment = Element.ALIGN_RIGHT
 spacingBefore = 4f
 }

 val labelFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, MID)
 val valueFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, DARK)
 val boldFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, WHITE)

 fun totalRow(label: String, value: String, highlight: Boolean = false) {
 val bg = if (highlight) ORANGE else WHITE
 val lf = if (highlight) boldFont else labelFont
 val vf = if (highlight) boldFont else valueFont

 table.addCell(PdfPCell(Phrase(label, lf)).apply {
 backgroundColor = bg; padding = 8f; border = Rectangle.NO_BORDER
 })
 table.addCell(PdfPCell(Phrase(value, vf)).apply {
 backgroundColor = bg; padding = 8f; border = Rectangle.NO_BORDER
 horizontalAlignment = Element.ALIGN_RIGHT
 })
 }

 totalRow("Subtotal", "${sym}${formatAmount(subtotal)}")
 totalRow("${taxLabel} (${taxRateVal.toInt()}%)", "${sym}${formatAmount(tax)}")
 totalRow("GRAND TOTAL", "${sym}${formatAmount(grandTotal)}", highlight = true)

 document.add(table)
 }

 private fun addFooter(document: Document) {
 document.add(Paragraph("\n\n"))
 val footerFont = Font(Font.FontFamily.HELVETICA, 9f, Font.ITALIC, MID)
 val footer = Paragraph("Generated by Autexel - Thank you for your business!", footerFont).apply {
 alignment = Element.ALIGN_CENTER
 }
 document.add(footer)
 }

 private fun formatAmount(value: Double): String =
 String.format("%,.2f", value)

 private fun formatNum(value: Double): String =
 if (value == value.toLong().toDouble()) value.toLong().toString()
 else String.format("%.2f", value)

 private fun getDownloadsFile(context: Context, name: String): File {
 val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
 if (downloadsDir.exists() || downloadsDir.mkdirs()) {
 return File(downloadsDir, name)
 }
 val appDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
 ?: context.filesDir
 appDir.mkdirs()
 return File(appDir, name)
 }
}
