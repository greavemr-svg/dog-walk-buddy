package io.github.greavemrsvg.dogwalkbuddy.ui.copy

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Each new or changed text of this run reads exactly as approved (AC63). Each task adds the rows for the items it
 * applies (design, "Approved copy by task"). Task 1 adds only the two font-name headings that AC8 requires.
 */
@RunWith(AndroidJUnit4::class)
class ApprovedCopyTest {
    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    /** String resource to its exact approved text. */
    private val approved: Map<Int, String> =
        mapOf(
            R.string.about_font_bricolage_heading to "Bricolage Grotesque",
            R.string.about_font_figtree_heading to "Figtree",
        )

    @Test
    fun UX_AC63_each_approved_text_reads_exactly_as_approved() {
        approved.forEach { (resId, text) ->
            assertEquals(resources.getResourceEntryName(resId), text, resources.getString(resId))
        }
    }
}
