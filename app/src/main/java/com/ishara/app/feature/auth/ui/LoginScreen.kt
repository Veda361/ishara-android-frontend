package com.ishara.app.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ishara.app.core.designsystem.component.IshaaraBadge
import com.ishara.app.core.designsystem.component.IshaaraErrorState
import com.ishara.app.core.designsystem.component.IshaaraPrimaryAction
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.feature.auth.AuthUiState
import com.ishara.app.feature.auth.AuthViewModel

/**
 * Human-centered Login Screen for Ishaara.
 * Adheres to Phase 02 design principles:
 * - Simple, fast, and trustworthy
 * - Single primary action ("Continue with Google")
 * - 54dp thumb-zone primary action button
 * - Clean typography hierarchy
 * - Inline human error messages and session expiration alerts
 * - Accessible content descriptions
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LoginContent(
        uiState = uiState,
        onSignInClick = { viewModel.onContinueWithGoogleClicked(context) },
        onRetryClick = { viewModel.clearError(); viewModel.onContinueWithGoogleClicked(context) },
        onMethodSelect = { viewModel.selectAuthMethod(it) },
        onEmailChange = { viewModel.onEmailInputChanged(it) },
        onOtpChange = { viewModel.onOtpInputChanged(it) },
        onSendOtpClick = { viewModel.sendVerificationOtp() },
        onVerifyOtpClick = { viewModel.verifyEmailOtp() },
        modifier = modifier
    )
}

@Composable
fun LoginContent(
    uiState: AuthUiState,
    onSignInClick: () -> Unit,
    onRetryClick: () -> Unit,
    onMethodSelect: (com.ishara.app.feature.auth.AuthMethod) -> Unit = {},
    onEmailChange: (String) -> Unit = {},
    onOtpChange: (String) -> Unit = {},
    onSendOtpClick: () -> Unit = {},
    onVerifyOtpClick: () -> Unit = {},
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
                .padding(horizontal = spacing.md)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Transit Pill Badge
                IshaaraBadge(
                    text = "PUBLIC TRANSIT & MOBILITY",
                    modifier = Modifier.padding(bottom = spacing.md)
                )

                Text(
                    text = "Ishaara",
                    style = typography.displayLarge,
                    color = colors.foreground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(spacing.xs))

                Text(
                    text = "Reliable, transparent mobility for students and drivers",
                    style = typography.bodyLarge,
                    color = colors.foregroundMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = spacing.sm)
                )
            }

            // Middle Feedback & Form Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Session expired alert
                if (uiState.sessionExpiredMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = spacing.md)
                            .background(colors.warningSubtle, shape = IshaaraTheme.shapes.md)
                            .padding(spacing.md)
                    ) {
                        Text(
                            text = uiState.sessionExpiredMessage,
                            style = typography.bodyMedium,
                            color = colors.warning,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Error State
                if (uiState.errorMessage != null) {
                    IshaaraErrorState(
                        title = "Sign-in could not be completed",
                        message = uiState.errorMessage,
                        retryLabel = "Try again",
                        onRetryClick = onRetryClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = spacing.md)
                    )
                }

                // Email OTP Input Form (when EMAIL_OTP mode selected)
                if (uiState.authMethod == com.ishara.app.feature.auth.AuthMethod.EMAIL_OTP) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = spacing.sm)
                    ) {
                        com.ishara.app.core.designsystem.component.IshaaraTextField(
                            value = uiState.emailInput,
                            onValueChange = onEmailChange,
                            label = "Email Address",
                            placeholder = "user@college.edu",
                            enabled = !uiState.isOtpSent && !uiState.isSendingOtp,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (uiState.isOtpSent) {
                            Spacer(modifier = Modifier.height(spacing.md))
                            com.ishara.app.core.designsystem.component.IshaaraTextField(
                                value = uiState.otpInput,
                                onValueChange = onOtpChange,
                                label = "Verification Code (OTP)",
                                placeholder = "123456",
                                enabled = !uiState.isVerifyingOtp,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Bottom Action Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.authMethod == com.ishara.app.feature.auth.AuthMethod.GOOGLE) {
                    IshaaraPrimaryAction(
                        text = if (uiState.isAuthenticating) "Signing in..." else "Continue with Google",
                        onClick = onSignInClick,
                        enabled = uiState.canInitiateSignIn,
                        loading = uiState.isAuthenticating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = if (uiState.isAuthenticating) {
                                    "Signing in, please wait"
                                } else {
                                    "Continue with Google sign-in button"
                                }
                            }
                    )

                    Spacer(modifier = Modifier.height(spacing.sm))

                    com.ishara.app.core.designsystem.component.IshaaraButton(
                        text = "Sign in with Email OTP",
                        onClick = { onMethodSelect(com.ishara.app.feature.auth.AuthMethod.EMAIL_OTP) },
                        variant = com.ishara.app.core.designsystem.component.IshaaraButtonVariant.Text,
                        enabled = uiState.canInitiateSignIn,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    if (!uiState.isOtpSent) {
                        IshaaraPrimaryAction(
                            text = if (uiState.isSendingOtp) "Sending Code..." else "Send Verification Code",
                            onClick = onSendOtpClick,
                            enabled = uiState.canSendOtp,
                            loading = uiState.isSendingOtp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        IshaaraPrimaryAction(
                            text = if (uiState.isVerifyingOtp) "Verifying..." else "Verify & Sign In",
                            onClick = onVerifyOtpClick,
                            enabled = uiState.canVerifyOtp,
                            loading = uiState.isVerifyingOtp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(spacing.xs))

                        com.ishara.app.core.designsystem.component.IshaaraButton(
                            text = "Resend Code",
                            onClick = onSendOtpClick,
                            variant = com.ishara.app.core.designsystem.component.IshaaraButtonVariant.Text,
                            enabled = uiState.canSendOtp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(spacing.xs))

                    com.ishara.app.core.designsystem.component.IshaaraButton(
                        text = "Back to Google Sign-In",
                        onClick = { onMethodSelect(com.ishara.app.feature.auth.AuthMethod.GOOGLE) },
                        variant = com.ishara.app.core.designsystem.component.IshaaraButtonVariant.Text,
                        enabled = !uiState.isSendingOtp && !uiState.isVerifyingOtp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(spacing.sm))

                Text(
                    text = "By continuing, you agree to Ishaara terms of transit service.",
                    style = typography.bodySmall,
                    color = colors.foregroundSubtle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = spacing.md)
                )
            }
        }
    }
}
