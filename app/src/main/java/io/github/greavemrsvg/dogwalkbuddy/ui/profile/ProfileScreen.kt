package io.github.greavemrsvg.dogwalkbuddy.ui.profile

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.data.repo.PhotoSource
import io.github.greavemrsvg.dogwalkbuddy.data.repo.PhotoStore
import io.github.greavemrsvg.dogwalkbuddy.domain.profile.ProfileError
import io.github.greavemrsvg.dogwalkbuddy.domain.profile.ProfileValidator
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity
import io.github.greavemrsvg.dogwalkbuddy.ui.common.PrimaryButton
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitShapes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSizes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSpacing
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.MinTouchTarget

/** Test tag of the 88 dp photo circle (AC50). */
const val PROFILE_AVATAR_TAG = "profile_avatar"

/** Test tag of the decorative placeholder icon shown with no photo (AC50). */
const val PROFILE_PLACEHOLDER_TAG = "profile_placeholder"

private const val BYTES_PER_MB = 1024 * 1024
private const val OPTIONAL_MARKER = "("
private val PhotoRing = 4.dp
private val PlaceholderIconSize = 40.dp
private val PhotoRowGap = 16.dp
private val PhotoButtonGap = 4.dp
private val PhotoButtonBorder = 1.5.dp
private val PhotoIconGap = 8.dp
private val RadioCardMinHeight = 92.dp
private val RadioCardSelectedBorder = 2.dp
private val RadioCardBorder = 1.dp
private val RadioCardPadding = 14.dp
private val RadioCardGap = 10.dp
private val RadioCardNameSize = 17.sp
private val TileBorder = 1.dp
private val TileGap = 10.dp

@Composable
fun ProfileRoute(
    viewModel: ProfileViewModel,
    onOpenPrivacy: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Application context: the picked photo is read once, straight away, and this never leaks an Activity.
    val appContext = LocalContext.current.applicationContext
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) viewModel.onPhotoPicked(contentSource(appContext, uri))
        }
    ProfileScreen(
        state = state,
        onNameChange = viewModel::onNameChange,
        onBreedChange = viewModel::onBreedChange,
        onSensitivityChange = viewModel::onSensitivityChange,
        onChoosePhoto = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onRemovePhoto = viewModel::onRemovePhoto,
        onSave = viewModel::onSave,
        onOpenPrivacy = onOpenPrivacy,
        onOpenAbout = onOpenAbout,
    )
}

private fun contentSource(
    context: Context,
    uri: android.net.Uri,
) = PhotoSource { context.contentResolver.openInputStream(uri) }

/**
 * The dog tab (AC50-AC53): the photo row, the name and breed fields with live counters, the heat-sensitivity radio
 * cards, Save, and the Privacy and About tiles.
 */
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onNameChange: (String) -> Unit,
    onBreedChange: (String) -> Unit,
    onSensitivityChange: (HeatSensitivity) -> Unit,
    onChoosePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSave: () -> Unit,
    onOpenPrivacy: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
) {
    val nameFocus = remember { FocusRequester() }
    val breedFocus = remember { FocusRequester() }
    // A refused save moves focus to the first field in error, so a screen reader announces its message (AC50, AC42).
    LaunchedEffect(state.refusedSaves) {
        if (state.refusedSaves > 0) {
            if (state.nameError != null) {
                nameFocus.requestFocus()
            } else if (state.breedError != null) {
                breedFocus.requestFocus()
            }
        }
    }
    Surface(modifier = Modifier.fillMaxSize(), color = FieldKit.colors.background) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = FieldKitSpacing.gutter, vertical = FieldKitSpacing.stackGap),
            verticalArrangement = Arrangement.spacedBy(FieldKitSpacing.sectionGap),
        ) {
            // The dog tab is a tab, so it has no back control (AC11); system Back returns to Today.
            Text(
                text = stringResource(R.string.profile_title),
                style = FieldKit.typography.screenTitle,
                modifier = Modifier.semantics { heading() },
            )
            PhotoSection(state, onChoosePhoto, onRemovePhoto)
            NameField(state, onNameChange, nameFocus)
            BreedField(state, onBreedChange, breedFocus)
            SensitivityGroup(state.sensitivity, state.isLoaded, onSensitivityChange)
            PrimaryButton(
                label = stringResource(R.string.profile_save),
                onClick = onSave,
                enabled = state.isLoaded && !state.isSaving,
            )
            SaveStatus(state)
            Tiles(onOpenPrivacy, onOpenAbout)
        }
    }
}

