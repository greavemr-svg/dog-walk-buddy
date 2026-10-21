package io.github.greavemrsvg.dogwalkbuddy.data.repo

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.testutil.SyntheticImages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

/** AC47: deleting every photo copy. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoStoreDeleteAllTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var root: File
    private var counter = 0
    private lateinit var store: PhotoStore

    @Before
    fun setUp() {
        root = temporaryFolder.newFolder("files")
        store = PhotoStore(root, Dispatchers.Unconfined) { "00000000-0000-0000-0000-00000000000${counter++}" }
    }

    private suspend fun storePhoto(): String {
        val ready = store.prepare { ByteArrayInputStream(SyntheticImages.jpeg(64, 64)) } as PhotoPreparation.Ready
        return store.store(ready.photo)
    }

    @Test
    fun AC47_deleteAll_removes_every_photo_copy_and_the_profile_directory() =
        runBlocking {
            val first = storePhoto()
            val second = storePhoto()

            store.deleteAll()

            assertFalse(File(root, PhotoStore.DIRECTORY_NAME).exists())
            assertNull(store.load(first))
            assertNull(store.load(second))
        }

    @Test
    fun AC47_deleteAll_also_removes_a_half_written_partial_file() =
        runBlocking {
            storePhoto()
            val partial = File(root, "${PhotoStore.DIRECTORY_NAME}/photo_00000000-0000-0000-0000-000000000009.jpg.partial")
            partial.writeBytes(byteArrayOf(1, 2, 3))
            assertTrue(partial.isFile)

            store.deleteAll()

            assertFalse(partial.exists())
            assertEquals(0, root.listFiles()!!.size)
        }

    @Test
    fun AC47_deleteAll_with_no_photos_does_nothing_and_can_be_repeated() =
        runBlocking {
            store.deleteAll()
            storePhoto()
            store.deleteAll()
            store.deleteAll()

            assertFalse(File(root, PhotoStore.DIRECTORY_NAME).exists())
        }

    @Test
    fun AC47_photos_can_be_stored_again_after_deleteAll() =
        runBlocking {
            storePhoto()
            store.deleteAll()

            val fileName = storePhoto()

            assertTrue(store.load(fileName) != null)
        }

    @Test
    fun AC47_deleteAll_leaves_other_files_in_the_files_directory_alone() =
        runBlocking {
            val other = File(root, "unrelated.txt").apply { writeText("x") }
            storePhoto()

            store.deleteAll()

            assertTrue(other.isFile)
        }
}
