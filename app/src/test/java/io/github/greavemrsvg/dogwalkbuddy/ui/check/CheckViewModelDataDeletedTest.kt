package io.github.greavemrsvg.dogwalkbuddy.ui.check

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity
import io.github.greavemrsvg.dogwalkbuddy.testutil.TestAppContainer
import io.github.greavemrsvg.dogwalkbuddy.testutil.awaitCondition
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

/** AC47: after "Delete all my data" the check forgets a result it holds in memory, as it forgets the stored one. */
@RunWith(AndroidJUnit4::class)
class CheckViewModelDataDeletedTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var test: TestAppContainer
    private val deleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @Before
    fun setUp() {
        test = TestAppContainer(temporaryFolder)
    }

    @After
    fun tearDown() {
        test.close()
    }

    private fun viewModel() =
        CheckViewModel(
            repository = test.container.checkRepository,
            locationSource = test.container.approximateLocationSource,
            sensitivity = flowOf(HeatSensitivity.NORMAL),
            hasProfile = flowOf(false),
            hasCoarsePermission = { true },
            clock = test.clock,
            dataDeleted = deleted,
        )

    @Test
    fun AC47_a_result_held_in_memory_is_forgotten_and_the_next_opening_checks_again() {
        test.stub.enqueueFixture().enqueueFixture()
        val vm = viewModel()
        vm.onOpened()
        awaitCondition("first result") { vm.state.value is CheckUiState.Rated }
        assertEquals(1, test.stub.server.requestCount)

        awaitCondition("delete announced") { deleted.subscriptionCount.value > 0 }
        deleted.tryEmit(Unit)
        awaitCondition("result forgotten") { vm.state.value == CheckUiState.Loading }
        vm.onOpened()
        awaitCondition("second result") { vm.state.value is CheckUiState.Rated }

        assertEquals(2, test.stub.server.requestCount)
    }
}
