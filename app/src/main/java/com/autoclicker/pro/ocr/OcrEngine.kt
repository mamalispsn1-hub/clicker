package com.autoclicker.pro.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class OcrTextBlock(val text: String, val bounds: Rect)

data class OcrResult(val fullText: String, val blocks: List<OcrTextBlock>)

/**
 * Thin wrapper around ML Kit's on-device text recognizer.
 *
 * Known limitation, stated plainly rather than silently mis-detecting: the
 * bundled on-device recognizer only ships a Latin-script model. It does not
 * recognize Persian/Arabic script. This is a generic building block for
 * "wait until text appears in a region" conditions on Latin-script UI (menus,
 * numbers, English labels, etc.) — for non-Latin text, condition matching
 * will simply never find the text, which [isLikelyUnsupportedScript] can flag
 * up front so the UI can warn the user instead of it silently never firing.
 */
@Singleton
class OcrEngine @Inject constructor() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(bitmap: Bitmap, region: Rect? = null): OcrResult {
        val input = if (region != null && isValidCrop(bitmap, region)) {
            val cropped = Bitmap.createBitmap(bitmap, region.left, region.top, region.width(), region.height())
            InputImage.fromBitmap(cropped, 0)
        } else {
            InputImage.fromBitmap(bitmap, 0)
        }

        return suspendCancellableCoroutine { continuation ->
            recognizer.process(input)
                .addOnSuccessListener { visionText ->
                    val blocks = visionText.textBlocks.map { block ->
                        OcrTextBlock(text = block.text, bounds = block.boundingBox ?: Rect())
                    }
                    if (continuation.isActive) {
                        continuation.resume(OcrResult(fullText = visionText.text, blocks = blocks))
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(OcrResult("", emptyList()))
                }
        }
    }

    private fun isValidCrop(bitmap: Bitmap, region: Rect): Boolean {
        return region.left >= 0 && region.top >= 0 &&
            region.right <= bitmap.width && region.bottom <= bitmap.height &&
            region.width() > 0 && region.height() > 0
    }

    /** Rough heuristic (checks for the Arabic/Persian Unicode block) so the UI
     * can warn the user at config time rather than the condition silently
     * never matching at runtime. Not used to block anything — just a hint. */
    fun isLikelyUnsupportedScript(text: String): Boolean =
        text.any { it.code in 0x0600..0x06FF || it.code in 0xFB50..0xFDFF || it.code in 0xFE70..0xFEFF }
}
