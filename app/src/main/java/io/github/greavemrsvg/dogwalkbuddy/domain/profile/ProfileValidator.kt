package io.github.greavemrsvg.dogwalkbuddy.domain.profile

/** Text the user typed into the profile form. [breed] is optional. */
data class ProfileInput(
    val name: String,
    val breed: String? = null,
)

enum class ProfileError {
    NAME_REQUIRED,
    NAME_TOO_LONG,
    NAME_INVALID_CHARACTERS,
    BREED_TOO_LONG,
    BREED_INVALID_CHARACTERS,
}

sealed interface ValidationResult {
    /** [input] is trimmed, and a blank breed is null. */
    data class Valid(
        val input: ProfileInput,
    ) : ValidationResult

    data class Invalid(
        val nameError: ProfileError?,
        val breedError: ProfileError?,
    ) : ValidationResult
}

/** Profile limits from the spec (AC42): name required, 1-30 characters; breed optional, up to 50. */
object ProfileValidator {
    const val NAME_MAX_LENGTH = 30
    const val BREED_MAX_LENGTH = 50

    fun validate(input: ProfileInput): ValidationResult {
        val name = input.name.trim()
        val breed = input.breed?.trim()?.takeIf { it.isNotEmpty() }
        val nameError =
            when {
                name.isEmpty() -> ProfileError.NAME_REQUIRED
                hasControlCharacter(name) -> ProfileError.NAME_INVALID_CHARACTERS
                length(name) > NAME_MAX_LENGTH -> ProfileError.NAME_TOO_LONG
                else -> null
            }
        val breedError =
            when {
                breed == null -> null
                hasControlCharacter(breed) -> ProfileError.BREED_INVALID_CHARACTERS
                length(breed) > BREED_MAX_LENGTH -> ProfileError.BREED_TOO_LONG
                else -> null
            }
        return if (nameError == null && breedError == null) {
            ValidationResult.Valid(ProfileInput(name, breed))
        } else {
            ValidationResult.Invalid(nameError, breedError)
        }
    }

    // Counted in characters the user sees, so an emoji is one, not two UTF-16 units.
    private fun length(text: String): Int = text.codePointCount(0, text.length)

    private fun hasControlCharacter(text: String): Boolean = text.any { Character.isISOControl(it) }
}
