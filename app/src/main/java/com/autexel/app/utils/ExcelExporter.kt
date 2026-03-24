package com.autexel.app.utils

import android.content.Context
import com.autexel.app.utils.HistoryManager
import android.os.Environment
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExcelExporter {

 /**
 * Export a 2D list of strings to an .xlsx file in the Downloads folder.
 * Returns the saved File on success, null on failure.
 */
 fun export(context: Context, tableData: List<List<String>>, fileName: String? = null): File? {
 return try {
 val workbook: Workbook = XSSFWorkbook()
 val sheet = workbook.createSheet("Autexel Data")

 // ── Styles ──────────────────────────────────────
 val headerFont = workbook.createFont().apply {
 bold = true
 fontHeightInPoints = 12
 color = IndexedColors.WHITE.index
 }
 val headerStyle = workbook.createCellStyle().apply {
 setFont(headerFont)
 fillForegroundColor = IndexedColors.ROYAL_BLUE.index
 fillPattern = FillPatternType.SOLID_FOREGROUND
 alignment = HorizontalAlignment.CENTER
 borderBottom = BorderStyle.THIN
 borderRight = BorderStyle.THIN
 }

 val evenStyle = workbook.createCellStyle().apply {
 fillForegroundColor = IndexedColors.WHITE.index
 fillPattern = FillPatternType.SOLID_FOREGROUND
 borderBottom = BorderStyle.HAIR
 borderRight = BorderStyle.HAIR
 }
 val oddStyle = workbook.createCellStyle().apply {
 fillForegroundColor = IndexedColors.LIGHT_TURQUOISE.index
 fillPattern = FillPatternType.SOLID_FOREGROUND
 borderBottom = BorderStyle.HAIR
 borderRight = BorderStyle.HAIR
 }

 // ── Populate rows ────────────────────────────────
 var maxCols = 0
 tableData.forEachIndexed { rowIdx, rowData ->
 val row = sheet.createRow(rowIdx)
 rowData.forEachIndexed { colIdx, cellValue ->
 val cell = row.createCell(colIdx)
 // Try numeric first
 val numVal = cellValue.replace(",", ".").toDoubleOrNull()
 if (numVal != null) {
 cell.setCellValue(numVal)
 } else {
 cell.setCellValue(cellValue)
 }
 cell.cellStyle = when {
 rowIdx == 0 -> headerStyle
 rowIdx % 2 == 0 -> evenStyle
 else -> oddStyle
 }
 }
 if (rowData.size > maxCols) maxCols = rowData.size
 }

 // ── Auto-size columns ────────────────────────────
 for (col in 0 until maxCols) {
 sheet.autoSizeColumn(col)
 // Ensure minimum width
 if (sheet.getColumnWidth(col) < 2560) sheet.setColumnWidth(col, 2560)
 }

 // Freeze header row
 sheet.createFreezePane(0, 1)

 // ── Save file ────────────────────────────────────
 val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
 val name = fileName ?: "Autexel_$timestamp.xlsx"
 val file = getDownloadsFile(context, name)

 FileOutputStream(file).use { out -> workbook.write(out) }
 workbook.close()
 file
 } catch (e: Exception) {
 e.printStackTrace()
 null
 }
 }

 private fun getDownloadsFile(context: Context, name: String): File {
 // Try public Downloads first (Android 9 and below need WRITE_EXTERNAL_STORAGE)
 val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
 if (downloadsDir.exists() || downloadsDir.mkdirs()) {
 return File(downloadsDir, name)
 }
 // Fallback to app-specific external Downloads
 val appDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
 ?: context.filesDir
 appDir.mkdirs()
 return File(appDir, name)
 }
}
