package com.ishara.app.feature.rating.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonSize
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraCard
import com.ishara.app.core.designsystem.component.IshaaraLoadingState
import com.ishara.app.core.designsystem.component.IshaaraTextField
import com.ishara.app.core.designsystem.theme.IshaaraPalette
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.feature.rating.RatingUiState
import com.ishara.app.feature.rating.RatingViewModel

/**
 * Production Dialog for Post-Ride Rating and Review.
 * Displays 1-5 star selector, optional review input (max 500 chars), and submission feedback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRideRatingDialog(
    viewModel: RatingViewModel,
    onDismiss: () -> Unit,
    onSubmittedSuccessfully: () -> Unit = onDismiss,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(IshaaraTheme.shapes.sm)
                .background(colors.surfaceElevated)
                .border(IshaaraTheme.borders.thin, colors.border, IshaaraTheme.shapes.sm)
                .padding(spacing.lg)
        ) {
            Text(
                text = "Rate Your Trip",
                style = typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.foreground
            )
            Spacer(modifier = Modifier.height(spacing.md))
        when (val state = uiState) {
            is RatingUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.lg),
                    contentAlignment = Alignment.Center
                ) {
                    IshaaraLoadingState(message = "Checking trip rating status...")
                }
            }

            is RatingUiState.Ineligible -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    Text(
                        text = if (state.alreadyRated) "Rating Already Submitted" else "Rating Not Available",
                        style = typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.foreground,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = state.reason,
                        style = typography.bodyMedium,
                        color = colors.foregroundMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(spacing.sm))

                    IshaaraButton(
                        text = "Done",
                        onClick = onDismiss,
                        variant = IshaaraButtonVariant.Primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            is RatingUiState.Form -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    Text(
                        text = "How was your ride experience with your driver?",
                        style = typography.bodyMedium,
                        color = colors.foregroundMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Interactive Star Rating Bar (1 to 5)
                    StarRatingBar(
                        currentScore = state.score,
                        onScoreSelected = { viewModel.onScoreChanged(it) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Optional Review TextField
                    Column(modifier = Modifier.fillMaxWidth()) {
                        IshaaraTextField(
                            value = state.review,
                            onValueChange = { viewModel.onReviewChanged(it) },
                            label = "Add a written note (optional)",
                            placeholder = "Tell us about vehicle cleanliness, driver professionalism...",
                            singleLine = false,
                            maxLines = 4,
                            errorText = state.validationError,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = spacing.xxs),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "${state.reviewCharacterCount} / 500",
                                style = typography.labelSmall,
                                color = if (state.isReviewLengthValid) colors.foregroundSubtle else colors.danger
                            )
                        }
                    }

                    // Server Error Feedback
                    if (!state.serverError.isNullOrBlank()) {
                        IshaaraCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = state.serverError,
                                style = typography.bodySmall,
                                color = colors.danger,
                                modifier = Modifier.padding(spacing.sm)
                            )
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        IshaaraButton(
                            text = "Skip",
                            onClick = onDismiss,
                            variant = IshaaraButtonVariant.Outlined,
                            size = IshaaraButtonSize.Medium,
                            modifier = Modifier.weight(1f),
                            enabled = !state.isSubmitting
                        )

                        IshaaraButton(
                            text = if (state.isSubmitting) "Submitting..." else "Submit Rating",
                            onClick = { viewModel.submitRating() },
                            variant = IshaaraButtonVariant.Primary,
                            size = IshaaraButtonSize.Medium,
                            modifier = Modifier.weight(1f),
                            enabled = state.canSubmit,
                            loading = state.isSubmitting
                        )
                    }
                }
            }

            is RatingUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(IshaaraPalette.Emerald100),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "★",
                            fontSize = 32.sp,
                            color = IshaaraPalette.Emerald500
                        )
                    }

                    Text(
                        text = "Thank You!",
                        style = typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.foreground,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Your rating has been recorded and helps maintain safety and service quality across the Ishaara mobility network.",
                        style = typography.bodyMedium,
                        color = colors.foregroundMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(spacing.sm))

                    IshaaraButton(
                        text = "Done",
                        onClick = onSubmittedSuccessfully,
                        variant = IshaaraButtonVariant.Primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

}

/**
 * Accessible 5-star rating selector.
 */
@Composable
fun StarRatingBar(
    currentScore: Int,
    onScoreSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = IshaaraTheme.spacing

    Row(
        modifier = modifier.padding(vertical = spacing.xs),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (star in 1..5) {
            val isFilled = star <= currentScore
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onScoreSelected(star) }
                    .semantics {
                        role = Role.Button
                        contentDescription = "$star stars"
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isFilled) "★" else "☆",
                    fontSize = 32.sp,
                    color = if (isFilled) IshaaraPalette.Amber500 else IshaaraPalette.Neutral400
                )
            }
        }
    }
}
