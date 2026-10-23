package io.github.greavemrsvg.dogwalkbuddy

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(AndroidJUnit4::class)
class ManifestSecurityTest {
    private val context = ApplicationProvider.getApplicationContext<DogWalkBuddyApp>()

    @Test
    fun AC48_cloud_backup_is_disabled_for_the_application() {
        assertEquals(0, context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }

    @Test
    fun AC48_manifest_points_at_both_backup_rule_files() {
        val application = sourceManifestApplication()

        assertEquals("false", application.getAttributeNS(ANDROID_NS, "allowBackup"))
        assertEquals("@xml/data_extraction_rules", application.getAttributeNS(ANDROID_NS, "dataExtractionRules"))
        assertEquals("@xml/backup_rules", application.getAttributeNS(ANDROID_NS, "fullBackupContent"))
    }

    @Test
    fun AC48_data_extraction_rules_exclude_every_domain_from_cloud_backup() {
        assertEveryDomainExcluded(sourceXml("data_extraction_rules.xml"), "cloud-backup", EXTRACTION_DOMAINS)
    }

    @Test
    fun AC48_data_extraction_rules_exclude_every_domain_from_device_transfer() {
        assertEveryDomainExcluded(sourceXml("data_extraction_rules.xml"), "device-transfer", EXTRACTION_DOMAINS)
    }

    @Test
    fun AC48_data_extraction_rules_never_include_anything() {
        val includes = sourceXml("data_extraction_rules.xml").getElementsByTagName("include")

        assertEquals(0, includes.length)
    }

    @Test
    fun AC48_legacy_backup_rules_exclude_every_domain() {
        val rules = sourceXml("backup_rules.xml")

        assertEveryDomainExcluded(rules, "full-backup-content", BACKUP_DOMAINS)
        assertEquals(0, rules.getElementsByTagName("include").length)
    }

    @Test
    fun cleartext_traffic_is_disabled_in_manifest_and_network_security_config() {
        val application = sourceManifestApplication()
        val config = sourceXml("network_security_config.xml")
        val baseConfig = config.getElementsByTagName("base-config").item(0) as Element

        assertEquals("false", application.getAttributeNS(ANDROID_NS, "usesCleartextTraffic"))
        assertEquals("@xml/network_security_config", application.getAttributeNS(ANDROID_NS, "networkSecurityConfig"))
        assertEquals("false", baseConfig.getAttribute("cleartextTrafficPermitted"))
        assertEquals(0, config.getElementsByTagName("domain-config").length)
    }

    @Test
    fun user_certificates_are_trusted_only_in_debug_overrides() {
        val config = sourceXml("network_security_config.xml")
        val baseConfig = config.getElementsByTagName("base-config").item(0) as Element
        val debugOverrides = config.getElementsByTagName("debug-overrides").item(0) as Element

        assertEquals(listOf("system"), certificateSources(baseConfig))
        assertTrue(certificateSources(debugOverrides).contains("user"))
    }

    @Test
    fun only_the_launcher_activity_is_exported() {
        val info =
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_ACTIVITIES or
                    PackageManager.GET_SERVICES or
                    PackageManager.GET_RECEIVERS or
                    PackageManager.GET_PROVIDERS,
            )
        val exported =
            listOfNotNull(info.activities, info.services, info.receivers, info.providers)
                .flatMap { it.asList() }
                .filter { it.exported }
                .map { it.name }
                // Activities that the debug-only test and tooling libraries add. Release builds never have them.
                .filterNot { it in DEBUG_ONLY_ACTIVITIES }

        assertEquals(listOf(MainActivity::class.java.name), exported)
    }

    @Test
    fun profile_installer_receiver_is_removed_from_the_merged_manifest() {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_RECEIVERS)
        val receivers = info.receivers.orEmpty().map { it.name }

        assertFalse(receivers.contains("androidx.profileinstaller.ProfileInstallReceiver"))
    }

    private fun certificateSources(parent: Element): List<String> {
        val certificates = parent.getElementsByTagName("certificates")
        return (0 until certificates.length).map { (certificates.item(it) as Element).getAttribute("src") }
    }

    private fun assertEveryDomainExcluded(
        document: Document,
        sectionTag: String,
        expectedDomains: Set<String>,
    ) {
        val section = document.getElementsByTagName(sectionTag).item(0) as Element
        val excludes = section.getElementsByTagName("exclude")
        val excluded =
            (0 until excludes.length)
                .map { excludes.item(it) as Element }
                .onEach { assertEquals(".", it.getAttribute("path")) }
                .map { it.getAttribute("domain") }
                .toSet()

        assertEquals(expectedDomains, excluded)
    }

    private fun sourceManifestApplication(): Element {
        val manifest = parse(File("src/main/AndroidManifest.xml"))
        return manifest.getElementsByTagName("application").item(0) as Element
    }

    private fun sourceXml(name: String): Document = parse(File("src/main/res/xml/$name"))

    private fun parse(file: File): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        return factory.newDocumentBuilder().parse(file)
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        val DEBUG_ONLY_ACTIVITIES =
            setOf("androidx.activity.ComponentActivity", "androidx.compose.ui.tooling.PreviewActivity")
        val BACKUP_DOMAINS = setOf("root", "file", "database", "sharedpref", "external")
        val EXTRACTION_DOMAINS =
            BACKUP_DOMAINS + setOf("device_root", "device_file", "device_database", "device_sharedpref")
    }
}
