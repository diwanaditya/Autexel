package com.autexel.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ShareHelper {

    fun shareFile(context: Context, file: File, subject: String = "") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mime = when {
                file.name.endsWith(".xlsx") ->
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                file.name.endsWith(".pdf") -> "application/pdf"
                else -> "*/*"
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                if (subject.isNotEmpty()) putExtra(Intent.EXTRA_SUBJECT, subject)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, "Share ${file.name}")
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareMultipleFiles(context: Context, files: List<File>) {
        if (files.isEmpty()) return
        if (files.size == 1) { shareFile(context, files[0]); return }

        try {
            val uris = ArrayList<Uri>()
            files.forEach { file ->
                uris.add(
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                )
            }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share files"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
