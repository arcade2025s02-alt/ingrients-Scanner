package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OcrExtractionResult(
    val extractedIngredients: String,
    val fullRawText: String,
    val detectedProductName: String?,
    val blockCount: Int
)

/**
 * 100% Free, On-Device ML Kit Text Recognition (OCR) engine.
 * Reads ingredient lists, INS/E-numbers, nutrition values, fertilizer NPK compositions,
 * and packaging resin codes directly from camera captures or uploaded gallery photos.
 */
object LabelOcrEngine {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun extractFromImage(
        context: Context,
        imageUri: Uri?,
        fallbackBitmap: Bitmap?
    ): Result<OcrExtractionResult> = withContext(Dispatchers.IO) {
        try {
            val inputImage: InputImage = when {
                imageUri != null -> {
                    try {
                        // Automatically respects EXIF orientation for both camera and gallery photos
                        InputImage.fromFilePath(context.applicationContext, imageUri)
                    } catch (_: Exception) {
                        val bmp = fallbackBitmap
                            ?: return@withContext Result.failure(IllegalArgumentException("Could not read image for OCR."))
                        InputImage.fromBitmap(bmp, 0)
                    }
                }
                fallbackBitmap != null -> InputImage.fromBitmap(fallbackBitmap, 0)
                else -> return@withContext Result.failure(IllegalArgumentException("No image provided for OCR."))
            }

            val visionText = recognizeAsync(inputImage)
            val orderedText = buildOrderedText(visionText)
            if (orderedText.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("No readable text found in this photo. Try a closer, sharper shot of the ingredient list.")
                )
            }

            val parsedIngredients = extractIngredientsAndComposition(orderedText)
            val detectedTitle = guessProductTitle(orderedText)

            Result.success(
                OcrExtractionResult(
                    extractedIngredients = parsedIngredients,
                    fullRawText = orderedText,
                    detectedProductName = detectedTitle,
                    blockCount = visionText.textBlocks.size
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun recognizeAsync(inputImage: InputImage): Text =
        suspendCancellableCoroutine { continuation ->
            recognizer.process(inputImage)
                .addOnSuccessListener { result ->
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                }
        }

    private fun buildOrderedText(visionText: Text): String {
        val sortedBlocks = visionText.textBlocks.sortedWith(
            compareBy<Text.TextBlock> { it.boundingBox?.top ?: Int.MAX_VALUE }
                .thenBy { it.boundingBox?.left ?: Int.MAX_VALUE }
        )
        return sortedBlocks.joinToString("\n") { block ->
            block.lines
                .sortedBy { it.boundingBox?.top ?: Int.MAX_VALUE }
                .joinToString(" ") { it.text.trim() }
                .replace(Regex("\\s+"), " ")
                .trim()
        }.trim()
    }

    /**
     * Intelligently isolates the Ingredients / Composition section plus any key Nutrition / NPK / Resin
     * details so the scanned field is clean and ready for risk scoring.
     * Falls back to the full cleaned OCR text when no explicit "Ingredients" heading appears.
     */
    private fun extractIngredientsAndComposition(fullText: String): String {
        val cleaned = fullText
            .replace("—", "-")
            .replace("•", ",")
            .replace("|", ",")
            .replace(Regex("[ \\t]+"), " ")
            .trim()

        val headerRegex = Regex(
            "(?i)(ingredients?|composition|contains|active ingredients?|material composition|guaranteed analysis)\\s*[:\\-]?"
        )
        val headerMatch = headerRegex.find(cleaned)

        val primarySection = if (headerMatch != null) {
            val afterHeader = cleaned.substring(headerMatch.range.first).trim()
            val stopRegex = Regex(
                "(?i)\\n\\s*(manufactured by|marketed by|packed by|best before|use by|mfg\\.?\\s*date|exp\\.?\\s*date|net\\s*weight|mrp|fssai\\s*lic|customer care|storage instructions|directions for use)"
            )
            val stopMatch = stopRegex.find(afterHeader)
            val sliced = if (stopMatch != null && stopMatch.range.first > 25) {
                afterHeader.substring(0, stopMatch.range.first).trim()
            } else {
                afterHeader
            }
            sliced.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
        } else {
            cleaned.replace("\n", ", ").replace(Regex(",\\s*,"), ",").replace(Regex("\\s+"), " ").trim()
        }

        // Also preserve key nutrition or chemical metrics if they appeared outside the sliced ingredients block
        val extraMetrics = mutableListOf<String>()
        val metricPatterns = listOf(
            Regex("(?i)(energy|calories)\\s*[:\\-]?\\s*\\d+(?:\\.\\d+)?\\s*(?:kcal|cal)"),
            Regex("(?i)(total\\s+sugars?|added\\s+sugars?|sugar)\\s*[:\\-]?\\s*\\d+(?:\\.\\d+)?\\s*g"),
            Regex("(?i)(sodium|salt)\\s*[:\\-]?\\s*\\d+(?:\\.\\d+)?\\s*(?:mg|g)"),
            Regex("(?i)(protein)\\s*[:\\-]?\\s*\\d+(?:\\.\\d+)?\\s*g"),
            Regex("(?i)(dietary\\s+fiber|fibre|fiber)\\s*[:\\-]?\\s*\\d+(?:\\.\\d+)?\\s*g"),
            Regex("(?i)(npk\\s*\\d+[-:]\\d+[-:]\\d+)"),
            Regex("(?i)(resin\\s*code\\s*#?\\d|pet|hdpe|pvc|ldpe|pp|ps|bpa|pfas)")
        )
        metricPatterns.forEach { regex ->
            val match = regex.find(cleaned)?.value?.trim()
            if (!match.isNullOrBlank() && !primarySection.contains(match, ignoreCase = true)) {
                extraMetrics.add(match)
            }
        }

        return if (extraMetrics.isNotEmpty()) {
            "$primarySection. ${extraMetrics.joinToString(", ")}."
        } else {
            primarySection
        }
    }

    private fun guessProductTitle(fullText: String): String? {
        val lines = fullText.lines().map { it.trim() }.filter { it.length in 3..48 }
        val skipPrefixes = listOf(
            "ingredient", "nutrition", "composition", "contains", "allergen",
            "energy", "protein", "sugar", "sodium", "net wt", "mrp", "mfg", "exp", "fssai", "best before"
        )
        return lines.firstOrNull { line ->
            val lower = line.lowercase()
            skipPrefixes.none { lower.startsWith(it) } &&
                line.count { it.isLetter() } >= 3 &&
                !line.contains(",")
        }
    }
}
