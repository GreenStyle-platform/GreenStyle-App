package com.vie.mit.auth.login

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vie.mit.auth.R
import com.vie.mit.auth.components.AuthBackground
import com.vie.mit.auth.components.GoogleLoginButton
import com.vie.mit.common.extension.addFocusCleaner
import com.vie.mit.common.extension.showToast
import com.vie.mit.common.ui.button.DebounceButton
import com.vie.mit.common.ui.button.LoadingButton
import com.vie.mit.common.ui.text.TextClickable
import com.vie.mit.common.ui.textfield.OutlineTextFieldGre
import com.vie.mit.common.ui.textfield.PasswordOutlineTextFieldGre
import com.vie.mit.common.ui.theme.AppTheme
import com.vie.mit.common.ui.theme.GreenTheme
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.event.collectLatest { event ->
            when (event) {
                is LoginEvent.ShowToast -> context.showToast(event.messageId)
            }
        }
    }

    LoginContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChanged,
        onPasswordChange = viewModel::onPasswordChanged,
        onLoginClick = viewModel::login,
        onForgotPasswordClick = viewModel::onForgotPassword,
        onSignUpClick = viewModel::onSignUp
    )
}

@Composable
fun LoginContent(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSignUpClick: () -> Unit
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
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            LoginHeader()
            Spacer(modifier = Modifier.height(32.dp))
            LoginMainComponent(
                uiState = uiState,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange,
                onForgotPasswordClick = onForgotPasswordClick,
                onLoginClick = onLoginClick,
                onSignUpClick = onSignUpClick,
                modifier = Modifier.padding(horizontal = AppTheme.dimens.paddingExtraLarge)
            )
        }
    }
}

@Composable
fun LoginHeader() {
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
            text = stringResource(R.string.sign_in_to_your_account),
            style = AppTheme.typography.headlineMedium,
            color = AppTheme.colors.onPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Enter your email and password to login",
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.onPrimary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LoginMainComponent(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
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
            SocialLoginSection()
            Spacer(modifier = Modifier.height(12.dp))
            LoginForm(
                email = uiState.email,
                password = uiState.password,
                errorMessage = uiState.errorMessage,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange,
                onForgotPasswordClick = onForgotPasswordClick
            )
            Spacer(modifier = Modifier.height(24.dp))
            LoginActions(
                onLoginClick = onLoginClick, onSignUpClick = onSignUpClick
            )
        }
    }
}

@Composable
fun SocialLoginSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        GoogleLoginButton(onClick = {})
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                thickness = 1.dp,
                color = AppTheme.colors.outlineVariant
            )
            Text(
                text = "Or login with",
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.secondaryText
            )
            HorizontalDivider(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                thickness = 1.dp,
                color = AppTheme.colors.outlineVariant
            )
        }
    }
}

@Composable
fun LoginForm(
    email: String,
    password: String,
    errorMessage: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlineTextFieldGre(
            value = email,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = onEmailChange,
            title = "Email",
            placeholder = "gregotrip@gmail.com"
        )
        Spacer(modifier = Modifier.height(8.dp))
        PasswordOutlineTextFieldGre(
            value = password,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = onPasswordChange,
            title = "Password",
            placeholder = "Your password"
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = AppTheme.colors.error,
                style = AppTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextClickable(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
            text = "Forgot Password ?",
            style = AppTheme.typography.bodyMedium.copy(color = AppTheme.colors.primary),
            onClick = onForgotPasswordClick
        )
    }
}

@Composable
fun LoginActions(
    onLoginClick: () -> Unit, onSignUpClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LoadingButton(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
            content = {
                Text(
                text = "Login",
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.onPrimary
            )}
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.don_t_have_an_account),
                style = AppTheme.typography.bodySmall
            )
            TextClickable(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(R.string.sign_up),
                style = AppTheme.typography.bodyMedium.copy(color = AppTheme.colors.primary),
                onClick = onSignUpClick
            )
        }
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    GreenTheme {
        LoginContent(
            uiState = LoginUiState(),
            onEmailChange = {},
            onPasswordChange = {},
            onLoginClick = {},
            onForgotPasswordClick = {},
            onSignUpClick = {})
    }
}
