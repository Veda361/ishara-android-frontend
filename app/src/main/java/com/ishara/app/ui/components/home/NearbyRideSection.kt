package com.ishara.app.ui.components.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@Composable
fun NearbyRideSection() {

    Column(

        verticalArrangement = Arrangement.spacedBy(12.dp)

    ) {

        Text(

            text = "Nearby rides",

            style = IshaaraTheme.typography.headlineSmall

        )

        NearbyRideCard()

        NearbyRideCard()

        NearbyRideCard()

    }

}