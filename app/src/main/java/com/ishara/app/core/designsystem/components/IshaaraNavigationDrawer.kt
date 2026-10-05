package com.ishara.app.core.designsystem.components


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@Composable
fun IshaaraNavigationDrawer(

    onHomeClick: () -> Unit,

    onDiscoverClick: () -> Unit,

    onTripsClick: () -> Unit,

    onProfileClick: () -> Unit,

    onDriverModeClick: () -> Unit

) {

    IshaaraCard(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.82f)
    ) {

        Column(
            modifier = Modifier.padding(IshaaraTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(IshaaraTheme.spacing.md)
        ) {

            Spacer(modifier = Modifier.height(IshaaraTheme.spacing.xl))

            IshaaraMenuItem(
                title = "Home",
                subtitle = "Dashboard",
                onClick = onHomeClick
            )

            IshaaraMenuItem(
                title = "Discover",
                subtitle = "Search rides",
                onClick = onDiscoverClick
            )

            IshaaraMenuItem(
                title = "Trips",
                subtitle = "Ride history",
                onClick = onTripsClick
            )

            IshaaraMenuItem(
                title = "Profile",
                subtitle = "Your account",
                onClick = onProfileClick
            )

            Spacer(modifier = Modifier.weight(1f))

            IshaaraMenuItem(
                title = "Driver Mode",
                subtitle = "Switch to driving",
                onClick = onDriverModeClick
            )
        }
    }

}