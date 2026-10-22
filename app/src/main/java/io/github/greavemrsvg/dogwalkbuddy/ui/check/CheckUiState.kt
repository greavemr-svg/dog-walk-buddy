package io.github.greavemrsvg.dogwalkbuddy.ui.check

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.domain.check.FactorCell
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HourRating
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Rating
import io.github.greavemrsvg.dogwalkbuddy.domain.window.BestWindow

/** What the check card shows. The ViewModel produces exactly one of these at a time. */
sealed interface CheckUiState {
    /** The check has not produced anything yet, or a refresh is running. */
    data object Loading : CheckUiState

    /** Approximate location is not granted. No weather request is made (AC25). */
    data object NeedsLocationPermission : CheckUiState

    /** Approximate location was refused and the system will not ask again: only Settings can grant it (AC25). */
    data object LocationPermissionBlocked : CheckUiState

    /** No usable result: no rating is shown, only a retry (AC15, AC22). */
    data object CantCheck : CheckUiState

    /**
     * A rated result. [ageMinutes] is non-null when the result is a stored one, or old enough to label, so the
     * screen can say how old it is (AC21). [usingDefaultSensitivity] is true when there is no dog profile (AC45).
     * [hours] are the rated hours from the current one, [factors] the four factor cells (AC17). [isCached] shows
     * the banner and [isOffline] its "You're offline." lead (AC21). [dogName] and [sensitivity] come from the
     * profile, for the title and the heat-sensitive line (AC20, AC24).
     */
    data class Rated(
        val current: HourRating,
        val window: BestWindow,
        val ageMinutes: Long?,
        val usingDefaultSensitivity: Boolean,
        val hours: List<HourRating>,
        val factors: List<FactorCell>,
        val isCached: Boolean,
        val isOffline: Boolean,
        val dogName: String?,
        val sensitivity: HeatSensitivity,
    ) : CheckUiState
}

@StringRes
fun Rating.labelRes(): Int =
    when (this) {
        Rating.GO -> R.string.rating_go
        Rating.CAUTION -> R.string.rating_caution
        Rating.AVOID -> R.string.rating_avoid
    }

/** Each rating has its own icon, so colour is never the only difference (AC53). */
@DrawableRes
fun Rating.iconRes(): Int =
    when (this) {
        Rating.GO -> R.drawable.ic_rating_go
        Rating.CAUTION -> R.drawable.ic_rating_caution
        Rating.AVOID -> R.drawable.ic_rating_avoid
    }
