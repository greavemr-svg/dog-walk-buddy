package io.github.greavemrsvg.dogwalkbuddy.data.repo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.math.roundToInt

/** Where the bytes of a picked photo come from. Opening can fail, which is reported as [PhotoPreparation.Unreadable]. */
fun interface PhotoSource {
    fun open(): InputStream?
}

/** A photo that passed the checks, downscaled and re-encoded as JPEG without EXIF, ready to store. */
class ProcessedPhoto(
    val jpegBytes: ByteArray,
    val bitmap: Bitmap,
)

sealed interface PhotoPreparation {
    data class Ready(
        val photo: ProcessedPhoto,
    ) : PhotoPreparation

    /** Over 20 MB, or over 8000 px on a side. */
    data object TooLarge : PhotoPreparation

    data object NotAnImage : PhotoPreparation

    data object Unreadable : PhotoPreparation
}

/**
 * The profile photo copy (ADR 0004). Picked images are bounds-checked, downscaled and re-encoded as JPEG, which
 * strips EXIF including GPS. Files get app-generated names inside `filesDir/profile/`, never a name or path from input.
 */
class PhotoStore(
    rootDir: File,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val nameGenerator: () -> String = { UUID.randomUUID().toString() },
) {
    private val directory = File(rootDir, DIRECTORY_NAME)

    suspend fun prepare(source: PhotoSource): PhotoPreparation =
        withContext(dispatcher) {
            val bytes = readLimited(source) ?: return@withContext PhotoPreparation.Unreadable
            if (bytes.size > MAX_SOURCE_BYTES) return@withContext PhotoPreparation.TooLarge
            val bounds = decodeBounds(bytes)
            when {
                bounds == null -> PhotoPreparation.NotAnImage
                maxOf(bounds.first, bounds.second) > MAX_SOURCE_EDGE_PX -> PhotoPreparation.TooLarge
                else -> encode(bytes)
            }
        }

    /** Writes [photo] under a new generated name and returns that name. */
    suspend fun store(photo: ProcessedPhoto): String =
        withContext(dispatcher) {
            check(directory.isDirectory || directory.mkdirs()) { "Cannot create the photo directory" }
            val fileName = "$FILE_PREFIX${nameGenerator()}$FILE_SUFFIX"
            val target = File(directory, fileName)
            // Written under a temporary name and renamed, so a failed write never leaves a partial photo behind.
            val partial = File(directory, "$fileName$PARTIAL_SUFFIX")
            try {
                partial.writeBytes(photo.jpegBytes)
                check(partial.renameTo(target)) { "Cannot move the photo into place" }
            } finally {
                partial.delete()
            }
            fileName
        }

    suspend fun load(fileName: String): Bitmap? =
        withContext(dispatcher) {
            resolve(fileName)?.takeIf { it.isFile }?.let { BitmapFactory.decodeFile(it.path) }
        }

    suspend fun delete(fileName: String) {
        withContext(dispatcher) { resolve(fileName)?.delete() }
    }

    // Only names this store generated resolve to a file, so a stored or supplied value cannot escape the directory.
    private fun resolve(fileName: String): File? = if (GENERATED_NAME.matches(fileName)) File(directory, fileName) else null

    private fun readLimited(source: PhotoSource): ByteArray? =
        try {
            source.open()?.use { stream ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(BUFFER_BYTES)
                while (output.size() <= MAX_SOURCE_BYTES) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }

    private fun decodeBounds(bytes: ByteArray): Pair<Int, Int>? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        return if (options.outWidth > 0 && options.outHeight > 0 && options.outMimeType != null) {
            options.outWidth to options.outHeight
        } else {
            null
        }
    }

    private fun encode(bytes: ByteArray): PhotoPreparation {
        // ImageDecoder applies the EXIF orientation to the pixels and scales while decoding, so a large photo is never
        // fully allocated. Re-encoding then drops EXIF, so the rotation must already be in the pixels.
        val decoded =
            try {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val longEdge = maxOf(info.size.width, info.size.height)
                    if (longEdge > MAX_STORED_EDGE_PX) {
                        val scale = MAX_STORED_EDGE_PX.toDouble() / longEdge
                        decoder.setTargetSize(
                            maxOf(1, (info.size.width * scale).roundToInt()),
                            maxOf(1, (info.size.height * scale).roundToInt()),
                        )
                    }
                }
            } catch (_: IOException) {
                return PhotoPreparation.NotAnImage
            }
        val bitmap = flattenOnWhite(decoded)
        decoded.recycle()
        val output = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) { "JPEG encoding failed" }
        return PhotoPreparation.Ready(ProcessedPhoto(output.toByteArray(), bitmap))
    }

    // JPEG has no alpha, so transparent pixels would turn black.
    private fun flattenOnWhite(source: Bitmap): Bitmap {
        val flat = createBitmap(source.width, source.height)
        val canvas = Canvas(flat)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(source, 0f, 0f, null)
        return flat
    }

    companion object {
        const val DIRECTORY_NAME = "profile"
        const val MAX_SOURCE_BYTES = 20 * 1024 * 1024
        const val MAX_SOURCE_EDGE_PX = 8000
        const val MAX_STORED_EDGE_PX = 1024
        private const val JPEG_QUALITY = 85
        private const val BUFFER_BYTES = 8 * 1024
        private const val FILE_PREFIX = "photo_"
        private const val FILE_SUFFIX = ".jpg"
        private const val PARTIAL_SUFFIX = ".partial"
        private val GENERATED_NAME = Regex("photo_[0-9a-fA-F-]{1,64}\\.jpg")
    }
}
