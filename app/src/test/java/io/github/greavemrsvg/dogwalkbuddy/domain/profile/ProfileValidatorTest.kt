package io.github.greavemrsvg.dogwalkbuddy.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileValidatorTest {
    private fun invalid(
        name: String,
        breed: String? = null,
    ) = ProfileValidator.validate(ProfileInput(name, breed)) as ValidationResult.Invalid

    private fun valid(
        name: String,
        breed: String? = null,
    ) = (ProfileValidator.validate(ProfileInput(name, breed)) as ValidationResult.Valid).input

    @Test
    fun AC42_empty_name_is_refused() {
        assertEquals(ProfileError.NAME_REQUIRED, invalid("").nameError)
    }

    @Test
    fun AC42_blank_name_is_refused_as_empty() {
        assertEquals(ProfileError.NAME_REQUIRED, invalid("   ").nameError)
    }

    @Test
    fun AC42_name_of_30_characters_is_accepted_and_31_is_refused() {
        assertEquals("a".repeat(30), valid("a".repeat(30)).name)
        assertEquals(ProfileError.NAME_TOO_LONG, invalid("a".repeat(31)).nameError)
    }

    @Test
    fun AC42_breed_of_50_characters_is_accepted_and_51_is_refused() {
        assertEquals("b".repeat(50), valid("Biscuit", "b".repeat(50)).breed)
        assertEquals(ProfileError.BREED_TOO_LONG, invalid("Biscuit", "b".repeat(51)).breedError)
    }

    @Test
    fun AC42_missing_or_blank_breed_is_allowed_and_stored_as_null() {
        assertNull(valid("Biscuit", null).breed)
        assertNull(valid("Biscuit", "  ").breed)
    }

    @Test
    fun AC42_both_fields_report_their_own_error() {
        val result = invalid("", "b".repeat(51))

        assertEquals(ProfileError.NAME_REQUIRED, result.nameError)
        assertEquals(ProfileError.BREED_TOO_LONG, result.breedError)
    }

    @Test
    fun AC42_surrounding_whitespace_is_trimmed_before_the_length_check() {
        assertEquals("Biscuit", valid("  Biscuit  ").name)
        assertEquals("a".repeat(30), valid(" " + "a".repeat(30) + " ").name)
    }

    @Test
    fun AC42_length_counts_characters_the_user_sees() {
        val thirtyEmoji = "🐕".repeat(30)

        assertEquals(thirtyEmoji, valid(thirtyEmoji).name)
    }

    @Test
    fun AC42_control_characters_are_refused_in_name_and_breed() {
        assertEquals(ProfileError.NAME_INVALID_CHARACTERS, invalid("Bis\ncuit").nameError)
        assertEquals(ProfileError.NAME_INVALID_CHARACTERS, invalid("Bis\u0000cuit").nameError)
        assertEquals(ProfileError.BREED_INVALID_CHARACTERS, invalid("Biscuit", "Cock\tapoo").breedError)
    }

    @Test
    fun AC41_the_spec_example_is_valid() {
        val input = valid("Biscuit", "Cockapoo")

        assertEquals(ProfileInput("Biscuit", "Cockapoo"), input)
    }
}
