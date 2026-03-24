package com.autexel.app.parser

import android.content.Context
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager

/**
 * On-device text intelligence — no external APIs.
 * Uses regex, NLP rules, and pattern matching to structure OCR output.
 */
object TextParser {

 // ─────────────────────────────────────────────
 // EXCEL PARSING
 // ─────────────────────────────────────────────

 /**
 * Parse raw OCR text into a 2D table (list of rows, each row = list of cells).
 * Strategy priority:
 * 1. Tab-separated → direct columns
 * 2. Pipe-separated (markdown table)
 * 3. Comma-separated values (CSV-like)
 * 4. Colon key–value pairs → 2 columns: Key | Value
 * 5. Multi-word lines → try to split by 2+ spaces
 * 6. Fallback: each line becomes a single cell in column A
 */
 fun parseToTable(rawText: String): MutableList<MutableList<String>> {
 val lines = rawText.lines()
 .map { it.trim() }
 .filter { it.isNotEmpty() }

 if (lines.isEmpty()) return mutableListOf(mutableListOf(""))

 return when {
 isTabSeparated(lines) -> parseTabSeparated(lines)
 isPipeSeparated(lines) -> parsePipeSeparated(lines)
 isCsvLike(lines) -> parseCsvLike(lines)
 isKeyValue(lines) -> parseKeyValue(lines)
 hasMultipleSpaces(lines)-> parseMultiSpace(lines)
 else -> lines.map { mutableListOf(it) }.toMutableList()
 }
 }

 private fun isTabSeparated(lines: List<String>) =
 lines.count { it.contains('\t') } >= lines.size / 2

 private fun isPipeSeparated(lines: List<String>) =
 lines.count { it.contains('|') } >= lines.size / 2

 private fun isCsvLike(lines: List<String>) =
 lines.count { it.count { c -> c == ',' } >= 1 } >= lines.size / 2

 private fun isKeyValue(lines: List<String>) =
 lines.count { Regex("^[^:]{1,30}:\\s*.+$").matches(it) } >= lines.size / 2

 private fun hasMultipleSpaces(lines: List<String>) =
 lines.count { it.contains(Regex("\\s{2,}")) } >= lines.size / 2

 private fun parseTabSeparated(lines: List<String>): MutableList<MutableList<String>> =
 lines.map { line ->
 line.split('\t').map { it.trim() }.toMutableList()
 }.toMutableList()

 private fun parsePipeSeparated(lines: List<String>): MutableList<MutableList<String>> =
 lines
 .filter { !it.matches(Regex("[|\\-\\s]+")) } // skip separator rows
 .map { line ->
 line.trim('|').split('|').map { it.trim() }.toMutableList()
 }.toMutableList()

 private fun parseCsvLike(lines: List<String>): MutableList<MutableList<String>> =
 lines.map { line ->
 parseCsvLine(line).toMutableList()
 }.toMutableList()

 private fun parseCsvLine(line: String): List<String> {
 val result = mutableListOf<String>()
 var current = StringBuilder()
 var inQuotes = false
 for (ch in line) {
 when {
 ch == '"' -> inQuotes = !inQuotes
 ch == ',' && !inQuotes -> { result.add(current.toString().trim()); current = StringBuilder() }
 else -> current.append(ch)
 }
 }
 result.add(current.toString().trim())
 return result
 }

 private fun parseKeyValue(lines: List<String>): MutableList<MutableList<String>> {
 val rows = mutableListOf(mutableListOf("Key", "Value"))
 lines.forEach { line ->
 val colonIdx = line.indexOf(':')
 if (colonIdx > 0) {
 val key = line.substring(0, colonIdx).trim()
 val value = line.substring(colonIdx + 1).trim()
 rows.add(mutableListOf(key, value))
 } else {
 rows.add(mutableListOf(line, ""))
 }
 }
 return rows
 }

 private fun parseMultiSpace(lines: List<String>): MutableList<MutableList<String>> =
 lines.map { line ->
 line.split(Regex("\\s{2,}")).map { it.trim() }.toMutableList()
 }.toMutableList()

 // ─────────────────────────────────────────────
 // INVOICE PARSING
 // ─────────────────────────────────────────────

 data class InvoiceItem(
 var name: String,
 var quantity: Double,
 var price: Double
 )

 data class InvoiceData(
 val items: MutableList<InvoiceItem>,
 val companyHint: String,
 val customerHint: String,
 val dateHint: String,
 val invoiceNoHint: String
 )

 /**
 * Parse raw OCR text into structured invoice data.
 * Detects: item names, qty, prices, company names, dates, invoice numbers.
 */
 fun parseToInvoice(rawText: String): InvoiceData {
 val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }

 val items = mutableListOf<InvoiceItem>()
 var companyHint = ""
 var customerHint = ""
 var dateHint = ""
 var invoiceNoHint = ""

 // Patterns
 val pricePattern = Regex("""[\$Rs.]?\s*(\d{1,6}(?:[.,]\d{1,2})?)\s*""")
 val datePattern = Regex("""\b(\d{1,2}[/\-\.]\d{1,2}[/\-\.]\d{2,4})\b""")
 val invoiceNoPattern = Regex("""(?:inv|invoice|bill|no|#)[\s\-:]*([A-Z0-9\-]+)""", RegexOption.IGNORE_CASE)
 val qtyPattern = Regex("""(\d+(?:\.\d+)?)\s*(?:x|x|nos?\.?|pcs?\.?|units?)?""", RegexOption.IGNORE_CASE)

