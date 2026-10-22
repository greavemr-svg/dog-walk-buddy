package io.github.greavemrsvg.dogwalkbuddy.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Plain JVM guard on the copied Material Symbols vector drawables (ADR 0008 Amendment 1). Every such file must
 * resolve in any theme, so it may not reference an app or library theme attribute (`?attr/`), and it must carry
 * the Apache License 2.0 §4(b) modification notice in its header.
 */
class MaterialSymbolsIconFilesTest {
    private val iconFiles: List<File> =
        File("src/main/res/drawable")
            .listFiles { file -> file.isFile && file.name.startsWith("ic_") && file.name.endsWith(".xml") }
            .orEmpty()
            .filter { file -> file.useLines { lines -> lines.firstOrNull().orEmpty() }.contains(MARKER) }
            .sortedBy { it.name }

    @Test
    fun at_least_the_three_rating_icons_are_material_symbols_files() {
        assertTrue(
            "expected at least $MIN_FILES Material Symbols files, found ${iconFiles.map { it.name }}",
            iconFiles.size >= MIN_FILES,
        )
    }

    @Test
    fun material_symbols_files_reference_no_theme_attribute_outside_the_platform() {
        for (file in iconFiles) {
            // The one-line header names the old tint in its modification notice, so only the XML below it counts.
            val body = file.readLines().drop(1)
            assertFalse("${file.name} references ?attr/", body.any { it.contains("?attr/") })
        }
    }

    @Test
    fun material_symbols_files_state_in_the_header_that_they_were_modified() {
        for (file in iconFiles) {
            val header = file.useLines { lines -> lines.first() }
            assertTrue("${file.name} header lacks the modification notice", header.contains("modified:"))
        }
    }

    @Test
    fun the_three_rating_icons_are_the_adr_0008_symbols_with_the_platform_tint() {
        val expected =
            mapOf(
                "ic_rating_go.xml" to "check_circle",
                "ic_rating_caution.xml" to "warning",
                "ic_rating_avoid.xml" to "dangerous",
            )
        val byName = iconFiles.associateBy { it.name }
        expected.forEach { (fileName, symbol) ->
            val file = checkNotNull(byName[fileName]) { "$fileName is not a Material Symbols Rounded file" }
            val lines = file.readLines()
            assertTrue("$fileName header names the $symbol symbol", lines.first().startsWith("<!-- $symbol (fill 1)."))
            assertTrue(
                "$fileName header names the fill 1 Rounded source file",
                lines.first().contains("symbols/android/$symbol/materialsymbolsrounded/${symbol}_fill1_24px.xml"),
            )
            val tints = lines.drop(1).filter { it.contains("android:tint=") }
            assertEquals("$fileName tint", listOf("android:tint=\"?android:attr/colorControlNormal\">"), tints.map { it.trim() })
        }
    }

    @Test
    fun the_guard_reads_the_real_drawable_folder() {
        val folder = File("src/main/res/drawable")
        assertTrue("drawable folder not found from ${File("").absolutePath}", folder.isDirectory)
        assertTrue(File(folder, "ic_rating_go.xml").isFile)
    }

    private companion object {
        const val MARKER = "Material Symbols Rounded"
        const val MIN_FILES = 3
    }
}
