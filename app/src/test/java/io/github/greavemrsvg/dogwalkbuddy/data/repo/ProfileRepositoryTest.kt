package io.github.greavemrsvg.dogwalkbuddy.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.data.db.AppDatabase
import io.github.greavemrsvg.dogwalkbuddy.data.db.DogProfileEntity
import io.github.greavemrsvg.dogwalkbuddy.domain.profile.DogProfile
import io.github.greavemrsvg.dogwalkbuddy.domain.profile.ProfileError
import io.github.greavemrsvg.dogwalkbuddy.domain.profile.ProfileInput
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity
import io.github.greavemrsvg.dogwalkbuddy.testutil.SyntheticImages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProfileRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var filesDir: File
    private lateinit var databaseFile: File
    private lateinit var database: AppDatabase
    private var counter = 0
    private lateinit var store: PhotoStore
    private lateinit var repository: ProfileRepository

    @Before
    fun setUp() {
        filesDir = temporaryFolder.newFolder("files")
        databaseFile = File(temporaryFolder.newFolder("db"), "test.db")
        store = PhotoStore(filesDir, Dispatchers.Unconfined) { "00000000-0000-0000-0000-00000000000${counter++}" }
        open()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun open() {
        database =
            Room
                .databaseBuilder(context, AppDatabase::class.java, databaseFile.path)
                .setQueryExecutor(Runnable::run)
                .setTransactionExecutor(Runnable::run)
                .build()
        repository = ProfileRepository(database.dogProfileDao(), store)
    }

    /** Closes and reopens the database file, like a new app launch. */
    private fun relaunch() {
        database.close()
        open()
    }

    private suspend fun photo(size: Int = 64): PhotoChange.Replace {
        val prepared = store.prepare { ByteArrayInputStream(SyntheticImages.jpeg(size, size)) }
        return PhotoChange.Replace((prepared as PhotoPreparation.Ready).photo)
    }

    private fun photoFiles(): List<String> = File(filesDir, "profile").list()?.sorted().orEmpty()

    @Test
    fun AC41_the_profile_with_photo_and_sensitivity_is_shown_after_a_relaunch() =
        runTest {
            val result = repository.save(ProfileInput("Biscuit", "Cockapoo"), HeatSensitivity.SENSITIVE, photo())
            assertTrue(result is SaveResult.Saved)

            relaunch()

            val profile = repository.observeProfile().first()!!
            assertEquals("Biscuit", profile.name)
            assertEquals("Cockapoo", profile.breed)
            assertEquals(HeatSensitivity.SENSITIVE, profile.heatSensitivity)
            assertNotNull(repository.loadPhoto(profile))
        }

    @Test
    fun AC41_there_is_no_profile_at_first_and_the_sensitivity_is_normal() =
        runTest {
            assertNull(repository.observeProfile().first())
            assertEquals(HeatSensitivity.NORMAL, repository.observeSensitivity().first())
        }

    @Test
    fun AC41_the_sensitivity_flow_follows_the_saved_profile() =
        runTest {
            repository.save(ProfileInput("Biscuit"), HeatSensitivity.SENSITIVE, PhotoChange.Keep)

            assertEquals(HeatSensitivity.SENSITIVE, repository.observeSensitivity().first())
        }

    @Test
    fun AC41_an_unknown_stored_sensitivity_falls_back_to_normal() =
        runTest {
            database.dogProfileDao().save(DogProfileEntity(name = "Biscuit", breed = null, photoFileName = null, heatSensitivity = "BOGUS"))

            assertEquals(HeatSensitivity.NORMAL, repository.observeProfile().first()!!.heatSensitivity)
        }

    @Test
    fun AC41_saving_again_updates_the_single_profile_and_trims_the_input() =
        runTest {
            repository.save(ProfileInput("Biscuit", "Cockapoo"), HeatSensitivity.NORMAL, PhotoChange.Keep)
            repository.save(ProfileInput("  Rex  ", "  "), HeatSensitivity.NORMAL, PhotoChange.Keep)

            assertEquals(DogProfile("Rex", null, null, HeatSensitivity.NORMAL), repository.observeProfile().first())
        }

    @Test
    fun AC42_an_invalid_profile_is_refused_and_nothing_is_stored() =
        runTest {
            val result = repository.save(ProfileInput("", "b".repeat(51)), HeatSensitivity.NORMAL, photo())

            result as SaveResult.Invalid
            assertEquals(ProfileError.NAME_REQUIRED, result.validation.nameError)
            assertEquals(ProfileError.BREED_TOO_LONG, result.validation.breedError)
            assertNull(repository.observeProfile().first())
            assertTrue(photoFiles().isEmpty())
        }

    @Test
    fun AC43_replacing_the_photo_keeps_one_file_and_deletes_the_old_one() =
        runTest {
            repository.save(ProfileInput("Biscuit"), HeatSensitivity.NORMAL, photo())
            val first = photoFiles()

            repository.save(ProfileInput("Biscuit"), HeatSensitivity.NORMAL, photo(32))

            val second = photoFiles()
            assertEquals(1, second.size)
            assertNotEquals(first, second)
            assertEquals(second.single(), repository.observeProfile().first()!!.photoFileName)
        }

    @Test
    fun AC43_removing_the_photo_deletes_the_file_and_clears_the_profile_reference() =
        runTest {
            repository.save(ProfileInput("Biscuit"), HeatSensitivity.NORMAL, photo())

            repository.save(ProfileInput("Biscuit"), HeatSensitivity.NORMAL, PhotoChange.Remove)

            assertTrue(photoFiles().isEmpty())
            relaunch()
            val profile = repository.observeProfile().first()!!
            assertNull(profile.photoFileName)
            assertNull(repository.loadPhoto(profile))
        }

    @Test
    fun AC43_keeping_the_photo_leaves_the_file_in_place() =
        runTest {
            repository.save(ProfileInput("Biscuit"), HeatSensitivity.NORMAL, photo())
            val before = photoFiles()

            repository.save(ProfileInput("Biscuit"), HeatSensitivity.SENSITIVE, PhotoChange.Keep)

            assertEquals(before, photoFiles())
        }

    @Test
    fun AC43_a_failed_save_removes_the_new_photo_and_keeps_the_old_one() =
        runTest {
            repository.save(ProfileInput("Biscuit"), HeatSensitivity.NORMAL, photo())
            val before = photoFiles()
            val change = photo(32)
            val dao = database.dogProfileDao()
            val failing =
                ProfileRepository(
                    object : io.github.greavemrsvg.dogwalkbuddy.data.db.DogProfileDao by dao {
                        override suspend fun save(entity: DogProfileEntity) = throw IllegalStateException("disk full")
                    },
                    store,
                )

            val failure = runCatching { failing.save(ProfileInput("Biscuit"), HeatSensitivity.NORMAL, change) }

            assertTrue(failure.exceptionOrNull() is IllegalStateException)
            assertEquals(before, photoFiles())
            assertEquals(before.single(), repository.observeProfile().first()!!.photoFileName)
        }
}
