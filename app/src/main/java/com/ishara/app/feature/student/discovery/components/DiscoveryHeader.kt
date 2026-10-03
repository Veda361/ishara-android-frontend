package com.ishara.app.feature.student.discovery.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonSize
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraCard
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.domain.model.DiscoveryQuery

/**
 * Top journey summary header for the Trip Discovery screen.
 * Clearly communicates: "Where from", "Where to", with an accessible edit action.
 */
@Composable
fun DiscoveryHeader(
    query: DiscoveryQuery,
    onChangeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    val originDisplay = query.originName ?: query.originAddress ?: "Current Location"
    val destDisplay = query.destinationName ?: query.destinationAddress ?: "Selected Destination"

    IshaaraCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Search journey: From $originDisplay to $destDisplay."
            }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SEARCH ROUTE",
                    style = typography.labelSmall,
                    color = colors.foregroundMuted
                )

                IshaaraButton(
                    text = "Edit",
                    onClick = onChangeClick,
                    variant = IshaaraButtonVariant.Text,
                    size = IshaaraButtonSize.Small,
                    modifier = Modifier.height(36.dp)
                )
            }

            // Origin Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(colors.primary, CircleShape)
                )
                Column {
                    Text(
                        text = "From",
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                    Text(
                        text = originDisplay,
                        style = typography.titleSmall,
                        color = colors.foreground,
                        maxLines = 1
                    )
                }
            }

            // Divider bar
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .width(2.dp)
                    .height(14.dp)
                    .background(colors.border)
            )

            // Destination Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(colors.accent, CircleShape)
                )
                Column {
                    Text(
                        text = "To",
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                    Text(
                        text = destDisplay,
                        style = typography.titleSmall,
                        color = colors.foreground,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
