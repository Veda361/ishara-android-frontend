package com.ishara.app.feature.student.voice

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ishara.app.R
import com.ishara.app.core.designsystem.component.IshaaraBadge
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonSize
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraCard
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.domain.model.DiscoveryQuery
import com.ishara.app.domain.model.SearchResultLocation
import com.ishara.app.domain.model.VoiceTripDraft

/**
 * Production Voice Trip Creation Screen for Student/Passenger.
 * Adheres strictly to ISHAARA Design System:
 * - Clean, distraction-free aesthetic with accessible 56dp touch targets
 * - Clear human language without cyber/AI jargon
 * - Explicit review card requiring student confirmation before trip discovery
 * - Seamless handoff to Phase 06 Discovery engine
 */
@Composable
fun VoiceTripScreen(
    viewModel: VoiceTripViewModel,
    onConfirmed: (DiscoveryQuery) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    VoiceTripContent(
        uiState = uiState,
        onStartListening = { viewModel.startListening() },
        onStopListening = { viewModel.stopListening() },
        onCancel = {
            viewModel.cancelSession()
            onNavigateBack()
        },
        onConfirm = { viewModel.onConfirmDraft(onConfirmed) },
        onClarifiedSelect = { viewModel.onSelectClarifiedLocation(it) },
        onRetry = { viewModel.onRetry() },
        onSearchManually = { viewModel.onNavigateToManualSearch() },
        modifier = modifier
    )
}

@Composable
fun VoiceTripContent(
    uiState: VoiceTripUiState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    onClarifiedSelect: (SearchResultLocation) -> Unit,
    onRetry: () -> Unit,
    onSearchManually: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.md)
        ) {
            // Header with Cancel/Back button
            VoiceHeader(onCancel = onCancel)

            Spacer(modifier = Modifier.height(spacing.md))

            // Body depending on stage
            when (val stage = uiState.stage) {
                is VoiceTripStage.Idle -> {
                    IdleStageView(
                        onStartListening = onStartListening,
                        onSearchManually = onSearchManually
                    )
                }
                is VoiceTripStage.Listening -> {
                    ListeningStageView(
                        interimTranscript = stage.interimTranscript,
                        onStopListening = onStopListening,
                        onCancel = onCancel
                    )
                }
                is VoiceTripStage.Submitting -> {
                    SubmittingStageView(
                        transcript = stage.transcript
                    )
                }
                is VoiceTripStage.ReviewDraft -> {
                    ReviewDraftStageView(
                        draft = stage.draft,
                        onConfirm = onConfirm,
                        onRetry = onRetry,
                        onSearchManually = onSearchManually
                    )
                }
                is VoiceTripStage.Clarification -> {
                    ClarificationStageView(
                        stage = stage,
                        onLocationSelected = onClarifiedSelect,
                        onSearchManually = onSearchManually
                    )
                }
                is VoiceTripStage.Error -> {
                    ErrorStageView(
                        message = stage.message,
                        canRetry = stage.canRetry,
                        onRetry = onRetry,
                        onSearchManually = onSearchManually
                    )
                }
                is VoiceTripStage.Confirmed -> {
                    ConfirmedStageView()
                }
            }
        }
    }
}

@Composable
private fun VoiceHeader(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.voice_title),
            style = typography.headlineSmall,
            color = colors.foreground,
            fontWeight = FontWeight.SemiBold
        )

        IshaaraButton(
            text = stringResource(R.string.voice_cancel_button),
            onClick = onCancel,
            variant = IshaaraButtonVariant.Text,
            size = IshaaraButtonSize.Small
        )
    }
}

