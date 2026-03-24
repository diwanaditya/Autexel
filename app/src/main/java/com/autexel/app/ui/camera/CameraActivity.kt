package com.autexel.app.ui.camera

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.autexel.app.R
import com.autexel.app.databinding.ActivityCameraBinding
import com.autexel.app.ui.excel.ExcelActivity
import com.autexel.app.ui.home.HomeActivity
import com.autexel.app.ui.invoice.InvoiceActivity
import com.autexel.app.utils.LanguageManager
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var scanMode: String = HomeActivity.MODE_EXCEL
    private var isProcessing = false
    private val scannedPages = mutableListOf<String>()

    private var selectedScriptId = "auto"

    companion object {
        private const val TAG = "CameraActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val KEY_SCAN_MODE    = "scan_mode"
        private const val KEY_IS_PROCESSING = "is_processing"
        private const val KEY_PAGES        = "scanned_pages"
        private const val KEY_SCRIPT       = "selected_script"
        const val MAX_PAGES = 20
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> processGalleryImage(uri) }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scanMode = savedInstanceState?.getString(KEY_SCAN_MODE)
            ?: intent.getStringExtra(HomeActivity.EXTRA_MODE)
            ?: HomeActivity.MODE_EXCEL

        selectedScriptId = savedInstanceState?.getString(KEY_SCRIPT)
            ?: LanguageManager.getSelectedScript(this).id

        isProcessing = savedInstanceState?.getBoolean(KEY_IS_PROCESSING, false) ?: false
        if (isProcessing) showLoading(true, "Processing...")

        savedInstanceState?.getStringArrayList(KEY_PAGES)?.let { saved ->
            scannedPages.addAll(saved)
            refreshPageStrip()
            updatePageUI()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        setupScriptSpinner()
        setupAccessibility()
        setupClickListeners()

        if (allPermissionsGranted()) startCamera()
        else ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SCAN_MODE, scanMode)
        outState.putBoolean(KEY_IS_PROCESSING, isProcessing)
        outState.putStringArrayList(KEY_PAGES, ArrayList(scannedPages))
        outState.putString(KEY_SCRIPT, selectedScriptId)
    }

    override fun onDestroy() { super.onDestroy(); cameraExecutor.shutdown() }

    // ── Script Spinner ────────────────────────────────────────────────────────

    private fun setupScriptSpinner() {
        val scripts = LanguageManager.supportedScripts
        val labels  = scripts.map {
            if (it.id == "auto") "Auto-Detect"
            else "${it.displayName} (${it.nativeName})"
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerScript.adapter = adapter

        val selectedIdx = scripts.indexOfFirst { it.id == selectedScriptId }.coerceAtLeast(0)
        binding.spinnerScript.setSelection(selectedIdx, false)

        binding.spinnerScript.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    selectedScriptId = scripts[pos].id
                    LanguageManager.setSelectedScript(this@CameraActivity, selectedScriptId)
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
    }

    // ── Accessibility ─────────────────────────────────────────────────────────

    private fun setupAccessibility() {
        binding.btnCapture.contentDescription  = "Capture this page"
        binding.btnBack.contentDescription     = "Cancel scanning and go back"
        binding.btnGallery.contentDescription  = "Add image from gallery"
        binding.btnDone.contentDescription     = "Finish scanning and proceed to editor"
        binding.previewView.contentDescription = "Camera preview. Point at handwritten text."
        binding.spinnerScript.contentDescription = "Select script language for text recognition"
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private fun setupClickListeners() {
        binding.btnCapture.setOnClickListener { captureAndRecognize() }
        binding.btnBack.setOnClickListener {
            if (scannedPages.isNotEmpty()) confirmDiscard() else finish()
        }
        binding.btnGallery.setOnClickListener { openGallery() }
        binding.btnDone.setOnClickListener { finishScanning() }
    }

    // ── Camera ────────────────────────────────────────────────────────────────

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            val preview  = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
                Toast.makeText(this, "Camera failed to start.", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // ── Capture ───────────────────────────────────────────────────────────────

    private fun captureAndRecognize() {
        val capture = imageCapture ?: return
        if (isProcessing) return
        if (scannedPages.size >= MAX_PAGES) {
            Toast.makeText(this, "Maximum $MAX_PAGES pages reached. Tap Done.", Toast.LENGTH_SHORT).show()
            return
        }
        showLoading(true, "Scanning page ${scannedPages.size + 1}...")
        isProcessing = true

        capture.takePicture(ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) { processImageProxy(imageProxy) }
                override fun onError(e: ImageCaptureException) {
                    resetButtons()
                    Toast.makeText(this@CameraActivity, "Capture failed. Try again.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun processImageProxy(imageProxy: ImageProxy) {
        val media = imageProxy.image
        if (media == null) { imageProxy.close(); resetButtons(); return }
        val image = InputImage.fromMediaImage(media, imageProxy.imageInfo.rotationDegrees)
        imageProxy.close()
        lifecycleScope.launch { recognizeAndAddPage(image) }
    }

    private fun openGallery() {
        if (scannedPages.size >= MAX_PAGES) {
            Toast.makeText(this, "Maximum $MAX_PAGES pages reached.", Toast.LENGTH_SHORT).show()
            return
        }
        galleryLauncher.launch(
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply { type = "image/*" }
        )
    }

    private fun processGalleryImage(uri: Uri) {
        showLoading(true, "Reading image...")
        isProcessing = true
        lifecycleScope.launch {
            try {
                val image = InputImage.fromFilePath(this@CameraActivity, uri)
                recognizeAndAddPage(image)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { resetButtons(); Toast.makeText(this@CameraActivity, "Could not read image.", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    // ── OCR with multi-script support ─────────────────────────────────────────

    private suspend fun recognizeAndAddPage(image: InputImage) {
        try {
            val text = withContext(Dispatchers.Default) {
                if (selectedScriptId == "auto") {
                    // Run both Latin and Devanagari, pick the longer result
                    val (latinRec, devanagariRec) = LanguageManager.getBothRecognizers()
                    var latinText = ""
                    var devText   = ""

                    val l1 = CountDownLatch(1)
                    latinRec.process(image)
                        .addOnSuccessListener { latinText = it.text.trim(); l1.countDown() }
                        .addOnFailureListener { l1.countDown() }
                    l1.await()

                    val l2 = CountDownLatch(1)
                    devanagariRec.process(image)
                        .addOnSuccessListener { devText = it.text.trim(); l2.countDown() }
                        .addOnFailureListener { l2.countDown() }
                    l2.await()

                    latinRec.close()
                    devanagariRec.close()

                    // Return the result with more content
                    if (devText.length > latinText.length * 1.2) devText else latinText
                } else {
                    // Use specific recognizer for selected script
                    var result = ""
                    val latch  = CountDownLatch(1)
                    LanguageManager.getRecognizer(selectedScriptId).process(image)
                        .addOnSuccessListener { result = it.text.trim(); latch.countDown() }
                        .addOnFailureListener { latch.countDown() }
                    latch.await()
                    result
                }
            }

            withContext(Dispatchers.Main) {
                resetButtons()
                if (text.isEmpty()) {
                    Toast.makeText(this@CameraActivity,
                        "No text detected. Ensure good lighting and hold steady.", Toast.LENGTH_LONG).show()
                    return@withContext
                }

                // Detect language and show badge
                LanguageManager.detectLanguage(text) { lang ->
                    if (lang != "und" && lang != "en") {
                        binding.tvDetectedLang.visibility = View.VISIBLE
                        binding.tvDetectedLang.text = LanguageManager.getLanguageDisplayName(lang)
                    }
                }

                scannedPages.add(text)
                addPageThumbnail(scannedPages.size)
                updatePageUI()

                val msg = if (scannedPages.size == 1)
                    "Page 1 captured. Tap Capture for more or Done to finish."
                else "Page ${scannedPages.size} captured."
                Toast.makeText(this@CameraActivity, msg, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                resetButtons()
                Toast.makeText(this@CameraActivity, "Recognition failed. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Page Strip ────────────────────────────────────────────────────────────

    private fun addPageThumbnail(pageNumber: Int) {
        val thumb = LayoutInflater.from(this)
            .inflate(R.layout.item_page_thumbnail, binding.pageStrip, false) as FrameLayout
        thumb.findViewById<TextView>(R.id.tvPageNumber).text = pageNumber.toString()
        thumb.contentDescription = "Page $pageNumber"
        thumb.findViewById<TextView>(R.id.btnRemovePage).setOnClickListener {
            removePage(pageNumber - 1)
        }
        binding.pageStrip.addView(thumb)
        binding.pageStripContainer.post { binding.pageStripContainer.fullScroll(View.FOCUS_RIGHT) }
    }

    private fun removePage(index: Int) {
        if (index < 0 || index >= scannedPages.size) return
        scannedPages.removeAt(index)
        refreshPageStrip()
        updatePageUI()
        Toast.makeText(this, "Page ${index + 1} removed.", Toast.LENGTH_SHORT).show()
    }

    private fun refreshPageStrip() {
        binding.pageStrip.removeAllViews()
        scannedPages.forEachIndexed { i, _ -> addPageThumbnail(i + 1) }
    }

    private fun updatePageUI() {
        val count    = scannedPages.size
        val hasPages = count > 0
        binding.pageStripContainer.visibility = if (hasPages) View.VISIBLE else View.GONE
        binding.tvPageCount.visibility = if (hasPages) View.VISIBLE else View.GONE
        binding.tvPageCount.text = if (count == 1) "1 page" else "$count pages"
        binding.btnDone.isEnabled = hasPages
        binding.btnCapture.text = if (hasPages) "Capture Another Page" else "Capture Page"
        binding.tvHint.text = if (!hasPages)
            "Align text within frame, then tap Capture"
        else "Tap Capture for more pages, or Done to process all $count pages"
    }

    // ── Finish & Translate ────────────────────────────────────────────────────

    private fun finishScanning() {
        if (scannedPages.isEmpty()) {
            Toast.makeText(this, "Scan at least one page first.", Toast.LENGTH_SHORT).show()
            return
        }

        val combined = scannedPages.joinToString(separator = "\n\n--- Page Break ---\n\n")

        // Check if non-English text detected — offer translation
        LanguageManager.detectLanguage(combined) { lang ->
            runOnUiThread {
                if (lang != "und" && lang != "en") {
                    showTranslateDialog(combined, lang)
                } else {
                    navigateToEditor(combined)
                }
            }
        }
    }

    private fun showTranslateDialog(text: String, detectedLang: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_translate, null)
        val langName   = LanguageManager.getLanguageDisplayName(detectedLang)

        val tvSource   = dialogView.findViewById<TextView>(R.id.tvSourceText)
        val tvStatus   = dialogView.findViewById<TextView>(R.id.tvTranslateStatus)
        val tvTransLabel = dialogView.findViewById<TextView>(R.id.tvTranslatedLabel)
        val tilTrans   = dialogView.findViewById<View>(R.id.tilTranslated)
        val etTrans    = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etTranslatedText)
        val btnRow     = dialogView.findViewById<View>(R.id.btnRowTranslate)
        val btnTrans   = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnTranslate)
        val spinnerLang = dialogView.findViewById<Spinner>(R.id.spinnerSourceLang)

        // Show first 200 chars as preview
        tvSource.text = if (text.length > 200) text.substring(0, 200) + "..." else text

        // Populate language spinner
        val scripts   = LanguageManager.supportedScripts.filter { it.id != "auto" }
        val langNames = scripts.map { "${it.displayName} (${it.nativeName})" }
        val langAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, langNames)
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLang.adapter = langAdapter
        val preselect = scripts.indexOfFirst { it.id == detectedLang }.coerceAtLeast(0)
        spinnerLang.setSelection(preselect)

        val dialog = AlertDialog.Builder(this)
            .setTitle("$langName text detected")
            .setView(dialogView)
            .setCancelable(false)
            .setNegativeButton("Proceed Without Translating") { _, _ -> navigateToEditor(text) }
            .create()

        btnTrans.setOnClickListener {
            val selScript = scripts[spinnerLang.selectedItemPosition]
            tvStatus.visibility  = View.VISIBLE
            tvStatus.text        = "Starting translation..."
            btnTrans.isEnabled   = false

            LanguageManager.translateToEnglish(
                text        = text,
                sourceLang  = selScript.id,
                onProgress  = { msg -> runOnUiThread { tvStatus.text = msg } },
                onResult    = { translated, _ ->
                    runOnUiThread {
                        tvStatus.text    = "Translation complete"
                        tvTransLabel.visibility = View.VISIBLE
                        tilTrans.visibility = View.VISIBLE
                        btnRow.visibility   = View.VISIBLE
                        btnTrans.isEnabled  = true
                        etTrans.setText(translated)
                    }
                },
                onError = { err ->
                    runOnUiThread {
                        tvStatus.text   = err
                        btnTrans.isEnabled = true
                    }
                }
            )
        }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnUseTranslated).setOnClickListener {
            dialog.dismiss()
            navigateToEditor(etTrans.text.toString().ifEmpty { text })
        }
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnUseOriginal).setOnClickListener {
            dialog.dismiss()
            navigateToEditor(text)
        }

        dialog.show()
    }

    private fun navigateToEditor(extractedText: String) {
        val intent = when (scanMode) {
            HomeActivity.MODE_INVOICE -> Intent(this, InvoiceActivity::class.java)
            else                      -> Intent(this, ExcelActivity::class.java)
        }
        intent.putExtra("EXTRACTED_TEXT", extractedText)
        startActivity(intent)
    }

    private fun confirmDiscard() {
        AlertDialog.Builder(this)
            .setTitle("Discard scanned pages?")
            .setMessage("You have ${scannedPages.size} page(s). Going back will discard them.")
            .setPositiveButton("Discard") { _, _ -> finish() }
            .setNegativeButton("Keep Scanning", null)
            .show()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun resetButtons() {
        isProcessing = false
        showLoading(false, "")
        binding.btnCapture.isEnabled = true
        binding.btnGallery.isEnabled = true
    }

    private fun showLoading(show: Boolean, message: String) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        if (message.isNotEmpty()) binding.tvLoadingMsg.text = message
        if (show) { binding.btnCapture.isEnabled = false; binding.btnGallery.isEnabled = false }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) startCamera()
            else { Toast.makeText(this, "Camera permission required.", Toast.LENGTH_LONG).show(); finish() }
        }
    }
}
