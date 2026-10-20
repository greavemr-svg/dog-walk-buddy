package io.github.greavemrsvg.dogwalkbuddy.testutil

import android.graphics.Bitmap
import android.graphics.Color
import android.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File

/** Tiny generated images for tests. No real photos are ever used as fixtures. */
object SyntheticImages {
    fun bitmap(
        width: Int,
        height: Int,
        color: Int = Color.rgb(200, 120, 40),
    ): Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { it.eraseColor(color) }

    fun jpeg(
        width: Int,
        height: Int,
    ): ByteArray = encode(bitmap(width, height), Bitmap.CompressFormat.JPEG)

    fun png(
        width: Int,
        height: Int,
    ): ByteArray = encode(bitmap(width, height), Bitmap.CompressFormat.PNG)

    fun transparentPng(
        width: Int,
        height: Int,
    ): ByteArray = encode(bitmap(width, height, Color.TRANSPARENT), Bitmap.CompressFormat.PNG)

    /** A JPEG carrying an EXIF orientation and a (fictional) GPS position, to prove both are handled. */
    fun jpegWithExif(
        width: Int,
        height: Int,
        orientation: Int,
        workDir: File,
    ): ByteArray {
        val file = File(workDir, "exif.jpg")
        file.writeBytes(jpeg(width, height))
        ExifInterface(file.path).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            setAttribute(ExifInterface.TAG_GPS_LATITUDE, GPS_LATITUDE)
            setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE, GPS_LONGITUDE)
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W")
            saveAttributes()
        }
        return file.readBytes()
    }

    private fun encode(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
    ): ByteArray {
        val output = ByteArrayOutputStream()
        check(bitmap.compress(format, 90, output))
        return output.toByteArray()
    }

    private const val GPS_LATITUDE = "51/1,30/1,0/1"
    private const val GPS_LONGITUDE = "0/1,6/1,0/1"
}