@Composable
private fun IdleStageView(
    onStartListening: () -> Unit,
    onSearchManually: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MicrophoneActionButton(
            isListening = false,
            onClick = onStartListening
        )

        Spacer(modifier = Modifier.height(spacing.xl))

        Text(
            text = "Tap microphone to speak",
            style = typography.headlineSmall,
            color = colors.foreground
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        Text(
            text = stringResource(R.string.voice_listening_hint),
            style = typography.bodyMedium,
            color = colors.foregroundMuted,
            modifier = Modifier.padding(horizontal = spacing.md)
        )

        Spacer(modifier = Modifier.height(spacing.xxl))

        IshaaraButton(
            text = stringResource(R.string.voice_search_manually_button),
            onClick = onSearchManually,
            variant = IshaaraButtonVariant.Outlined,
            size = IshaaraButtonSize.Large,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ListeningStageView(
    interimTranscript: String,
    onStopListening: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(spacing.lg))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MicrophoneActionButton(
                isListening = true,
                onClick = onStopListening
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            Text(
                text = stringResource(R.string.voice_listening_title),
                style = typography.headlineMedium,
                color = colors.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(spacing.sm))

            Text(
                text = if (interimTranscript.isNotBlank()) {
                    "\"$interimTranscript\""
                } else {
                    stringResource(R.string.voice_listening_hint)
                },
                style = typography.bodyLarge,
                color = if (interimTranscript.isNotBlank()) colors.foreground else colors.foregroundMuted,
                modifier = Modifier.padding(horizontal = spacing.md)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            IshaaraButton(
                text = stringResource(R.string.voice_stop_listening),
                onClick = onStopListening,
                variant = IshaaraButtonVariant.Primary,
                size = IshaaraButtonSize.Large,
                modifier = Modifier.fillMaxWidth()
            )

            IshaaraButton(
                text = stringResource(R.string.voice_cancel_button),
                onClick = onCancel,
                variant = IshaaraButtonVariant.Text,
                size = IshaaraButtonSize.Medium,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SubmittingStageView(
    transcript: String,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(56.dp),
            color = colors.primary,
            strokeWidth = 4.dp
        )

        Spacer(modifier = Modifier.height(spacing.xl))

        Text(
            text = stringResource(R.string.voice_processing_title),
            style = typography.headlineSmall,
            color = colors.foreground,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(spacing.xs))

        Text(
            text = stringResource(R.string.voice_processing_subtitle),
            style = typography.bodyMedium,
            color = colors.foregroundMuted
        )

        if (transcript.isNotBlank()) {
            Spacer(modifier = Modifier.height(spacing.lg))
            IshaaraCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "\"$transcript\"",
                    style = typography.bodyMedium,
                    color = colors.foreground
                )
            }
        }
    }
}

@Composable
private fun ReviewDraftStageView(
    draft: VoiceTripDraft,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onSearchManually: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.xs),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.voice_review_title),
                    style = typography.headlineMedium,
                    color = colors.foreground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(spacing.xxs))
                Text(
                    text = stringResource(R.string.voice_review_subtitle),
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
            }

            // Origin Card
            IshaaraCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(colors.success)
                        )
                        Text(
                            text = stringResource(R.string.voice_from_label),
                            style = typography.labelMedium,
                            color = colors.foregroundMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(spacing.xxs))
                    Text(
                        text = draft.origin.displayName,
                        style = typography.headlineSmall,
                        color = colors.foreground,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(spacing.xxs))
                    Text(
                        text = draft.origin.formattedAddress,
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                }
            }

            // Destination Card
            IshaaraCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(colors.primary)
                        )
                        Text(
                            text = stringResource(R.string.voice_to_label),
                            style = typography.labelMedium,
                            color = colors.foregroundMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(spacing.xxs))
                    Text(
                        text = draft.destination.displayName,
                        style = typography.headlineSmall,
                        color = colors.foreground,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(spacing.xxs))
                    Text(
                        text = draft.destination.formattedAddress,
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                }
            }

            // Spoken transcript reference
            IshaaraCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.voice_transcript_label),
                        style = typography.labelSmall,
                        color = colors.foregroundMuted
                    )
                    Spacer(modifier = Modifier.height(spacing.xxs))
                    Text(
                        text = "\"${draft.originalTranscript}\"",
                        style = typography.bodySmall,
                        color = colors.foreground
                    )
                }
            }
        }

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            IshaaraButton(
                text = stringResource(R.string.voice_confirm_button),
                onClick = onConfirm,
                variant = IshaaraButtonVariant.Primary,
                size = IshaaraButtonSize.Large,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                IshaaraButton(
                    text = stringResource(R.string.voice_try_again_button),
                    onClick = onRetry,
                    variant = IshaaraButtonVariant.Outlined,
                    size = IshaaraButtonSize.Medium,
                    modifier = Modifier.weight(1f)
                )

                IshaaraButton(
                    text = stringResource(R.string.voice_search_manually_button),
                    onClick = onSearchManually,
                    variant = IshaaraButtonVariant.Text,
                    size = IshaaraButtonSize.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ClarificationStageView(
    stage: VoiceTripStage.Clarification,
    onLocationSelected: (SearchResultLocation) -> Unit,
    onSearchManually: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.voice_clarification_title),
                style = typography.headlineSmall,
                color = colors.foreground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(spacing.xxs))
            Text(
                text = stringResource(R.string.voice_clarification_subtitle),
                style = typography.bodySmall,
                color = colors.foregroundMuted
            )

            Spacer(modifier = Modifier.height(spacing.md))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                items(stage.candidateLocations) { place ->
                    IshaaraCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLocationSelected(place) }
                    ) {
                        Column {
                            Text(
                                text = place.name,
                                style = typography.bodyLarge,
                                color = colors.foreground,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(spacing.xxs))
                            Text(
                                text = place.formattedAddress,
                                style = typography.bodySmall,
                                color = colors.foregroundMuted
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.md))

        IshaaraButton(
            text = stringResource(R.string.voice_search_manually_button),
            onClick = onSearchManually,
            variant = IshaaraButtonVariant.Outlined,
            size = IshaaraButtonSize.Large,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ErrorStageView(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onSearchManually: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(colors.danger.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "!",
                style = typography.headlineLarge,
                color = colors.danger,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(spacing.xl))

        Text(
            text = message,
            style = typography.headlineSmall,
            color = colors.foreground,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(spacing.xxl))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            if (canRetry) {
                IshaaraButton(
                    text = stringResource(R.string.voice_try_again_button),
                    onClick = onRetry,
                    variant = IshaaraButtonVariant.Primary,
                    size = IshaaraButtonSize.Large,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            IshaaraButton(
                text = stringResource(R.string.voice_search_manually_button),
                onClick = onSearchManually,
                variant = IshaaraButtonVariant.Outlined,
                size = IshaaraButtonSize.Large,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ConfirmedStageView(
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = colors.primary
        )
        Spacer(modifier = Modifier.height(spacing.md))
        Text(
            text = "Loading matching trips…",
            style = typography.bodyLarge,
            color = colors.foregroundMuted
        )
    }
}

/**
 * Large accessible microphone button with subtle breathing pulse animation when listening.
 */
@Composable
private fun MicrophoneActionButton(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by if (isListening) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1.0f) }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(96.dp)
            .scale(pulseScale)
            .clip(CircleShape)
            .background(if (isListening) colors.primary else colors.surfaceElevated)
            .border(
                width = if (isListening) 4.dp else 1.dp,
                color = if (isListening) colors.primary.copy(alpha = 0.4f) else colors.border,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = if (isListening) "Stop listening" else "Start voice trip creation"
            }
    ) {
        Text(
            text = "🎙",
            style = IshaaraTheme.typography.headlineLarge,
            color = if (isListening) colors.surface else colors.foreground
        )
    }
}
