package io.github.greavemrsvg.dogwalkbuddy.data.repo

import android.graphics.Color
import android.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.testutil.SyntheticImages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
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
import java.io.IOException
import java.io.InputStream

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoStoreTest {
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

    private suspend fun prepared(bytes: ByteArray): ProcessedPhoto =
        (
            store.prepare {
                ByteArrayInputStream(bytes)
            } as PhotoPreparation.Ready
        ).photo

    @Test
    fun AC41_a_picked_photo_is_prepared_stored_and_loaded_back() =
        runTest {
            val photo = prepared(SyntheticImages.jpeg(300, 200))

            val fileName = store.store(photo)
            val loaded = store.load(fileName)

            assertNotNull(loaded)
            assertEquals(300, loaded!!.width)
            assertEquals(200, loaded.height)
        }

    @Test
    fun AC43_the_stored_copy_survives_the_original_being_deleted() =
        runTest {
            val original = temporaryFolder.newFile("gallery.jpg").apply { writeBytes(SyntheticImages.jpeg(64, 64)) }
            val photo = (store.prepare { original.inputStream() } as PhotoPreparation.Ready).photo
            val fileName = store.store(photo)

            assertTrue(original.delete())

            assertNotNull(store.load(fileName))
        }

    @Test
    fun AC43_delete_removes_the_stored_file() =
        runTest {
            val fileName = store.store(prepared(SyntheticImages.jpeg(64, 64)))

            store.delete(fileName)

            assertNull(store.load(fileName))
            assertFalse(File(root, "profile/$fileName").exists())
        }

    @Test
    fun AC41_files_get_app_generated_names_inside_the_profile_directory() =
        runTest {
            val fileName = store.store(prepared(SyntheticImages.jpeg(64, 64)))

            assertTrue(Regex("photo_[0-9a-f-]+\\.jpg").matches(fileName))
            assertTrue(File(root, "profile/$fileName").isFile)
        }

    @Test
    fun AC43_names_that_are_not_generated_by_the_store_never_touch_the_file_system() =
        runTest {
            val outside = File(root, "secret.jpg").apply { writeBytes(byteArrayOf(1)) }

            store.delete("../secret.jpg")

            assertNull(store.load("../secret.jpg"))
            assertTrue(outside.exists())
        }

    @Test
    fun AC41_a_large_photo_is_downscaled_to_1024_pixels_on_the_long_edge() =
        runTest {
            val photo = prepared(SyntheticImages.jpeg(3000, 1500))

            assertEquals(1024, photo.bitmap.width)
            assertEquals(512, photo.bitmap.height)
        }

    @Test
    fun AC41_a_photo_at_the_8000_pixel_limit_is_accepted() =
        runTest {
            val photo = prepared(SyntheticImages.png(8000, 1))

            assertEquals(1024, photo.bitmap.width)
        }

    @Test
    fun AC41_a_photo_over_8000_pixels_on_a_side_is_refused() =
        runTest {
            val result = store.prepare { ByteArrayInputStream(SyntheticImages.png(8001, 1)) }

            assertEquals(PhotoPreparation.TooLarge, result)
        }

    @Test
    fun AC41_a_file_over_20_MB_is_refused() =
        runTest {
            val result = store.prepare { ByteArrayInputStream(ByteArray(PhotoStore.MAX_SOURCE_BYTES + 1)) }

            assertEquals(PhotoPreparation.TooLarge, result)
        }

    @Test
    fun AC41_something_that_is_not_an_image_is_refused() =
        runTest {
            val result = store.prepare { ByteArrayInputStream("not an image".toByteArray()) }

            assertEquals(PhotoPreparation.NotAnImage, result)
        }

    @Test
    fun AC41_a_source_that_cannot_be_opened_or_read_is_reported_as_unreadable() =
        runTest {
            val failing =
                object : InputStream() {
                    override fun read(): Int = throw IOException("boom")
                }

            assertEquals(PhotoPreparation.Unreadable, store.prepare { null })
            assertEquals(PhotoPreparation.Unreadable, store.prepare { failing })
            assertEquals(PhotoPreparation.Unreadable, store.prepare { throw SecurityException("revoked") })
        }

    @Test
    fun AC41_re_encoding_strips_exif_including_gps() =
        runTest {
            val source = SyntheticImages.jpegWithExif(40, 20, ExifInterface.ORIENTATION_NORMAL, temporaryFolder.newFolder())
            assertTrue(ExifInterface(ByteArrayInputStream(source)).getLatLong(FloatArray(2)))

            val photo = prepared(source)

            val exif = ExifInterface(ByteArrayInputStream(photo.jpegBytes))
            assertFalse(exif.getLatLong(FloatArray(2)))
        }

    @Test
    fun AC41_exif_rotation_is_applied_to_the_pixels() =
        runTest {
            val source = SyntheticImages.jpegWithExif(40, 20, ExifInterface.ORIENTATION_ROTATE_90, temporaryFolder.newFolder())

            val photo = prepared(source)

            assertEquals(20, photo.bitmap.width)
            assertEquals(40, photo.bitmap.height)
        }

    @Test
    fun AC41_every_exif_orientation_gives_a_photo_of_the_same_size_or_swapped() =
        runTest {
            val swapped =
                setOf(
                    ExifInterface.ORIENTATION_TRANSPOSE,
                    ExifInterface.ORIENTATION_ROTATE_90,
                    ExifInterface.ORIENTATION_TRANSVERSE,
                    ExifInterface.ORIENTATION_ROTATE_270,
                )
            (ExifInterface.ORIENTATION_NORMAL..ExifInterface.ORIENTATION_ROTATE_270).forEach { orientation ->
                val source = SyntheticImages.jpegWithExif(40, 20, orientation, temporaryFolder.newFolder())

                val photo = prepared(source)

                val expected = if (orientation in swapped) 20 to 40 else 40 to 20
                assertEquals("orientation $orientation", expected, photo.bitmap.width to photo.bitmap.height)
            }
        }

    @Test
    fun AC41_transparent_pixels_become_white_not_black() =
        runTest {
            val photo = prepared(SyntheticImages.transparentPng(16, 16))

            assertEquals(Color.WHITE, photo.bitmap.getPixel(8, 8))
        }

    @Test
    fun AC41_a_one_pixel_photo_is_accepted_and_not_enlarged() =
        runTest {
            val photo = prepared(SyntheticImages.png(1, 1))

            assertEquals(1, photo.bitmap.width)
            assertEquals(1, photo.bitmap.height)
        }

    @Test
    fun AC41_a_photo_exactly_1024_pixels_wide_is_not_resized() =
        runTest {
            val photo = prepared(SyntheticImages.png(1024, 10))

            assertEquals(1024, photo.bitmap.width)
            assertEquals(10, photo.bitmap.height)
        }

    @Test
    fun AC41_a_very_tall_photo_keeps_at_least_one_pixel_on_the_short_edge() =
        runTest {
            val photo = prepared(SyntheticImages.png(1, 8000))

            assertEquals(1024, photo.bitmap.height)
            assertTrue(photo.bitmap.width >= 1)
        }

    @Test
    fun AC41_a_truncated_jpeg_does_not_crash_and_writes_nothing() =
        runTest {
            val whole = SyntheticImages.jpeg(200, 200)
            val truncated = whole.copyOf(whole.size / 3)

            val result = store.prepare { ByteArrayInputStream(truncated) }

            assertFalse(result is PhotoPreparation.Unreadable)
            assertFalse(File(root, "profile").exists())
        }

    @Test
    fun AC41_an_empty_file_is_refused_as_not_an_image() =
        runTest {
            assertEquals(PhotoPreparation.NotAnImage, store.prepare { ByteArrayInputStream(ByteArray(0)) })
        }

    @Test
    fun AC41_a_file_of_exactly_20_MB_is_not_refused_for_size() =
        runTest {
            val result = store.prepare { ByteArrayInputStream(ByteArray(PhotoStore.MAX_SOURCE_BYTES)) }

            assertEquals(PhotoPreparation.NotAnImage, result)
        }

    @Test
    fun AC41_an_svg_text_file_is_refused() =
        runTest {
            val svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".toByteArray()

            assertEquals(PhotoPreparation.NotAnImage, store.prepare { ByteArrayInputStream(svg) })
        }

    @Test
    fun AC43_prepare_alone_writes_nothing_to_the_file_system() =
        runTest {
            prepared(SyntheticImages.jpeg(64, 64))

            assertFalse(File(root, "profile").exists())
        }

    @Test
    fun AC43_names_that_merely_look_like_generated_names_never_touch_the_file_system() =
        runTest {
            val victim = File(root, "victim.jpg").apply { writeBytes(byteArrayOf(1)) }
            val names =
                listOf(
                    "photo_../victim.jpg",
                    "photo_..${'\\'}victim.jpg",
                    "/photo_1.jpg",
                    "photo_1.jpg/..",
                    "",
                    "photo_1.jpg\u0000x",
                    "photo_%2e%2e.jpg",
                )

            names.forEach { name ->
                store.delete(name)
                assertNull(name, store.load(name))
            }

            assertTrue(victim.exists())
        }

    @Test
    fun AC43_deleting_a_missing_generated_name_is_harmless() =
        runTest {
            store.delete("photo_00000000-0000-0000-0000-000000000099.jpg")
        }
}