/** The "Privacy & data" and "About the advice" tiles below Save, the dog tab's way to Privacy and About (AC53). */
@Composable
private fun Tiles(
    onOpenPrivacy: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(FieldKitSpacing.stackGap),
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
    ) {
        Tile(stringResource(R.string.profile_privacy_tile), R.drawable.ic_shield, onOpenPrivacy, Modifier.weight(1f))
        Tile(stringResource(R.string.profile_about_tile), R.drawable.ic_info, onOpenAbout, Modifier.weight(1f))
    }
}

@Composable
private fun Tile(
    label: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        onClick = onClick,
        shape = FieldKitShapes.card,
        color = FieldKit.colors.surface,
        contentColor = FieldKit.colors.onSurface,
        border = BorderStroke(TileBorder, FieldKit.colors.outlineVariant),
        modifier = modifier.fillMaxHeight().heightIn(min = MinTouchTarget),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TileGap),
            modifier = Modifier.padding(FieldKitSpacing.gutter),
        ) {
            // Decorative: the label names the tile.
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = FieldKit.colors.primary,
                modifier = Modifier.size(FieldKitSizes.iconNav),
            )
            Text(text = label, style = FieldKit.typography.label)
        }
    }
}

/**
 * The 88 dp round photo (or its placeholder) beside the photo controls (AC50), and a refused photo's message below,
 * in `errorText` (AC67).
 */
@Composable
private fun PhotoSection(
    state: ProfileUiState,
    onChoosePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    val colors = FieldKit.colors
    Column(verticalArrangement = Arrangement.spacedBy(FieldKitSpacing.stackGap), modifier = Modifier.fillMaxWidth()) {
        val photo = state.photo
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PhotoRowGap)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .testTag(PROFILE_AVATAR_TAG)
                        .size(FieldKitSizes.avatar)
                        .background(if (photo != null) colors.surface else colors.surfaceMuted, CircleShape)
                        .padding(if (photo != null) PhotoRing else 0.dp),
            ) {
                if (photo != null) {
                    Image(
                        bitmap = photo,
                        contentDescription = stringResource(R.string.profile_photo_description),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                } else {
                    // Decorative: the "No photo" caption beside it says the same.
                    Icon(
                        painter = painterResource(R.drawable.ic_nav_dog),
                        contentDescription = null,
                        tint = colors.iconMuted,
                        modifier = Modifier.size(PlaceholderIconSize).testTag(PROFILE_PLACEHOLDER_TAG),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(PhotoButtonGap)) {
                PhotoButton(
                    label = stringResource(if (photo == null) R.string.profile_choose_photo else R.string.profile_replace_photo),
                    enabled = state.isLoaded,
                    onClick = onChoosePhoto,
                )
                if (photo != null) {
                    TextButton(
                        onClick = onRemovePhoto,
                        enabled = state.isLoaded,
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.errorText),
                        modifier = Modifier.heightIn(min = MinTouchTarget).widthIn(min = MinTouchTarget),
                    ) {
                        Text(stringResource(R.string.profile_remove_photo), style = FieldKit.typography.label)
                    }
                } else {
                    Text(
                        text = stringResource(R.string.profile_no_photo),
                        color = colors.textTertiary,
                        style = FieldKit.typography.caption,
                        modifier = Modifier.padding(start = FieldKitSpacing.stackGap),
                    )
                }
            }
        }
        state.photoProblem?.let { problem ->
            Text(
                text = photoProblemMessage(problem),
                color = colors.errorText,
                style = FieldKit.typography.body,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/** "Choose photo" or "Change photo": an outlined pill with the image icon, at least 48 dp high. */
@Composable
private fun PhotoButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = FieldKit.colors
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = FieldKitShapes.pill,
        border = BorderStroke(PhotoButtonBorder, colors.controlBorder),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.surface, contentColor = colors.onSurface),
        modifier = Modifier.heightIn(min = MinTouchTarget),
    ) {
        // Decorative: the label names the control.
        Icon(
            painter = painterResource(R.drawable.ic_image),
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(FieldKitSizes.iconInline),
        )
        Spacer(Modifier.width(PhotoIconGap))
        Text(label, style = FieldKit.typography.label)
    }
}

