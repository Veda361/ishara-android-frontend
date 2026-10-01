package com.ishara.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ishara.app.core.designsystem.components.IshaaraButton
import com.ishara.app.core.designsystem.components.IshaaraCard
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@Composable
fun LogoutCard() {

    IshaaraCard {

        IshaaraButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Log Out",
            onClick = {}
        )

    }

}

@Preview(showBackground = true)
@Composable
private fun LogoutCardPreview() {
    IshaaraTheme {
        LogoutCard()
    }
}