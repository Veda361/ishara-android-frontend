package com.ishara.app.ui.components.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ishara.app.core.designsystem.components.IshaaraButton
import com.ishara.app.core.designsystem.components.IshaaraCard
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@Composable
fun QuickActionSection(

    onDiscoverClick: () -> Unit = {},

    onTripsClick: () -> Unit = {}

) {

    IshaaraCard {

        Row(

            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement = Arrangement.spacedBy(
                IshaaraTheme.spacing.md
            )

        ) {

            IshaaraButton(

                modifier = Modifier.weight(1f),

                text = "Discover",

                onClick = onDiscoverClick

            )

            IshaaraButton(

                modifier = Modifier.weight(1f),

                text = "My Trips",

                onClick = onTripsClick

            )

        }

    }

}