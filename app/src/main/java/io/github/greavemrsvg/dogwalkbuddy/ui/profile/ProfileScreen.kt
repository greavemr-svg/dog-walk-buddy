package io.github.greavemrsvg.dogwalkbuddy.ui.profile

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.data.repo.PhotoSource
import io.github.greavemrsvg.dogwalkbuddy.domain.profile.ProfileError
import io.github.greavemrsvg.dogwalkbuddy.domain.profile.ProfileValidator
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity

/** Minimum touch target (AC51). */
val MinTouchTarget = 48.dp
private val SaveButtonHeight = 56.dp
private val PhotoSize = 160.dp

@Composable
fun ProfileRoute(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
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
        onBack = onBack,
    )
}

private fun contentSource(
    context: Context,
    uri: android.net.Uri,
) = PhotoSource { context.contentResolver.openInputStream(uri) }

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onNameChange: (String) -> Unit,
    onBreedChange: (String) -> Unit,
    onSensitivityChange: (HeatSensitivity) -> Unit,
    onChoosePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.profile_back))
            }
            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            PhotoSection(state, onChoosePhoto, onRemovePhoto)
            NameField(state, onNameChange)
            BreedField(state, onBreedChange)
            SensitivityGroup(state.sensitivity, onSensitivityChange)
            Button(
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().heightIn(min = SaveButtonHeight),
            ) {
                Text(stringResource(R.string.profile_save))
            }
            SaveStatus(state)
        }
    }
}

@Composable
private fun PhotoSection(
    state: ProfileUiState,
    onChoosePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        val photo = state.photo
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = stringResource(R.string.profile_photo_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(PhotoSize).clip(MaterialTheme.shapes.medium).align(Alignment.CenterHorizontally),
            )
        } else {
            Box(modifier = Modifier.size(PhotoSize).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.profile_no_photo), style = MaterialTheme.typography.bodyLarge)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onChoosePhoto, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(if (photo == null) R.string.profile_choose_photo else R.string.profile_replace_photo))
            }
            if (photo != null) {
                OutlinedButton(onClick = onRemovePhoto, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                    Text(stringResource(R.string.profile_remove_photo))
                }
            }
        }
        state.photoProblem?.let { problem ->
            Text(
                text = stringResource(photoProblemText(problem)),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun NameField(
    state: ProfileUiState,
    onNameChange: (String) -> Unit,
) {
    val message = state.nameError?.let { stringResource(nameErrorText(it), ProfileValidator.NAME_MAX_LENGTH.toString()) }
    ProfileTextField(
        value = state.name,
        label = stringResource(R.string.profile_name_label),
        errorMessage = message,
        onValueChange = onNameChange,
    )
}

@Composable
private fun BreedField(
    state: ProfileUiState,
    onBreedChange: (String) -> Unit,
) {
    val message = state.breedError?.let { stringResource(breedErrorText(it), ProfileValidator.BREED_MAX_LENGTH.toString()) }
    ProfileTextField(
        value = state.breed,
        label = stringResource(R.string.profile_breed_label),
        errorMessage = message,
        onValueChange = onBreedChange,
    )
}

@Composable
private fun ProfileTextField(
    value: String,
    label: String,
    errorMessage: String?,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = errorMessage != null,
        supportingText = errorMessage?.let { { Text(it) } },
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget)
                .semantics(mergeDescendants = true) { if (errorMessage != null) error(errorMessage) },
    )
}

@Composable
private fun SensitivityGroup(
    selected: HeatSensitivity,
    onSelect: (HeatSensitivity) -> Unit,
) {
    Column(modifier = Modifier.selectableGroup()) {
        Text(
            text = stringResource(R.string.profile_sensitivity_label),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        HeatSensitivity.entries.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = MinTouchTarget)
                        .selectable(selected = option == selected, role = Role.RadioButton, onClick = { onSelect(option) }),
            ) {
                RadioButton(selected = option == selected, onClick = null)
                Text(
                    text = stringResource(sensitivityText(option)),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
        Text(stringResource(R.string.profile_sensitivity_hint), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SaveStatus(state: ProfileUiState) {
    when {
        state.justSaved -> Text(stringResource(R.string.profile_saved), style = MaterialTheme.typography.bodyLarge)
        state.saveFailed ->
            Text(
                text = stringResource(R.string.profile_save_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
            )
    }
}

private fun sensitivityText(option: HeatSensitivity) =
    when (option) {
        HeatSensitivity.NORMAL -> R.string.profile_sensitivity_normal
        HeatSensitivity.SENSITIVE -> R.string.profile_sensitivity_sensitive
    }

private fun photoProblemText(problem: PhotoProblem) =
    when (problem) {
        PhotoProblem.TOO_LARGE -> R.string.profile_photo_too_large
        PhotoProblem.NOT_AN_IMAGE -> R.string.profile_photo_not_an_image
        PhotoProblem.UNREADABLE -> R.string.profile_photo_unreadable
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
