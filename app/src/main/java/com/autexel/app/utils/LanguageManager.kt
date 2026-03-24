package com.autexel.app.utils

import android.content.Context
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.CountDownLatch

/**
 * LanguageManager
 *
 * Provides:
 *  - Multi-script OCR recognizers for all major Indian scripts + Urdu + Latin
 *  - Automatic language detection from recognized text
 *  - On-device translation to English (no API key, no internet)
 *  - Script selector UI data
 *
 * Supported scripts for OCR:
 *  Latin    : English, Hindi (romanized), Urdu (romanized)
 *  Devanagari: Hindi, Marathi, Sanskrit, Nepali, Konkani, Dogri
 *  (Chinese, Japanese, Korean via separate ML Kit models - for numeric tables)
 *
 * Supported translation targets:
 *  Hindi, Bengali, Gujarati, Punjabi, Tamil, Telugu, Kannada, Malayalam,
 *  Marathi, Urdu, Odia, Assamese → English
 *
 * All models run 100% on-device after first download.
 */
object LanguageManager {

    // ── Script definitions ────────────────────────────────────────────────────

    data class ScriptOption(
        val id: String,
        val displayName: String,
        val nativeName: String,
        val mlkitTag: String,          // ML Kit TranslateLanguage tag
        val usesDevanagari: Boolean = false
    )

    val supportedScripts = listOf(
        ScriptOption("en",  "English",    "English",    TranslateLanguage.ENGLISH),
        ScriptOption("hi",  "Hindi",      "हिन्दी",      TranslateLanguage.HINDI,     usesDevanagari = true),
        ScriptOption("mr",  "Marathi",    "मराठी",       TranslateLanguage.MARATHI,   usesDevanagari = true),
        ScriptOption("bn",  "Bengali",    "বাংলা",       TranslateLanguage.BENGALI),
        ScriptOption("gu",  "Gujarati",   "ગુજરાતી",     TranslateLanguage.GUJARATI),
        ScriptOption("pa",  "Punjabi",    "ਪੰਜਾਬੀ",      TranslateLanguage.PUNJABI),
        ScriptOption("ta",  "Tamil",      "தமிழ்",       TranslateLanguage.TAMIL),
        ScriptOption("te",  "Telugu",     "తెలుగు",      TranslateLanguage.TELUGU),
        ScriptOption("kn",  "Kannada",    "ಕನ್ನಡ",       TranslateLanguage.KANNADA),
        ScriptOption("ml",  "Malayalam",  "മലയാളം",      TranslateLanguage.MALAYALAM),
        ScriptOption("ur",  "Urdu",       "اردو",        TranslateLanguage.URDU),
        ScriptOption("or",  "Odia",       "ଓଡ଼ିଆ",       TranslateLanguage.ODIA),
        ScriptOption("as",  "Assamese",   "অসমীয়া",     TranslateLanguage.ASSAMESE),
        ScriptOption("ne",  "Nepali",     "नेपाली",      TranslateLanguage.NEPALI,   usesDevanagari = true),
        ScriptOption("sa",  "Sanskrit",   "संस्कृत",     TranslateLanguage.HINDI,    usesDevanagari = true),
        ScriptOption("kok", "Konkani",    "कोंकणी",      TranslateLanguage.HINDI,    usesDevanagari = true),
        ScriptOption("auto","Auto-Detect","Auto",        TranslateLanguage.ENGLISH)
    )

    // Default = Auto-Detect
    private const val PREF_NAME      = "autexel_prefs"
    private const val KEY_SCRIPT     = "selected_script"
    private const val DEFAULT_SCRIPT = "auto"

