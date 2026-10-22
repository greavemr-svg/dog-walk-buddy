package io.github.greavemrsvg.dogwalkbuddy.ui.nav

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitShapes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSizes

/** Test tag of the Today nav item. */
const val NAV_TODAY_TAG = "nav_today"

/** Test tag of the Walks nav item. */
const val NAV_WALKS_TAG = "nav_walks"

/** Test tag of the dog nav item, whose label is the saved dog name or "Dog". */
const val NAV_DOG_TAG = "nav_dog"

/** Suffix of the test tag on each nav item's icon, which is decorative: the item's tag plus this suffix. */
const val NAV_ICON_TAG_SUFFIX = "_icon"

/** Test tag of the dog nav item's label text, the one label that may be shortened with "…" (AC60). */
const val NAV_DOG_LABEL_TAG = "nav_dog_label"

private val ItemMinWidth = 80.dp
private val ItemMinHeight = 56.dp
private val ItemGap = 4.dp
private val BarSidePadding = 8.dp
private val BarBottomPadding = 6.dp
private val TopBorder = 1.dp

/**
 * The bottom navigation bar (AC9, AC60). A custom row rather than Material 3 `NavigationBar`, because that has a
 * fixed height and the "Today", "Walks" and "Dog" labels must be free to wrap at 200 % font size. The dog label
 * is [dogName] on one line, shortened with "…" when it does not fit; its spoken text is always the full name.
 */
@Composable
fun FieldKitNavBar(
    selectedRoute: String?,
    dogName: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().background(FieldKit.colors.surface)) {
        HorizontalDivider(thickness = TopBorder, color = FieldKit.colors.outlineVariant)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .heightIn(min = FieldKitSizes.bottomNavHeight)
                    .padding(start = BarSidePadding, end = BarSidePadding, bottom = BarBottomPadding)
                    .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(
                label = stringResource(R.string.nav_today),
                icon = R.drawable.ic_nav_today,
                selected = selectedRoute == Routes.HOME,
                tag = NAV_TODAY_TAG,
                onClick = { onSelect(Routes.HOME) },
            )
            NavItem(
                label = stringResource(R.string.nav_walks),
                icon = R.drawable.ic_nav_walks,
                selected = selectedRoute == Routes.HISTORY,
                tag = NAV_WALKS_TAG,
                onClick = { onSelect(Routes.HISTORY) },
            )
            NavItem(
                label = dogName ?: stringResource(R.string.nav_dog),
                icon = R.drawable.ic_nav_dog,
                selected = selectedRoute == Routes.PROFILE,
                tag = NAV_DOG_TAG,
                onClick = { onSelect(Routes.PROFILE) },
                singleLine = dogName != null,
                labelTag = NAV_DOG_LABEL_TAG,
            )
        }
    }
}

@Composable
private fun RowScope.NavItem(
    label: String,
    @DrawableRes icon: Int,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    singleLine: Boolean = false,
    labelTag: String? = null,
) {
    val colors = FieldKit.colors
    val contentColor = if (selected) colors.primaryPressed else colors.onSurfaceVariant
    Column(
        modifier =
            Modifier
                .weight(1f)
                .widthIn(min = ItemMinWidth)
                .heightIn(min = ItemMinHeight)
                .testTag(tag)
                .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ItemGap, Alignment.CenterVertically),
    ) {
        Box(
            modifier =
                Modifier
                    .testTag(tag + NAV_ICON_TAG_SUFFIX)
                    .size(FieldKitSizes.navIndicatorWidth, FieldKitSizes.navIndicatorHeight)
                    .background(if (selected) colors.navIndicator else Color.Transparent, FieldKitShapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            // Decorative: the label beside it names the item.
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(FieldKitSizes.iconNav),
            )
        }
        Text(
            text = label,
            style = if (selected) FieldKit.typography.navLabelActive else FieldKit.typography.navLabel,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            overflow = if (singleLine) TextOverflow.Ellipsis else TextOverflow.Clip,
            modifier = if (labelTag != null) Modifier.testTag(labelTag) else Modifier,
        )
    }
}
