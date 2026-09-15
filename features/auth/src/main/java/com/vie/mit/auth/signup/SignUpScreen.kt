package com.vie.mit.auth.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vie.mit.auth.R
import com.vie.mit.auth.components.AuthBackground
import com.vie.mit.auth.components.GoogleLoginButton
import com.vie.mit.common.extension.addFocusCleaner
import com.vie.mit.common.extension.showToast
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.common.theme.GreenTheme
import com.vie.mit.common.ui.button.LoadingButton
import com.vie.mit.common.ui.text.TextClickable
import com.vie.mit.common.ui.textfield.OutlineTextFieldGre
import com.vie.mit.common.ui.textfield.PasswordOutlineTextFieldGre
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SignUpScreen(
    viewModel: SignUpViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.event.collectLatest { event ->
            when (event) {
                is SignUpEvent.ShowToast -> context.showToast(event.messageId)
            }
        }
    }

    SignUpContent(
        uiState = uiState,
        onUsernameChange = viewModel::onUsernameChanged,
        onEmailChange = viewModel::onEmailChanged,
        onPhoneChange = viewModel::onPhoneChanged,
        onPasswordChange = viewModel::onPasswordChanged,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChanged,
        onSignUpClick = viewModel::signUp,
        onBackClick = viewModel::onBackClick,
        onLoginClick = viewModel::onLoginClick
    )
}

@Composable
fun SignUpContent(
    uiState: SignUpUiState,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSignUpClick: () -> Unit,
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    val focusManager: FocusManager = LocalFocusManager.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .addFocusCleaner(focusManager)
    ) {
        AuthBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = AppTheme.dimens.screenPaddingLarge)
                .verticalScroll(rememberScrollState())
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.padding(start = AppTheme.dimens.paddingMedium)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = AppTheme.colors.onPrimary
                )
            }

            SignUpHeader(onLoginClick = onLoginClick)

            Spacer(modifier = Modifier.height(24.dp))

            SignUpMainComponent(
                uiState = uiState,
                onUsernameChange = onUsernameChange,
                onEmailChange = onEmailChange,
                onPhoneChange = onPhoneChange,
                onPasswordChange = onPasswordChange,
                onConfirmPasswordChange = onConfirmPasswordChange,
                onSignUpClick = onSignUpClick,
                modifier = Modifier.padding(horizontal = AppTheme.dimens.paddingExtraLarge)
            )
        }
    }
}

@Composable
fun SignUpHeader(onLoginClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = "GreGo",
            style = AppTheme.typography.headlineLarge,
            color = AppTheme.colors.onPrimary
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppTheme.dimens.paddingMedium),
            textAlign = TextAlign.Center,
            text = stringResource(R.string.sign_up),
            style = AppTheme.typography.headlineMedium,
            color = AppTheme.colors.onPrimary
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.already_have_an_account),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onPrimary
            )
            TextClickable(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(R.string.login),
                style = AppTheme.typography.bodyMedium.copy(color = Color.White),
                onClick = onLoginClick
            )
        }
    }
}

@Composable
fun SignUpMainComponent(
    uiState: SignUpUiState,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSignUpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp))
            .background(color = AppTheme.colors.surface, shape = RoundedCornerShape(12.dp))
            .padding(24.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SignUpForm(
                uiState = uiState,
                onUsernameChange = onUsernameChange,
                onEmailChange = onEmailChange,
                onPhoneChange = onPhoneChange,
                onPasswordChange = onPasswordChange,
                onConfirmPasswordChange = onConfirmPasswordChange
            )

            Spacer(modifier = Modifier.height(24.dp))

            SignUpActions(
                uiState = uiState, onSignUpClick = onSignUpClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            GoogleLoginButton(onClick = {})
        }
    }
}

@Composable
fun SignUpForm(
    uiState: SignUpUiState,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlineTextFieldGre(
            value = uiState.username,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = onUsernameChange,
            title = stringResource(R.string.username),
            placeholder = "Enter your username"
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlineTextFieldGre(
            value = uiState.email,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = onEmailChange,
            title = stringResource(R.string.email),
            placeholder = "gregotrip@gmail.com"
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlineTextFieldGre(
            value = uiState.phone,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = onPhoneChange,
            title = stringResource(R.string.phone),
            placeholder = "Enter your phone number"
        )
        Spacer(modifier = Modifier.height(8.dp))
        PasswordOutlineTextFieldGre(
            value = uiState.password,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = onPasswordChange,
            title = stringResource(R.string.password),
            placeholder = "Your password"
        )
        Spacer(modifier = Modifier.height(8.dp))
        PasswordOutlineTextFieldGre(
            value = uiState.confirmPassword,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = onConfirmPasswordChange,
            title = stringResource(R.string.confirm_password),
            placeholder = "Confirm your password"
        )

        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                color = AppTheme.colors.error,
                style = AppTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun SignUpActions(
    uiState: SignUpUiState, onSignUpClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LoadingButton(
            isLoading = uiState.isLoading,
            onClick = onSignUpClick,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
            content = {
                Text(
                    text = stringResource(R.string.sign_up),
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.onPrimary
                )
            })
    }
}

@Preview
@Composable
private fun SignUpScreenPreview() {
    GreenTheme {
        SignUpContent(
            uiState = SignUpUiState(),
            onUsernameChange = {},
            onEmailChange = {},
            onPhoneChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onSignUpClick = {},
            onBackClick = {},
            onLoginClick = {})
    }
}