    fun getSelectedScript(context: Context): ScriptOption {
        val id = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SCRIPT, DEFAULT_SCRIPT) ?: DEFAULT_SCRIPT
        return supportedScripts.find { it.id == id } ?: supportedScripts.last()
    }

    fun setSelectedScript(context: Context, scriptId: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_SCRIPT, scriptId).apply()
    }

    // ── OCR Recognizer factory ────────────────────────────────────────────────

    /**
     * Returns the best TextRecognizer for the given script.
     * Devanagari scripts use the dedicated recognizer for much higher accuracy.
     * All others fall back to Latin (which handles romanized Indian text well).
     */
    fun getRecognizer(scriptId: String): TextRecognizer {
        val script = supportedScripts.find { it.id == scriptId }
        return if (script?.usesDevanagari == true) {
            TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
        } else {
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        }
    }

    /**
     * For Auto-Detect mode: runs BOTH Latin and Devanagari recognizers and
     * picks the result with more text. This gives the highest accuracy without
     * requiring the user to select a script.
     */
    fun getBothRecognizers(): Pair<TextRecognizer, TextRecognizer> = Pair(
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
        TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
    )

    // ── Language Detection ────────────────────────────────────────────────────

    /**
     * Detect the language of a text string.
     * Returns BCP-47 language tag (e.g. "hi", "en", "ur") or "und" if unknown.
     * Runs on-device via ML Kit Language ID.
     */
    fun detectLanguage(text: String, onResult: (String) -> Unit) {
        if (text.isBlank()) { onResult("und"); return }
        LanguageIdentification.getClient().identifyLanguage(text)
            .addOnSuccessListener { lang -> onResult(lang) }
            .addOnFailureListener { onResult("und") }
    }

    fun detectLanguageSync(text: String): String {
        if (text.isBlank()) return "und"
        var result = "und"
        val latch = CountDownLatch(1)
        LanguageIdentification.getClient().identifyLanguage(text)
            .addOnSuccessListener { lang -> result = lang; latch.countDown() }
            .addOnFailureListener { latch.countDown() }
        latch.await()
        return result
    }

    /**
     * Returns the human-readable display name of a detected BCP-47 language tag.
     */
    fun getLanguageDisplayName(tag: String): String {
        return supportedScripts.find { it.id == tag }?.displayName
            ?: when (tag) {
                "en" -> "English"
                "hi" -> "Hindi"
                "mr" -> "Marathi"
                "bn" -> "Bengali"
                "gu" -> "Gujarati"
                "pa" -> "Punjabi"
                "ta" -> "Tamil"
                "te" -> "Telugu"
                "kn" -> "Kannada"
                "ml" -> "Malayalam"
                "ur" -> "Urdu"
                "or" -> "Odia"
                "as" -> "Assamese"
                "ne" -> "Nepali"
                "und" -> "Unknown"
                else -> tag.uppercase()
            }
    }

    // ── Translation ───────────────────────────────────────────────────────────

    /**
     * Translate text to English on-device using ML Kit.
     *
     * First call with a new language pair downloads the model (~30MB).
     * Subsequent calls use the cached model and work offline.
     *
     * @param text         Source text to translate
     * @param sourceLang   BCP-47 source language tag (e.g. "hi", "ur")
     *                     Pass "auto" to auto-detect first
     * @param onProgress   Called with status messages during model download
     * @param onResult     Called with (translatedText, detectedSourceLang)
     * @param onError      Called if translation fails
     */
    fun translateToEnglish(
        text: String,
        sourceLang: String = "auto",
        onProgress: (String) -> Unit = {},
        onResult: (translated: String, detectedLang: String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (text.isBlank()) { onResult(text, "en"); return }

        val doTranslate = { lang: String ->
            val mlLang = supportedScripts.find { it.id == lang }?.mlkitTag
                ?: TranslateLanguage.ENGLISH

            if (mlLang == TranslateLanguage.ENGLISH) {
                // Already English
                onResult(text, "en")
                return@doTranslate
            }

            val options = TranslatorOptions.Builder()
                .setSourceLanguage(mlLang)
                .setTargetLanguage(TranslateLanguage.ENGLISH)
                .build()

            val translator = Translation.getClient(options)
            onProgress("Preparing translation model for ${getLanguageDisplayName(lang)}...")

            translator.downloadModelIfNeeded()
                .addOnSuccessListener {
                    onProgress("Translating...")
                    translator.translate(text)
                        .addOnSuccessListener { translated ->
                            translator.close()
                            onResult(translated, lang)
                        }
                        .addOnFailureListener { e ->
                            translator.close()
                            onError("Translation failed: ${e.message}")
                        }
                }
                .addOnFailureListener { e ->
                    translator.close()
                    onError("Model download failed. Check internet connection once to download the model (${e.message})")
                }
        }

        if (sourceLang == "auto") {
            onProgress("Detecting language...")
            detectLanguage(text) { detected ->
                doTranslate(if (detected == "und") "en" else detected)
            }
        } else {
            doTranslate(sourceLang)
        }
    }

    /**
     * Check if translation model for a language is already downloaded.
     */
    fun isModelDownloaded(scriptId: String, onResult: (Boolean) -> Unit) {
        val mlLang = supportedScripts.find { it.id == scriptId }?.mlkitTag
            ?: TranslateLanguage.ENGLISH
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(mlLang)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
        val translator = Translation.getClient(options)
        translator.downloadModelIfNeeded()
            .addOnSuccessListener { onResult(true); translator.close() }
            .addOnFailureListener { onResult(false); translator.close() }
    }
}