@Composable
private fun NameField(
    state: ProfileUiState,
    onNameChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    val message = state.nameError?.let { stringResource(nameErrorText(it), ProfileValidator.NAME_MAX_LENGTH.toString()) }
    ProfileTextField(
        value = state.name,
        label = stringResource(R.string.profile_name_label),
        maxLength = ProfileValidator.NAME_MAX_LENGTH,
        errorMessage = message,
        enabled = state.isLoaded,
        focusRequester = focusRequester,
        onValueChange = onNameChange,
    )
}

@Composable
private fun BreedField(
    state: ProfileUiState,
    onBreedChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    val message = state.breedError?.let { stringResource(breedErrorText(it), ProfileValidator.BREED_MAX_LENGTH.toString()) }
    ProfileTextField(
        value = state.breed,
        label = stringResource(R.string.profile_breed_label),
        maxLength = ProfileValidator.BREED_MAX_LENGTH,
        errorMessage = message,
        enabled = state.isLoaded,
        focusRequester = focusRequester,
        onValueChange = onBreedChange,
    )
}

/**
 * The label with its bracketed part, such as "(optional)", in [muted] at weight 400 (AC51, D9). The string resource
 * is unchanged, so the label stays one text.
 */
internal fun mutedOptionalLabel(
    label: String,
    muted: Color,
): AnnotatedString {
    val split = label.indexOf(OPTIONAL_MARKER)
    if (split < 0) return AnnotatedString(label)
    return buildAnnotatedString {
        append(label.substring(0, split))
        withStyle(SpanStyle(color = muted, fontWeight = FontWeight.Normal)) { append(label.substring(split)) }
    }
}

@Composable
private fun ProfileTextField(
    value: String,
    label: String,
    maxLength: Int,
    errorMessage: String?,
    enabled: Boolean,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
) {
    val colors = FieldKit.colors
    val counter = stringResource(R.string.profile_counter, value.length, maxLength)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = { Text(mutedOptionalLabel(label, colors.textTertiary)) },
        singleLine = true,
        isError = errorMessage != null,
        shape = FieldKitShapes.input,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.controlBorder,
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                errorContainerColor = colors.surface,
                errorSupportingTextColor = colors.errorText,
            ),
        // The existing error text at the start and the live counter, for example "7 / 30", at the end (AC51, D9).
        supportingText = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FieldKitSpacing.stackGap)) {
                Text(text = errorMessage.orEmpty(), modifier = Modifier.weight(1f))
                Text(text = counter, color = colors.textTertiary)
            }
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = FieldKitSizes.inputHeight)
                .focusRequester(focusRequester)
                .semantics(mergeDescendants = true) { if (errorMessage != null) error(errorMessage) },
    )
}

/**
 * Two radio cards in a radio group (AC52). Each card is one selectable node with role RadioButton, so its name, its
 * bands and its selected state are announced together, for Normal as for Sensitive (MVP finding F1). The selected
 * card has a `primary` border and a filled radio dot as well as its fill, so colour is never the only difference.
 */
