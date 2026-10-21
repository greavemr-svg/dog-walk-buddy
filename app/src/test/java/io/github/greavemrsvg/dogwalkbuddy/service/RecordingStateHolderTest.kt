package io.github.greavemrsvg.dogwalkbuddy.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RecordingStateHolderTest {
    private val start = Instant.parse("2026-10-19T09:00:00Z")

    @Test
    fun a_fresh_holder_has_no_session_which_is_what_marks_a_recording_row_as_interrupted() {
        val holder = RecordingStateHolder()

        assertNull(holder.state.value)
        assertFalse(holder.isRecording)
    }

    @Test
    fun begin_publishes_the_session_with_no_distance() {
        val holder = RecordingStateHolder()

        holder.begin(4, start)

        assertEquals(RecordingSession(4, start, 0.0), holder.state.value)
        assertTrue(holder.isRecording)
    }

    @Test
    fun begin_again_for_the_same_walk_keeps_the_distance() {
        val holder = RecordingStateHolder()
        holder.begin(4, start)
        holder.updateDistance(250.0)

        holder.begin(4, start)

        assertEquals(250.0, holder.state.value!!.distanceM, 0.0)
    }

    @Test
    fun begin_for_another_walk_replaces_the_session() {
        val holder = RecordingStateHolder()
        holder.begin(4, start)
        holder.updateDistance(250.0)

        holder.begin(5, start.plusSeconds(60))

        assertEquals(RecordingSession(5, start.plusSeconds(60), 0.0), holder.state.value)
    }

    @Test
    fun the_distance_is_ignored_when_nothing_is_recording() {
        val holder = RecordingStateHolder()

        holder.updateDistance(10.0)

        assertNull(holder.state.value)
    }

    @Test
    fun clear_removes_the_session() {
        val holder = RecordingStateHolder()
        holder.begin(4, start)

        holder.clear()

        assertNull(holder.state.value)
    }

    @Test
    fun clearIf_clears_only_the_matching_walk() {
        val holder = RecordingStateHolder()
        holder.begin(4, start)

        holder.clearIf(5)
        assertEquals(4L, holder.state.value?.walkId)

        holder.clearIf(4)
        assertNull(holder.state.value)
    }
}
