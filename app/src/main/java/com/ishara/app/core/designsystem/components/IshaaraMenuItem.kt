package com.ishara.app.core.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@Composable
fun IshaaraMenuItem(

    title: String,

    subtitle: String,

    onClick: () -> Unit

) {

    IshaaraCard(

        modifier = Modifier.fillMaxWidth(),

        onClick = onClick

    ) {

        Column {

            Text(

                text = title,

                style = IshaaraTheme.typography.titleMedium

            )

            Text(

                text = subtitle,

                style = IshaaraTheme.typography.bodySmall,

                color = IshaaraTheme.colors.foregroundMuted

            )

        }

    }

}