package io.github.greavemrsvg.dogwalkbuddy.ui.profile

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.data.db.AppDatabase
import io.github.greavemrsvg.dogwalkbuddy.data.db.DogProfileDao
import io.github.greavemrsvg.dogwalkbuddy.data.db.DogProfileEntity
import io.github.greavemrsvg.dogwalkbuddy.data.repo.PhotoStore
import io.github.greavemrsvg.dogwalkbuddy.data.repo.ProfileRepository
import io.github.greavemrsvg.dogwalkbuddy.domain.profile.ProfileError
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity
import io.github.greavemrsvg.dogwalkbuddy.testutil.SyntheticImages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProfileViewModelTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var database: AppDatabase
    private lateinit var repository: ProfileRepository
    private lateinit var filesDir: File
    private var counter = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        filesDir = temporaryFolder.newFolder("files")
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
                .setQueryExecutor(Runnable::run)
                .setTransactionExecutor(Runnable::run)
                .build()
        repository = repositoryOver(database.dogProfileDao())
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private fun repositoryOver(dao: DogProfileDao) =
        ProfileRepository(dao, PhotoStore(filesDir, Dispatchers.Unconfined) { "00000000-0000-0000-0000-00000000000${counter++}" })

    private fun newViewModel() = ProfileViewModel(repository)

    private fun photoFiles() = File(filesDir, "profile").list().orEmpty().toList()

    @Test
    fun AC41_the_form_starts_empty_with_normal_sensitivity_when_there_is_no_profile() {
        val state = newViewModel().state.value

        assertTrue(state.isLoaded)
        assertEquals("", state.name)
        assertEquals(HeatSensitivity.NORMAL, state.sensitivity)
        assertNull(state.photo)
    }

    @Test
    fun AC41_saving_shows_the_saved_values_in_a_new_view_model() =
        runTest {
            val first = newViewModel()
            first.onNameChange("Biscuit")
            first.onBreedChange("Cockapoo")
            first.onSensitivityChange(HeatSensitivity.SENSITIVE)
            first.onPhotoPicked { ByteArrayInputStream(SyntheticImages.jpeg(64, 64)) }

            first.onSave()

            assertTrue(first.state.value.justSaved)
            val relaunched = newViewModel().state.value
            assertEquals("Biscuit", relaunched.name)
            assertEquals("Cockapoo", relaunched.breed)
            assertEquals(HeatSensitivity.SENSITIVE, relaunched.sensitivity)
            assertNotNull(relaunched.photo)
        }

    @Test
    fun AC42_an_empty_name_is_refused_with_a_name_error_and_nothing_is_saved() =
        runTest {
            val viewModel = newViewModel()

            viewModel.onSave()

            assertEquals(ProfileError.NAME_REQUIRED, viewModel.state.value.nameError)
            assertFalse(viewModel.state.value.justSaved)
            assertFalse(viewModel.state.value.isSaving)
            assertNull(repository.observeProfile().first())
        }

    @Test
    fun AC42_a_long_name_and_a_long_breed_are_each_refused_on_their_own_field() {
        val viewModel = newViewModel()
        viewModel.onNameChange("a".repeat(31))
        viewModel.onBreedChange("b".repeat(51))

        viewModel.onSave()

        assertEquals(ProfileError.NAME_TOO_LONG, viewModel.state.value.nameError)
        assertEquals(ProfileError.BREED_TOO_LONG, viewModel.state.value.breedError)
    }

    @Test
    fun AC42_editing_a_field_clears_its_error() {
        val viewModel = newViewModel()
        viewModel.onSave()

        viewModel.onNameChange("Biscuit")

        assertNull(viewModel.state.value.nameError)
    }

    @Test
    fun AC42_pasted_text_is_capped_far_above_the_limits_so_the_limit_message_still_applies() {
        val viewModel = newViewModel()

        viewModel.onNameChange("a".repeat(5000))
        viewModel.onSave()

        assertEquals(ProfileViewModel.INPUT_HARD_CAP, viewModel.state.value.name.length)
        assertEquals(ProfileError.NAME_TOO_LONG, viewModel.state.value.nameError)
    }

    @Test
    fun AC42_breed_and_photo_may_be_left_empty() =
        runTest {
            val viewModel = newViewModel()
            viewModel.onNameChange("Biscuit")

            viewModel.onSave()

            assertTrue(viewModel.state.value.justSaved)
            assertEquals("Biscuit", repository.observeProfile().first()!!.name)
        }

    @Test
    fun AC43_removing_the_photo_and_saving_deletes_it() =
        runTest {
            val viewModel = newViewModel()
            viewModel.onNameChange("Biscuit")
            viewModel.onPhotoPicked { ByteArrayInputStream(SyntheticImages.jpeg(64, 64)) }
            viewModel.onSave()
            assertEquals(1, photoFiles().size)

            val relaunched = newViewModel()
            relaunched.onRemovePhoto()
            assertNull(relaunched.state.value.photo)
            relaunched.onSave()

            assertTrue(photoFiles().isEmpty())
            assertNull(newViewModel().state.value.photo)
        }

    @Test
    fun AC43_replacing_the_photo_and_saving_keeps_a_single_file() =
        runTest {
            val viewModel = newViewModel()
            viewModel.onNameChange("Biscuit")
            viewModel.onPhotoPicked { ByteArrayInputStream(SyntheticImages.jpeg(64, 64)) }
            viewModel.onSave()
            val first = photoFiles()

            viewModel.onPhotoPicked { ByteArrayInputStream(SyntheticImages.jpeg(32, 32)) }
            viewModel.onSave()

            assertEquals(1, photoFiles().size)
            assertTrue(photoFiles() != first)
        }

    @Test
    fun AC43_a_photo_removed_before_saving_a_new_profile_is_not_stored() =
        runTest {
            val viewModel = newViewModel()
            viewModel.onNameChange("Biscuit")
            viewModel.onPhotoPicked { ByteArrayInputStream(SyntheticImages.jpeg(64, 64)) }
            viewModel.onRemovePhoto()

            viewModel.onSave()

            assertTrue(photoFiles().isEmpty())
            assertNull(repository.observeProfile().first()!!.photoFileName)
        }

    @Test
    fun AC41_a_refused_photo_shows_a_problem_and_keeps_the_current_photo() {
        val viewModel = newViewModel()

        viewModel.onPhotoPicked { ByteArrayInputStream("not an image".toByteArray()) }
        assertEquals(PhotoProblem.NOT_AN_IMAGE, viewModel.state.value.photoProblem)

        viewModel.onPhotoPicked { ByteArrayInputStream(SyntheticImages.png(8001, 1)) }
        assertEquals(PhotoProblem.TOO_LARGE, viewModel.state.value.photoProblem)

        viewModel.onPhotoPicked { null }
        assertEquals(PhotoProblem.UNREADABLE, viewModel.state.value.photoProblem)
        assertNull(viewModel.state.value.photo)

        viewModel.onPhotoPicked { ByteArrayInputStream(SyntheticImages.jpeg(16, 16)) }
        assertNull(viewModel.state.value.photoProblem)
        assertNotNull(viewModel.state.value.photo)
    }

    @Test
    fun AC41_a_failing_save_tells_the_user_and_can_be_retried() {
        val dao = database.dogProfileDao()
        val failingDao =
            object : DogProfileDao by dao {
                override suspend fun save(entity: DogProfileEntity) = throw IllegalStateException("disk full")
            }
        val viewModel = ProfileViewModel(repositoryOver(failingDao))
        viewModel.onNameChange("Biscuit")

        viewModel.onSave()

        assertTrue(viewModel.state.value.saveFailed)
        assertFalse(viewModel.state.value.isSaving)
        viewModel.onSave()
        assertTrue(viewModel.state.value.saveFailed)
    }
}
