package com.autexel.app.ui.excel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.autexel.app.parser.TextParser
import com.autexel.app.utils.ExcelExporter
import com.autexel.app.utils.UndoRedoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ExcelViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    companion object {
        private const val KEY_TABLE = "table_data"
        private const val KEY_RAW = "raw_text"
    }

    // Table data survives rotation via SavedStateHandle
    private val undoRedo = UndoRedoManager<MutableList<MutableList<String>>>()

    private val _tableData = MutableStateFlow<MutableList<MutableList<String>>>(mutableListOf())
    val tableData: StateFlow<MutableList<MutableList<String>>> = _tableData

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState

    private val _rawText = MutableStateFlow(savedStateHandle.get<String>(KEY_RAW) ?: "")
    val rawText: StateFlow<String> = _rawText

    init {
        // Restore table on rotation
        savedStateHandle.get<ArrayList<ArrayList<String>>>(KEY_TABLE)?.let { saved ->
            _tableData.value = saved.map { it.toMutableList() }.toMutableList()
        }
    }

    fun setRawText(text: String) {
        _rawText.value = text
        savedStateHandle[KEY_RAW] = text
    }

    fun parseText(raw: String) {
        if (raw.isBlank()) return
        val corrected = TextParser.quickCorrect(raw)
        val parsed = TextParser.parseMultiPageToTable(corrected)
        updateTable(parsed)
    }

    fun updateTable(data: MutableList<MutableList<String>>, pushUndo: Boolean = true) {
        if (pushUndo) undoRedo.push(_tableData.value.map { it.toMutableList() }.toMutableList())
        _tableData.value = data
        savedStateHandle[KEY_TABLE] = ArrayList(data.map { ArrayList(it) })
    }

    fun addRow() {
        val current = _tableData.value.map { it.toMutableList() }.toMutableList()
        val cols = if (current.isNotEmpty()) current[0].size else 1
        current.add(MutableList(cols) { "" })
        updateTable(current)
    }

    fun addColumn() {
        val current = _tableData.value.map { it.toMutableList() }.toMutableList()
        if (current.isEmpty()) {
            current.add(mutableListOf(""))
        } else {
            current.forEach { row -> row.add("") }
        }
        updateTable(current)
    }

    fun deleteLastRow() {
        val current = _tableData.value.map { it.toMutableList() }.toMutableList()
        if (current.size > 1) {
            current.removeAt(current.size - 1)
            updateTable(current)
        }
    }

    fun exportExcel(data: List<List<String>>) {
        if (data.isEmpty()) return
        _exportState.value = ExportState.Loading
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                ExcelExporter.export(getApplication(), data)
            }
            _exportState.value = if (file != null) ExportState.Success(file) else ExportState.Error("Export failed")
        }
    }

    fun resetExportState() { _exportState.value = ExportState.Idle }

    fun undo(): Boolean {
        val prev = undoRedo.undo(_tableData.value.map { it.toMutableList() }.toMutableList()) ?: return false
        updateTable(prev, pushUndo = false)
        return true
    }

    fun redo(): Boolean {
        val next = undoRedo.redo(_tableData.value.map { it.toMutableList() }.toMutableList()) ?: return false
        updateTable(next, pushUndo = false)
        return true
    }

    fun canUndo() = undoRedo.canUndo()
    fun canRedo() = undoRedo.canRedo()

    sealed class ExportState {
        object Idle : ExportState()
        object Loading : ExportState()
        data class Success(val file: File) : ExportState()
        data class Error(val message: String) : ExportState()
    }
}
