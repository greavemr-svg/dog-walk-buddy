package io.github.greavemrsvg.dogwalkbuddy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Plain JVM checks on the source manifest and backup resources (AC48 and T1 security notes). */
class SourceManifestHardeningTest {
    private val manifest =
        DocumentBuilderFactory
            .newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(File("src/main/AndroidManifest.xml"))

    private fun NodeList.elements(): List<Element> = (0 until length).map { item(it) as Element }

    @Test
    fun AC48_backup_rule_resources_referenced_by_the_manifest_exist() {
        assertTrue(File("src/main/res/xml/data_extraction_rules.xml").isFile)
        assertTrue(File("src/main/res/xml/backup_rules.xml").isFile)
        assertTrue(File("src/main/res/xml/network_security_config.xml").isFile)
    }

    @Test
    fun AC48_no_manifest_attribute_re_enables_backup() {
        val application = manifest.getElementsByTagName("application").item(0) as Element
        assertEquals("false", application.getAttributeNS(ANDROID_NS, "allowBackup"))
        assertFalse(application.hasAttributeNS(ANDROID_NS, "backupAgent"))
        assertFalse(application.hasAttributeNS(ANDROID_NS, "hasFragileUserData"))
    }

    @Test
    fun AC48_backup_exclusions_cover_each_domain_exactly_once_per_section() {
        val doc =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(File("src/main/res/xml/data_extraction_rules.xml"))
        for (section in listOf("cloud-backup", "device-transfer")) {
            val domains =
                (doc.getElementsByTagName(section).item(0) as Element)
                    .getElementsByTagName("exclude")
                    .elements()
                    .map { it.getAttribute("domain") }
            assertEquals("duplicate domains in $section", domains.size, domains.toSet().size)
        }
    }

    @Test
    fun only_the_launcher_activity_is_declared_exported_in_the_source_manifest() {
        val components =
            listOf("activity", "service", "receiver", "provider")
                .flatMap { manifest.getElementsByTagName(it).elements() }
                .filter { it.getAttributeNS(ANDROID_NS, "exported") == "true" }
                .map { it.getAttributeNS(ANDROID_NS, "name") }

        assertEquals(listOf(".MainActivity"), components)
    }

    @Test
    fun source_manifest_declares_only_internet() {
        val permissions =
            manifest.getElementsByTagName("uses-permission").elements().map { it.getAttributeNS(ANDROID_NS, "name") }

        assertEquals(listOf("android.permission.INTERNET"), permissions)
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