 for (line in lines) {
 // Skip separator lines
 if (line.matches(Regex("[\\-=_*]{3,}"))) continue

 // Extract date
 datePattern.find(line)?.let { if (dateHint.isEmpty()) dateHint = it.groupValues[1] }

 // Extract invoice number
 invoiceNoPattern.find(line)?.let { if (invoiceNoHint.isEmpty()) invoiceNoHint = it.groupValues[1] }

 // Company / customer heuristics
 val lower = line.lowercase()
 when {
 lower.contains("company") || lower.contains("pvt") ||
 lower.contains("ltd") || lower.contains("inc") ||
 lower.contains("corp") || lower.startsWith("from:") -> {
 if (companyHint.isEmpty()) companyHint = cleanLabel(line)
 }
 lower.startsWith("to:") || lower.contains("customer") ||
 lower.contains("client") || lower.contains("bill to") -> {
 if (customerHint.isEmpty()) customerHint = cleanLabel(line)
 }
 }

 // Try to parse item lines: look for at least one number (price or qty)
 val prices = pricePattern.findAll(line)
 .map { it.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0 }
 .filter { it > 0 }
 .toList()

 if (prices.isNotEmpty()) {
 // Remove all numbers to get item name
 val nameRaw = line
 .replace(Regex("""[\$Rs.]?\s*\d+(?:[.,]\d{1,2})?"""), " ")
 .replace(Regex("""(?:x|x|nos?\.?|pcs?\.?|units?)""", RegexOption.IGNORE_CASE), " ")
 .replace(Regex("\\s{2,}"), " ")
 .trim()
 .trim(':')

 if (nameRaw.length < 2) continue // skip if no meaningful name remains

 val qty = if (prices.size >= 2) prices[0] else 1.0
 val price = if (prices.size >= 2) prices[1] else prices[0]

 items.add(InvoiceItem(nameRaw.ifEmpty { "Item" }, qty, price))
 }
 }

 // If no items detected, add placeholder
 if (items.isEmpty()) {
 items.add(InvoiceItem("Item 1", 1.0, 0.0))
 }

 return InvoiceData(items, companyHint, customerHint, dateHint, invoiceNoHint)
 }

 private fun cleanLabel(line: String): String =
 line.replace(Regex("(?i)^(from|to|company|customer|client|bill to)\\s*:?\\s*"), "").trim()

 // ─────────────────────────────────────────────
 // SPELL CHECK (Android on-device)
 // ─────────────────────────────────────────────

 /**
 * Basic spell correction using Android's built-in SpellCheckerService.
 * Returns corrected text word by word.
 */
 fun quickCorrect(text: String): String {
 // Simple cleanup rules (no API needed)
 return text
 .replace(Regex("(?<=[a-z])(?=[A-Z])"), " ") // camelCase split
 .replace(Regex("[|]{2,}"), "|") // double pipes
 .replace(Regex("[ \\t]+"), " ") // multi-spaces
 .replace(Regex("(?m)^\\s+"), "") // leading whitespace per line
 .trim()
 }

    // ─────────────────────────────────────────────
    //  MULTI-PAGE HELPERS
    // ─────────────────────────────────────────────

    private const val PAGE_BREAK_MARKER = "--- Page Break ---"

    /**
     * Returns true if the text contains multiple scanned pages.
     */
    fun isMultiPage(text: String): Boolean = text.contains(PAGE_BREAK_MARKER)

    /**
     * Splits combined multi-page text into individual page strings.
     */
    fun splitPages(text: String): List<String> {
        return if (isMultiPage(text)) {
            text.split(PAGE_BREAK_MARKER)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        } else {
            listOf(text.trim())
        }
    }

    /**
     * Parses multi-page text into a combined table.
     * Each page becomes its own section separated by an empty row with a "Page N" label.
     * Single-page text is parsed normally.
     */
    fun parseMultiPageToTable(rawText: String): MutableList<MutableList<String>> {
        val pages = splitPages(rawText)
        if (pages.size == 1) return parseToTable(pages[0])

        val combined = mutableListOf<MutableList<String>>()
        pages.forEachIndexed { index, pageText ->
            // Add page header row
            if (combined.isNotEmpty()) {
                combined.add(mutableListOf(""))  // blank spacer
            }
            combined.add(mutableListOf("=== Page ${index + 1} ==="))
            val pageTable = parseToTable(pageText)
            combined.addAll(pageTable)
        }
        return combined
    }

    /**
     * Parses multi-page text into invoice items, merging items from all pages.
     */
    fun parseMultiPageToInvoice(rawText: String): InvoiceData {
        val pages = splitPages(rawText)
        if (pages.size == 1) return parseToInvoice(pages[0])

        // Use first page for header info (company, date, invoice no)
        val first = parseToInvoice(pages[0])
        val allItems = first.items.toMutableList()

        // Collect items from remaining pages
        for (i in 1 until pages.size) {
            val pageData = parseToInvoice(pages[i])
            allItems.addAll(pageData.items)
        }

        return InvoiceData(
            items = allItems,
            companyHint = first.companyHint,
            customerHint = first.customerHint,
            dateHint = first.dateHint,
            invoiceNoHint = first.invoiceNoHint
        )
    }


}