package io.github.greavemrsvg.dogwalkbuddy.ui.check

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.ui.common.PermissionExplainer

/**
 * Today without approximate location (AC30, AC31, D5): why the check needs it and what is sent, then "Allow
 * approximate location", or "Open settings" when the system will not ask again ([blocked]), and the "How your data
 * is used" link to Privacy. No weather request is made in this state.
 */
@Composable
fun LocationExplainer(
    blocked: Boolean,
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PermissionExplainer(
        illustrationIcon = R.drawable.ic_my_location,
        heading = stringResource(R.string.check_needs_location),
        body = stringResource(R.string.check_location_body),
        bullets =
            listOf(
                stringResource(R.string.check_location_bullet_rounded),
                stringResource(R.string.check_location_bullet_when),
                stringResource(R.string.check_location_bullet_where),
            ),
        buttonLabel = stringResource(if (blocked) R.string.check_open_settings else R.string.check_grant_location),
        onButton = if (blocked) onOpenSettings else onGrantPermission,
        linkLabel = stringResource(R.string.check_how_data_used),
        onLink = onOpenPrivacy,
        modifier = modifier,
    )
}