@Composable
private fun SensitivityGroup(
    selected: HeatSensitivity,
    enabled: Boolean,
    onSelect: (HeatSensitivity) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(FieldKitSpacing.stackGap)) {
        Text(
            text = stringResource(R.string.profile_sensitivity_label),
            style = FieldKit.typography.factorValue,
            modifier = Modifier.semantics { heading() },
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(RadioCardGap),
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max).selectableGroup(),
        ) {
            HeatSensitivity.entries.forEach { option ->
                RadioCard(
                    option = option,
                    selected = option == selected,
                    enabled = enabled,
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            text = stringResource(R.string.profile_sensitivity_hint),
            color = FieldKit.colors.onSurfaceVariant,
            style = FieldKit.typography.body,
        )
    }
}

@Composable
private fun RadioCard(
    option: HeatSensitivity,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = FieldKit.colors
    val shape = FieldKitShapes.button
    Row(
        horizontalArrangement = Arrangement.spacedBy(RadioCardGap),
        modifier =
            modifier
                .fillMaxHeight()
                .heightIn(min = RadioCardMinHeight)
                .clip(shape)
                .background(if (selected) colors.primaryContainer else colors.surface, shape)
                .border(
                    width = if (selected) RadioCardSelectedBorder else RadioCardBorder,
                    color = if (selected) colors.primary else colors.outlineVariant,
                    shape = shape,
                ).selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
                .padding(RadioCardPadding),
    ) {
        // The card is the control; the dot only draws its state.
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(selectedColor = colors.primary, unselectedColor = colors.controlBorder),
        )
        Column(verticalArrangement = Arrangement.spacedBy(PhotoButtonGap)) {
            Text(
                text = stringResource(sensitivityText(option)),
                style = FieldKit.typography.factorValue.copy(fontSize = RadioCardNameSize, fontWeight = FontWeight.ExtraBold),
            )
            Text(
                text = stringResource(sensitivityBands(option)),
                color = colors.onSurfaceVariant,
                style = FieldKit.typography.label,
            )
        }
    }
}

private fun sensitivityBands(option: HeatSensitivity) =
    when (option) {
        HeatSensitivity.NORMAL -> R.string.profile_band_normal
        HeatSensitivity.SENSITIVE -> R.string.profile_band_sensitive
    }

@Composable
private fun SaveStatus(state: ProfileUiState) {
    when {
        state.loadFailed && !state.justSaved && !state.saveFailed ->
            Text(
                text = stringResource(R.string.profile_load_failed),
                color = FieldKit.colors.errorText,
                style = FieldKit.typography.bodyLarge,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        state.justSaved ->
            Text(
                text = stringResource(R.string.profile_saved),
                style = FieldKit.typography.bodyLarge,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        state.saveFailed ->
            Text(
                text = stringResource(R.string.profile_save_failed),
                color = FieldKit.colors.errorText,
                style = FieldKit.typography.bodyLarge,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
    }
}

private fun sensitivityText(option: HeatSensitivity) =
    when (option) {
        HeatSensitivity.NORMAL -> R.string.profile_sensitivity_normal
        HeatSensitivity.SENSITIVE -> R.string.profile_sensitivity_sensitive
    }

@Composable
private fun photoProblemMessage(problem: PhotoProblem) =
    when (problem) {
        PhotoProblem.TOO_LARGE ->
            stringResource(
                R.string.profile_photo_too_large,
                PhotoStore.MAX_SOURCE_BYTES / BYTES_PER_MB,
                PhotoStore.MAX_SOURCE_EDGE_PX,
            )
        PhotoProblem.NOT_AN_IMAGE -> stringResource(R.string.profile_photo_not_an_image)
        PhotoProblem.UNREADABLE -> stringResource(R.string.profile_photo_unreadable)
    }

private fun nameErrorText(error: ProfileError) =
    when (error) {
        ProfileError.NAME_INVALID_CHARACTERS -> R.string.profile_error_name_characters
        ProfileError.NAME_TOO_LONG -> R.string.profile_error_name_too_long
        else -> R.string.profile_error_name_required
    }

private fun breedErrorText(error: ProfileError) =
    when (error) {
        ProfileError.BREED_INVALID_CHARACTERS -> R.string.profile_error_breed_characters
        else -> R.string.profile_error_breed_too_long
    }
