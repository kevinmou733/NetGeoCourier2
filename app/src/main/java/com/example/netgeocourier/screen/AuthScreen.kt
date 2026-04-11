package com.example.netgeocourier.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.netgeocourier.R
import com.example.netgeocourier.helper.ApiConfigStore
import com.example.netgeocourier.helper.AuthTokenStore
import com.example.netgeocourier.network.ApiClient
import com.example.netgeocourier.network.AuthRepository
import kotlinx.coroutines.launch

private const val LOGIN_TAB = 0
private const val REGISTER_TAB = 1

@Composable
fun AuthScreen(
    onBack: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var selectedTab by rememberSaveable { mutableIntStateOf(LOGIN_TAB) }
    var serverUrl by rememberSaveable { mutableStateOf(ApiConfigStore.getBaseUrl(context)) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var studentId by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun validateInput(): String? {
        if (username.trim().length < 3) {
            return context.getString(R.string.auth_validation_username)
        }
        if (password.length < 6) {
            return context.getString(R.string.auth_validation_password)
        }
        return null
    }

    fun submit() {
        val validationError = validateInput()
        if (validationError != null) {
            errorMessage = validationError
            return
        }

        errorMessage = null
        isSubmitting = true
        coroutineScope.launch {
            serverUrl = ApiConfigStore.saveBaseUrl(context, serverUrl)
            val repository = AuthRepository(ApiClient.authService(context))
            val result = if (selectedTab == LOGIN_TAB) {
                repository.login(username, password)
            } else {
                repository.register(username, password, displayName, studentId)
            }

            result
                .onSuccess { payload ->
                    AuthTokenStore.saveSession(context, payload.accessToken, payload.user)
                    isSubmitting = false
                    onAuthSuccess()
                }
                .onFailure { throwable ->
                    errorMessage = throwable.message ?: context.getString(R.string.auth_failed)
                    isSubmitting = false
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        Text(
            text = stringResource(R.string.auth_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.auth_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = stringResource(R.string.auth_server_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.auth_server_label)) },
                    supportingText = { Text(stringResource(R.string.auth_server_hint)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == LOGIN_TAB,
                        onClick = {
                            selectedTab = LOGIN_TAB
                            errorMessage = null
                        },
                        text = { Text(stringResource(R.string.auth_tab_login)) }
                    )
                    Tab(
                        selected = selectedTab == REGISTER_TAB,
                        onClick = {
                            selectedTab = REGISTER_TAB
                            errorMessage = null
                        },
                        text = { Text(stringResource(R.string.auth_tab_register)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.auth_username)) },
                    supportingText = { Text(stringResource(R.string.auth_username_hint)) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.auth_password)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    supportingText = { Text(stringResource(R.string.auth_password_hint)) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(
                        if (passwordVisible) {
                            stringResource(R.string.auth_hide_password)
                        } else {
                            stringResource(R.string.auth_show_password)
                        }
                    )
                }

                if (selectedTab == REGISTER_TAB) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.auth_display_name)) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = studentId,
                        onValueChange = { studentId = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.auth_student_id)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = ::submit,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    } else {
                        Text(
                            if (selectedTab == LOGIN_TAB) {
                                stringResource(R.string.auth_submit_login)
                            } else {
                                stringResource(R.string.auth_submit_register)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.auth_back))
            }
        }
    }
}
