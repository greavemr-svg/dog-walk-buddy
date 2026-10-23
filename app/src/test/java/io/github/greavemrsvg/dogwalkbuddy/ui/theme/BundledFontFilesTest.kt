package io.github.greavemrsvg.dogwalkbuddy.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer

/**
 * Plain JVM checks on the bundled font files (ADR 0006). The two fonts ship inside the app as real variable
 * TrueType files, with no downloadable-font descriptor beside them (AC5, AC65), every weight and optical size the
 * type scale asks for is inside the file's axis range (AC5), and each licence is the complete OFL 1.1 text (AC8).
 */
class BundledFontFilesTest {
    private val fontFolder = File("src/main/res/font")
    private val rawFolder = File("src/main/res/raw")

    /** A variation axis of a font's `fvar` table: tag, minimum and maximum. */
    private data class Axis(
        val tag: String,
        val min: Float,
        val max: Float,
    )

    private fun tag(
        buffer: ByteBuffer,
        offset: Int,
    ): String = String(ByteArray(TAG_LENGTH) { buffer.get(offset + it) }, Charsets.US_ASCII)

    /** Reads the `fvar` axes from the sfnt table directory. */
    private fun axesOf(file: File): List<Axis> {
        val buffer = ByteBuffer.wrap(file.readBytes())
        assertEquals("${file.name} is a TrueType sfnt", TRUETYPE_VERSION, buffer.getInt(0))
        val tableCount = buffer.getShort(NUM_TABLES_OFFSET).toInt() and U16
        val fvar =
            (0 until tableCount)
                .map { TABLE_DIRECTORY_OFFSET + it * TABLE_RECORD_SIZE }
                .firstOrNull { tag(buffer, it) == "fvar" }
        checkNotNull(fvar) { "${file.name} has no fvar table, so it is not a variable font" }
        val start = buffer.getInt(fvar + TABLE_OFFSET_FIELD)
        val axesOffset = buffer.getShort(start + FVAR_AXES_OFFSET_FIELD).toInt() and U16
        val axisCount = buffer.getShort(start + FVAR_AXIS_COUNT_FIELD).toInt() and U16
        val axisSize = buffer.getShort(start + FVAR_AXIS_SIZE_FIELD).toInt() and U16
        return (0 until axisCount).map {
            val record = start + axesOffset + it * axisSize
            Axis(
                tag = tag(buffer, record),
                min = buffer.getInt(record + AXIS_MIN_FIELD) / FIXED_ONE,
                max = buffer.getInt(record + AXIS_MAX_FIELD) / FIXED_ONE,
            )
        }
    }

    private fun assertCovers(
        name: String,
        axes: List<Axis>,
        tag: String,
        values: List<Int>,
    ) {
        val axis = checkNotNull(axes.firstOrNull { it.tag == tag }) { "$name has no $tag axis: $axes" }
        values.forEach { value ->
            assertTrue("$name $tag $value is outside ${axis.min}-${axis.max}", value >= axis.min && value <= axis.max)
        }
    }

    @Test
    fun UX_AC65_the_font_folder_holds_only_the_two_bundled_font_files() {
        assertTrue("font folder not found from ${File("").absolutePath}", fontFolder.isDirectory)
        // No XML font descriptor (which could name a downloadable-font provider), only the two TTF files.
        assertEquals(
            listOf("bricolage_grotesque.ttf", "figtree.ttf"),
            fontFolder
                .listFiles()
                .orEmpty()
                .map { it.name }
                .sorted(),
        )
    }

    @Test
    fun UX_AC5_bricolage_grotesque_is_a_variable_font_covering_the_display_weights_and_optical_sizes() {
        val axes = axesOf(File(fontFolder, "bricolage_grotesque.ttf"))

        assertCovers("Bricolage Grotesque", axes, "wght", listOf(700, 800))
        // ratingWord 60, Caution 52, timer 92, screenTitle 32, checkQuestion 27, statLarge 36, bestWindow 24.
        assertCovers("Bricolage Grotesque", axes, "opsz", listOf(60, 52, 92, 32, 27, 36, 24))
    }

    @Test
    fun UX_AC5_figtree_is_a_variable_font_covering_the_body_weights() {
        val axes = axesOf(File(fontFolder, "figtree.ttf"))

        assertCovers("Figtree", axes, "wght", listOf(400, 500, 600, 700, 800))
    }

    private fun assertFullOfl(
        fileName: String,
        copyright: String,
    ) {
        val text = File(rawFolder, fileName).readText(Charsets.UTF_8).replace("\r\n", "\n").trim()
        assertTrue("$fileName starts with its copyright line", text.startsWith(copyright))
        listOf(
            "This Font Software is licensed under the SIL Open Font License, Version 1.1.",
            "SIL OPEN FONT LICENSE Version 1.1 - 26 February 2007",
            "PREAMBLE",
            "DEFINITIONS",
            "PERMISSION & CONDITIONS",
            "TERMINATION",
            "DISCLAIMER",
        ).forEach { assertTrue("$fileName lacks \"$it\"", text.contains(it)) }
        assertTrue("$fileName is cut short", text.endsWith("OTHER DEALINGS IN THE FONT SOFTWARE."))
    }

    @Test
    fun UX_AC8_the_bricolage_grotesque_licence_is_the_complete_ofl_text() =
        assertFullOfl(
            "licence_ofl_bricolage_grotesque.txt",
            "Copyright 2022 The Bricolage Grotesque Project Authors (https://github.com/ateliertriay/bricolage)",
        )

    @Test
    fun UX_AC8_the_figtree_licence_is_the_complete_ofl_text() =
        assertFullOfl(
            "licence_ofl_figtree.txt",
            "Copyright 2022 The Figtree Project Authors (https://github.com/erikdkennedy/figtree)",
        )

    private companion object {
        const val TRUETYPE_VERSION = 0x00010000
        const val NUM_TABLES_OFFSET = 4
        const val TABLE_DIRECTORY_OFFSET = 12
        const val TABLE_RECORD_SIZE = 16
        const val TABLE_OFFSET_FIELD = 8
        const val TAG_LENGTH = 4
        const val FVAR_AXES_OFFSET_FIELD = 4
        const val FVAR_AXIS_COUNT_FIELD = 8
        const val FVAR_AXIS_SIZE_FIELD = 10
        const val AXIS_MIN_FIELD = 4
        const val AXIS_MAX_FIELD = 12
        const val FIXED_ONE = 65536f
        const val U16 = 0xFFFF
    }
}
