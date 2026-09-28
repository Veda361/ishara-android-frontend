package com.ishara.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.ishara.app.core.designsystem.components.IshaaraSearchField
import com.ishara.app.ui.theme.IsharaTheme

@Composable
fun SearchDestinationCard() {

    var query by rememberSaveable {
        mutableStateOf("")
    }

    IshaaraSearchField(
        query = query,
        onQueryChange = { query = it }
    )
}

@Preview(showBackground = true)
@Composable
private fun SearchDestinationCardPreview() {
    IsharaTheme {
        SearchDestinationCard()
    }
}