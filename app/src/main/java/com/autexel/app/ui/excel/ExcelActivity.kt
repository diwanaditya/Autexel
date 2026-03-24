package com.autexel.app.ui.excel

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.autexel.app.R
import com.autexel.app.databinding.ActivityExcelBinding
import com.autexel.app.parser.TextParser
import com.autexel.app.utils.FileHelper
import com.autexel.app.utils.ReviewHelper
import com.autexel.app.utils.DataAnalyticsEngine
import com.autexel.app.utils.ShareHelper
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class ExcelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExcelBinding
    private val viewModel: ExcelViewModel by viewModels()
    private var lastSavedFile: File? = null
    private val cellViews: MutableList<MutableList<EditText>> = mutableListOf()
    private val CELL_WIDTH_DP  = 120
    private val CELL_HEIGHT_DP = 44

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExcelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.navigationContentDescription = "Navigate up"

        setupAccessibility()

        // On fresh launch, load extracted text
        if (savedInstanceState == null) {
            val rawText = intent.getStringExtra("EXTRACTED_TEXT") ?: ""
            if (rawText.isNotEmpty()) {
                viewModel.setRawText(rawText)
                viewModel.parseText(rawText)
                val pageCount = TextParser.splitPages(rawText).size
                if (pageCount > 1) {
                    Toast.makeText(this,
                        "$pageCount pages detected - all combined into table",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.etRawText.setText(viewModel.rawText.value)

        // Observe table data
        lifecycleScope.launch {
            viewModel.tableData.collect { data ->
                if (data.isNotEmpty()) renderTable(data)
            }
        }

        // Observe export state
        lifecycleScope.launch {
            viewModel.exportState.collect { state ->
                when (state) {
                    is ExcelViewModel.ExportState.Loading -> {
                        binding.btnExport.isEnabled = false
                        binding.btnExport.text = "Exporting..."
                    }
                    is ExcelViewModel.ExportState.Success -> {
                        binding.btnExport.isEnabled = true
                        binding.btnExport.text = "Export Excel"
                        lastSavedFile = state.file
                        binding.btnOpenFile.visibility = View.VISIBLE
                        binding.btnShare.visibility = View.VISIBLE
                        Toast.makeText(this@ExcelActivity,
                            "Saved: ${state.file.name}", Toast.LENGTH_LONG).show()
                        ReviewHelper.onExportSuccess(this@ExcelActivity)
                        viewModel.resetExportState()
                    }
                    is ExcelViewModel.ExportState.Error -> {
                        binding.btnExport.isEnabled = true
                        binding.btnExport.text = "Export Excel"
                        Toast.makeText(this@ExcelActivity, state.message, Toast.LENGTH_SHORT).show()
                        viewModel.resetExportState()
                    }
                    else -> {
                        binding.btnExport.isEnabled = true
                        binding.btnExport.text = "Export Excel"
                    }
                }
            }
        }

        binding.btnParse.setOnClickListener {
            val raw = binding.etRawText.text.toString()
            viewModel.setRawText(raw)
            viewModel.parseText(raw)
            updateUndoRedoButtons()
        }
        binding.btnAddRow.setOnClickListener    { syncAndUpdate { viewModel.addRow() } }
        binding.btnAddColumn.setOnClickListener { syncAndUpdate { viewModel.addColumn() } }
        binding.btnDeleteRow.setOnClickListener {
            if (viewModel.tableData.value.size <= 1) {
                Toast.makeText(this, "At least one row is required", Toast.LENGTH_SHORT).show()
            } else {
                syncAndUpdate { viewModel.deleteLastRow() }
            }
        }
        binding.btnExport.setOnClickListener {
            syncToViewModel()
            viewModel.exportExcel(viewModel.tableData.value)
        }
        binding.btnOpenFile.setOnClickListener {
            lastSavedFile?.let { FileHelper.openFile(this, it) }
        }
        binding.btnUndo.setOnClickListener {
            syncToViewModel()
            if (viewModel.undo()) {
                updateUndoRedoButtons()
                Toast.makeText(this, "Undone", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnRedo.setOnClickListener {
            if (viewModel.redo()) {
                updateUndoRedoButtons()
                Toast.makeText(this, "Redone", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnShare.setOnClickListener {
            lastSavedFile?.let {
                ShareHelper.shareFile(this, it, "Autexel Excel Export")
            }
        }
        binding.btnAnalyse.setOnClickListener {
            syncToViewModel()
            runAnalytics()
        }
    }

    private fun runAnalytics() {
        val table = viewModel.tableData.value
        if (table.isEmpty()) {
            Toast.makeText(this, "No data to analyse", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val result = withContext(Dispatchers.Default) {
                DataAnalyticsEngine.analyse(table)
            }
            showAnalyticsDialog(result)
        }
    }

    private fun showAnalyticsDialog(result: com.autexel.app.utils.DataAnalyticsEngine.AnalysisResult) {
        val scrollView = android.widget.ScrollView(this)
        val tv = android.widget.TextView(this).apply {
            text = result.summary
            textSize = 13f
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(32, 24, 32, 24)
            setTextColor(android.graphics.Color.parseColor("#212121"))
        }
        scrollView.addView(tv)

        android.app.AlertDialog.Builder(this)
            .setTitle("Data Analysis (${result.rowCount} rows x ${result.colCount} cols)")
            .setView(scrollView)
            .setPositiveButton("Close", null)
            .setNeutralButton("Sort by Col A") { _, _ ->
                val sorted = DataAnalyticsEngine.sortBy(
                    viewModel.tableData.value.map { it.toMutableList() }.toMutableList(), 1, true
                )
                viewModel.updateTable(sorted)
                Toast.makeText(this, "Table sorted by column A", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    // ── Accessibility ────────────────────────────────────────────────────────

    private fun setupAccessibility() {
        binding.btnParse.contentDescription     = "Parse extracted text into table"
        binding.btnAddRow.contentDescription    = "Add a new row to the table"
        binding.btnAddColumn.contentDescription = "Add a new column to the table"
        binding.btnDeleteRow.contentDescription = "Delete the last row from the table"
        binding.btnExport.contentDescription    = "Export table as Excel file"
        binding.btnOpenFile.contentDescription  = "Open the saved Excel file"
        binding.btnShare.contentDescription = "Share the saved Excel file"
        binding.btnUndo.contentDescription  = "Undo last table change"
        binding.btnRedo.contentDescription  = "Redo last undone change"
        binding.btnAnalyse.contentDescription  = "Analyse table data and get statistics"
    }

    // ── Table sync ───────────────────────────────────────────────────────────

    private fun syncAndUpdate(action: () -> Unit) {
        syncToViewModel()
        action()
        updateUndoRedoButtons()
    }

    private fun updateUndoRedoButtons() {
        binding.btnUndo.isEnabled = viewModel.canUndo()
        binding.btnRedo.isEnabled = viewModel.canRedo()
    }

    private fun syncToViewModel() {
        val current = viewModel.tableData.value.map { it.toMutableList() }.toMutableList()
        cellViews.forEachIndexed { rIdx, row ->
            row.forEachIndexed { cIdx, et ->
                if (rIdx < current.size && cIdx < current[rIdx].size) {
                    current[rIdx][cIdx] = et.text.toString()
                }
            }
        }
        viewModel.updateTable(current)
    }

    // ── Table rendering ──────────────────────────────────────────────────────

    private fun renderTable(data: MutableList<MutableList<String>>) {
        binding.tableContainer.removeAllViews()
        cellViews.clear()

        val dp       = resources.displayMetrics.density
        val cellW    = (CELL_WIDTH_DP * dp).toInt()
        val cellH    = (CELL_HEIGHT_DP * dp).toInt()
        val colCount = if (data.isNotEmpty()) data[0].size else 0

        // Column header row (A, B, C...)
        val headerRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        headerRow.addView(TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(cellW / 2, cellH)
            setBackgroundColor(Color.parseColor("#E8EAED"))
        })
        for (col in 0 until colCount) {
            val colLabel = ('A' + col).toString()
            headerRow.addView(TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(cellW, cellH).apply { setMargins(2, 2, 2, 2) }
                text = colLabel
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#5F6368"))
                textSize = 12f
                setBackgroundColor(Color.parseColor("#E8EAED"))
                contentDescription = "Column $colLabel"
            })
        }
        binding.tableContainer.addView(headerRow)

        data.forEachIndexed { rowIdx, row ->
            val rowView = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

            // Row number label
            rowView.addView(TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(cellW / 2, cellH).apply { setMargins(2, 2, 2, 2) }
                text = if (rowIdx == 0) "#" else rowIdx.toString()
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#5F6368"))
                textSize = 12f
                setBackgroundColor(Color.parseColor("#E8EAED"))
                contentDescription = if (rowIdx == 0) "Header row" else "Row $rowIdx"
            })

            val rowCells = mutableListOf<EditText>()
            row.forEachIndexed { colIdx, cellValue ->
                val colLabel = ('A' + colIdx).toString()
                rowCells.add(EditText(this).apply {
                    setText(cellValue)
                    layoutParams = LinearLayout.LayoutParams(cellW, cellH).apply { setMargins(2, 2, 2, 2) }
                    setPadding(12, 8, 12, 8)
                    gravity = Gravity.CENTER_VERTICAL
                    textSize = 13f
                    maxLines = 2
                    isSingleLine = false
                    contentDescription = "Cell $colLabel${if (rowIdx == 0) " header" else rowIdx.toString()}"
                    when {
                        rowIdx == 0 -> {
                            setBackgroundColor(Color.parseColor("#1A73E8"))
                            setTextColor(Color.WHITE)
                            paint.isFakeBoldText = true
                        }
                        rowIdx % 2 == 0 -> {
                            setBackgroundColor(Color.parseColor("#F1F3F4"))
                            setTextColor(Color.parseColor("#212121"))
                        }
                        else -> {
                            setBackgroundResource(R.drawable.bg_input_cell)
                            setTextColor(Color.parseColor("#212121"))
                        }
                    }
                    val rI = rowIdx; val cI = colIdx
                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                        override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                        override fun afterTextChanged(s: Editable?) {
                            val current = viewModel.tableData.value
                            if (rI < current.size && cI < current[rI].size) {
                                current[rI][cI] = s.toString()
                            }
                        }
                    })
                    rowView.addView(this)
                })
            }
            cellViews.add(rowCells)
            binding.tableContainer.addView(rowView)
        }
    }
}
