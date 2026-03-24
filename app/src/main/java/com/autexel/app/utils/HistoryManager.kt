package com.autexel.app.utils

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * HistoryManager — tracks every file exported by the app.
 * Stored in app private storage as a JSON array.
 * Max 50 entries, oldest removed automatically.
 */
object HistoryManager {

    private const val FILE_NAME = "export_history.json"
    private const val MAX_ENTRIES = 50

    data class HistoryEntry(
        val id: String,
        val fileName: String,
        val filePath: String,
        val type: String,        // "excel" or "invoice"
        val dateCreated: String,
        val pageSources: Int,
        val language: String,
        val fileSizeKb: Long
    )

    fun addEntry(context: Context, file: File, type: String, pages: Int = 1, lang: String = "en") {
        val entries = loadAll(context).toMutableList()
        val entry = HistoryEntry(
            id          = UUID.randomUUID().toString(),
            fileName    = file.name,
            filePath    = file.absolutePath,
            type        = type,
            dateCreated = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date()),
            pageSources = pages,
            language    = lang,
            fileSizeKb  = if (file.exists()) file.length() / 1024 else 0L
        )
        entries.add(0, entry)
        val trimmed = if (entries.size > MAX_ENTRIES) entries.take(MAX_ENTRIES) else entries
        save(context, trimmed)
    }

    fun loadAll(context: Context): List<HistoryEntry> {
        return try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return emptyList()
            val json = JSONArray(file.readText())
            (0 until json.length()).map { i ->
                val obj = json.getJSONObject(i)
                HistoryEntry(
                    id          = obj.optString("id", UUID.randomUUID().toString()),
                    fileName    = obj.getString("fileName"),
                    filePath    = obj.getString("filePath"),
                    type        = obj.getString("type"),
                    dateCreated = obj.getString("dateCreated"),
                    pageSources = obj.optInt("pageSources", 1),
                    language    = obj.optString("language", "en"),
                    fileSizeKb  = obj.optLong("fileSizeKb", 0L)
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    fun deleteEntry(context: Context, id: String) {
        val entries = loadAll(context).filter { it.id != id }
        save(context, entries)
    }

    fun clearAll(context: Context) {
        File(context.filesDir, FILE_NAME).delete()
    }

    private fun save(context: Context, entries: List<HistoryEntry>) {
        val array = JSONArray()
        entries.forEach { e ->
            array.put(JSONObject().apply {
                put("id", e.id)
                put("fileName", e.fileName)
                put("filePath", e.filePath)
                put("type", e.type)
                put("dateCreated", e.dateCreated)
                put("pageSources", e.pageSources)
                put("language", e.language)
                put("fileSizeKb", e.fileSizeKb)
            })
        }
        File(context.filesDir, FILE_NAME).writeText(array.toString())
    }
}
