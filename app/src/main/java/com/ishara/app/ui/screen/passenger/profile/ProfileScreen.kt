package com.ishara.app.ui.screen.passenger.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ishara.app.ui.components.common.LogoutCard
import com.ishara.app.ui.components.profile.ProfileHeaderCard
import com.ishara.app.ui.components.profile.SavedPlacesCard
import com.ishara.app.ui.components.profile.SettingsCard

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        ProfileHeaderCard()

        SavedPlacesCard()

        SettingsCard()

        LogoutCard(
            onLogout = { viewModel.logout() }
        )
    }
}
