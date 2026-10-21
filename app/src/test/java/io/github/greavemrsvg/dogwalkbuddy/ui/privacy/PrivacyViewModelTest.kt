package io.github.greavemrsvg.dogwalkbuddy.ui.privacy

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** The "Delete all my data" flow: ask, confirm, delete (AC47). */
class PrivacyViewModelTest {
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val logged = mutableListOf<String>()
    private var deleteCalls = 0

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun viewModel(deleteAll: suspend () -> Unit = { deleteCalls++ }) = PrivacyViewModel(deleteAll, scope) { logged += it }

    @Test
    fun the_initial_state_is_idle() {
        assertEquals(PrivacyUiState(), viewModel().state.value)
    }

    @Test
    fun AC47_asking_opens_the_confirmation_and_deletes_nothing() {
        val vm = viewModel()

        vm.requestDelete()

        assertTrue(vm.state.value.confirming)
        assertEquals(0, deleteCalls)
    }

    @Test
    fun AC47_cancelling_closes_the_confirmation_and_deletes_nothing() {
        val vm = viewModel()
        vm.requestDelete()

        vm.cancelDelete()

        assertEquals(PrivacyUiState(), vm.state.value)
        assertEquals(0, deleteCalls)
    }

    @Test
    fun AC47_confirming_deletes_once_and_reports_success() {
        val vm = viewModel()
        vm.requestDelete()

        vm.confirmDelete()

        assertEquals(1, deleteCalls)
        assertEquals(PrivacyUiState(deleted = true), vm.state.value)
    }

    @Test
    fun AC47_confirming_without_asking_first_deletes_nothing() {
        val vm = viewModel()

        vm.confirmDelete()

        assertEquals(0, deleteCalls)
        assertEquals(PrivacyUiState(), vm.state.value)
    }

    @Test
    fun AC47_while_deleting_the_state_says_so_and_a_second_request_or_confirm_does_nothing() {
        val release = CompletableDeferred<Unit>()
        val vm =
            viewModel {
                deleteCalls++
                release.await()
            }
        vm.requestDelete()
        vm.confirmDelete()

        vm.requestDelete()
        vm.confirmDelete()

        assertEquals(PrivacyUiState(deleting = true), vm.state.value)
        assertEquals(1, deleteCalls)
        release.complete(Unit)
        assertEquals(PrivacyUiState(deleted = true), vm.state.value)
    }

    @Test
    fun AC47_a_failure_is_reported_and_logged_by_class_name_only_and_can_be_retried() {
        var fail = true
        val vm =
            viewModel {
                deleteCalls++
                if (fail) throw IOException("secret path /data/user/0/x")
            }
        vm.requestDelete()
        vm.confirmDelete()

        assertEquals(PrivacyUiState(deleteFailed = true), vm.state.value)
        assertEquals(listOf("IOException"), logged)

        fail = false
        vm.requestDelete()
        assertFalse(vm.state.value.deleteFailed)
        vm.confirmDelete()

        assertEquals(PrivacyUiState(deleted = true), vm.state.value)
        assertEquals(2, deleteCalls)
    }

    @Test
    fun dismissing_the_failure_clears_the_flag() {
        val vm = viewModel { throw IOException() }
        vm.requestDelete()
        vm.confirmDelete()

        vm.dismissDeleteFailure()

        assertEquals(PrivacyUiState(), vm.state.value)
    }

    @Test
    fun a_new_request_after_success_clears_the_success_message() {
        val vm = viewModel()
        vm.requestDelete()
        vm.confirmDelete()

        vm.requestDelete()

        assertEquals(PrivacyUiState(confirming = true), vm.state.value)
    }
}
