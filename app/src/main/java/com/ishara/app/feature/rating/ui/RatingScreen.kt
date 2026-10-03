package com.ishara.app.feature.rating.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.ishara.app.core.designsystem.component.IshaaraTopBar
import com.ishara.app.core.designsystem.theme.IshaaraPalette
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.feature.rating.RatingUiState
import com.ishara.app.feature.rating.RatingViewModel

/**
 * Full-screen Post-Ride Rating Screen.
 */
@Composable
fun RatingScreen(
    viewModel: RatingViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            IshaaraTopBar(
                title = "Rate Trip",
                onBackClick = onNavigateBack
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is RatingUiState.Loading -> {
                    IshaaraLoadingState(message = "Verifying rating eligibility...")
                }

                is RatingUiState.Ineligible -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.xl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (state.alreadyRated) "Rating Already Submitted" else "Rating Unavailable",
                            style = typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.foreground,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(spacing.sm))

                        Text(
                            text = state.reason,
                            style = typography.bodyMedium,
                            color = colors.foregroundMuted,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(spacing.xl))

                        IshaaraButton(
                            text = "Back to Home",
                            onClick = onNavigateBack,
                            variant = IshaaraButtonVariant.Primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                is RatingUiState.Form -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(spacing.lg)
                    ) {
                        IshaaraCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(spacing.md),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(spacing.md)
                            ) {
                                Text(
                                    text = "How was your driver?",
                                    style = typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.foreground
                                )

                                Text(
                                    text = "Your honest feedback helps us keep the Ishaara transit community safe and reliable.",
                                    style = typography.bodySmall,
                                    color = colors.foregroundMuted,
                                    textAlign = TextAlign.Center
                                )

                                StarRatingBar(
                                    currentScore = state.score,
                                    onScoreSelected = { viewModel.onScoreChanged(it) }
                                )
                            }
                        }

                        IshaaraCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(spacing.md),
                                verticalArrangement = Arrangement.spacedBy(spacing.xs)
                            ) {
                                Text(
                                    text = "Leave a Review (Optional)",
                                    style = typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.foreground
                                )

                                IshaaraTextField(
                                    value = state.review,
                                    onValueChange = { viewModel.onReviewChanged(it) },
                                    label = "Written Feedback",
                                    placeholder = "Share details about punctuality, driving safety, vehicle cleanliness...",
                                    singleLine = false,
                                    maxLines = 5,
                                    errorText = state.validationError,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "${state.reviewCharacterCount} / 500",
                                        style = typography.labelSmall,
                                        color = if (state.isReviewLengthValid) colors.foregroundSubtle else colors.danger
                                    )
                                }
                            }
                        }

                        if (!state.serverError.isNullOrBlank()) {
                            IshaaraCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = state.serverError,
                                    style = typography.bodySmall,
                                    color = colors.danger,
                                    modifier = Modifier.padding(spacing.md)
                                )
                            }
                        }

                        IshaaraButton(
                            text = if (state.isSubmitting) "Submitting Rating..." else "Submit Rating",
                            onClick = { viewModel.submitRating() },
                            enabled = state.canSubmit,
                            loading = state.isSubmitting,
                            variant = IshaaraButtonVariant.Primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                is RatingUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.xl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(IshaaraPalette.Emerald100),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "★",
                                fontSize = 36.sp,
                                color = IshaaraPalette.Emerald500
                            )
                        }

                        Spacer(modifier = Modifier.height(spacing.lg))

                        Text(
                            text = "Thank You!",
                            style = typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.foreground,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(spacing.xs))

                        Text(
                            text = "Your rating has been submitted successfully.",
                            style = typography.bodyMedium,
                            color = colors.foregroundMuted,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(spacing.xl))

                        IshaaraButton(
                            text = "Done",
                            onClick = onNavigateBack,
                            variant = IshaaraButtonVariant.Primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
