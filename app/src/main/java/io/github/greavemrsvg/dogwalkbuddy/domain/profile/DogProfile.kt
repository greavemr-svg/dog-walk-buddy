package io.github.greavemrsvg.dogwalkbuddy.domain.profile

import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity

/** The single dog profile. [photoFileName] is an app-generated name inside the photo store, never a path. */
data class DogProfile(
    val name: String,
    val breed: String?,
    val photoFileName: String?,
    val heatSensitivity: HeatSensitivity,
)
