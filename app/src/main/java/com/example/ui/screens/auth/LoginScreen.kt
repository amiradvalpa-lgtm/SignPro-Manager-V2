package com.example.ui.screens.auth

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.ui.components.common.AppCard
import com.example.ui.viewmodels.SignageViewModel
import com.example.util.SecurityUtils

@Composable
fun LoginScreen(
    viewModel: SignageViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()

    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showHintDialog by remember { mutableStateOf(false) }

    // Auto-prompt biometrics if enabled
    LaunchedEffect(isBiometricEnabled) {
        if (isBiometricEnabled && context is FragmentActivity) {
            if (SecurityUtils.canAuthenticateWithBiometrics(context)) {
                SecurityUtils.showBiometricPrompt(
                    activity = context,
                    onSuccess = { onLoginSuccess() },
                    onError = { /* fallback to password */ }
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AppCard(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .testTag("login_card"),
            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ورود به نرم‌افزار تابلوسازی",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "تابلوسازی تبلیغاتی آلپا",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = {
                        passwordInput = it
                        errorMessage = null
                    },
                    label = { Text("رمز عبور یا PIN") },
                    leadingIcon = {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "نمایش رمز"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    isError = errorMessage != null,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input_field")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val storedHash = currentUser?.passwordHash ?: ""
                        if (SecurityUtils.verifyPassword(passwordInput, storedHash)) {
                            onLoginSuccess()
                        } else {
                            errorMessage = "رمز عبور وارد شده نادرست است."
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("login_submit_btn")
                ) {
                    Text("ورود به برنامه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isBiometricEnabled && context is FragmentActivity) {
                        IconButton(
                            onClick = {
                                SecurityUtils.showBiometricPrompt(
                                    activity = context,
                                    onSuccess = { onLoginSuccess() },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            modifier = Modifier.testTag("biometric_login_btn")
                        ) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = "ورود با اثر انگشت",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    TextButton(
                        onClick = { showHintDialog = true },
                        modifier = Modifier.testTag("forgot_password_btn")
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فراموشی رمز عبور؟", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showHintDialog) {
        val user = currentUser
        AlertDialog(
            onDismissRequest = { showHintDialog = false },
            icon = {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            title = {
                Text("راهنمای یادآوری رمز عبور", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "راهنمای ثبت‌شده توسط شما:\n${user?.securityHint?.ifEmpty { "راهنمایی ثبت نشده است. رمز اولیه 1234 است." }}",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "در صورت نیاز، می‌توانید یادآوری رمز را از طریق پیامک برای خود ارسال نمایید.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("smsto:09144862950")
                            putExtra("sms_body", "یادآوری رمز ورود نرم‌افزار تابلوسازی آلپا: ${user?.securityHint}")
                        }
                        try {
                            context.startActivity(smsIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "برنامه پیامک یافت نشد", Toast.LENGTH_SHORT).show()
                        }
                        showHintDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ارسال پیامک یادآوری")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showHintDialog = false }, shape = RoundedCornerShape(12.dp)) {
                    Text("بستن")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Login Card Preview", showBackground = true)
@Composable
fun LoginCardPreview() {
    com.example.ui.theme.MyApplicationTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "تابلوسازی آلپا",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "سامانه جامع مدیریت کارگاه و پروژه‌ها",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
