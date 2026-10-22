package io.github.greavemrsvg.dogwalkbuddy.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSizes

private val BannerShape = RoundedCornerShape(16.dp)

/**
 * A muted information banner: a decorative icon, then [text] at 13 sp. [lead], when given, comes first in bold and
 * is separated from [text] by a space (design, "Shared components").
 */
@Composable
fun InfoBanner(
    @DrawableRes icon: Int,
    text: String,
    modifier: Modifier = Modifier,
    lead: String? = null,
) {
    val colors = FieldKit.colors
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.surfaceMuted, BannerShape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(FieldKitSizes.iconInline),
        )
        Text(
            text =
                buildAnnotatedString {
                    if (lead != null) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(lead) }
                        append(' ')
                    }
                    append(text)
                },
            style = FieldKit.typography.body.copy(fontSize = 13.sp, lineHeight = 18.sp),
            color = colors.onSurface,
        )
    }
}
