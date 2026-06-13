package com.rami.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.online.service.AuthService
import com.rami.ui.theme.RamiColors
import com.rami.ui.theme.RamiTheme
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    authService: AuthService,
    onSuccess: () -> Unit
) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    RamiTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(RamiColors.DarkGreen),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("🃏", fontSize = 64.sp, textAlign = TextAlign.Center)
                Text(
                    "رامي تونسي",
                    fontSize = 32.sp, fontWeight = FontWeight.Bold,
                    color = RamiColors.Gold, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))

                // Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    listOf(true to "تسجيل دخول", false to "حساب جديد").forEach { (flag, label) ->
                        FilterChip(
                            selected = isLogin == flag,
                            onClick  = { isLogin = flag; errorMsg = "" },
                            label    = { Text(label, fontWeight = FontWeight.Bold) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RamiColors.Gold,
                                selectedLabelColor     = RamiColors.DarkGreen,
                                labelColor             = RamiColors.TextLight
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                AnimatedVisibility(!isLogin) {
                    OutlinedTextField(
                        value = username, onValueChange = { username = it },
                        label = { Text("اسم المستخدم  •  Username") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        colors = authFieldColors()
                    )
                }

                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("البريد الإلكتروني  •  Email") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    colors = authFieldColors()
                )

                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("كلمة المرور  •  Password") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    colors = authFieldColors()
                )

                AnimatedVisibility(errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, textAlign = TextAlign.Center)
                }

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMsg = ""
                            val result = if (isLogin)
                                authService.signInWithEmail(email, password)
                            else
                                authService.signUpWithEmail(email, password, username)
                            isLoading = false
                            result.onSuccess { onSuccess() }
                                .onFailure { errorMsg = it.message ?: "فشل • Failed" }
                        }
                    },
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape  = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RamiColors.Gold, contentColor = RamiColors.DarkGreen)
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = RamiColors.DarkGreen, strokeWidth = 2.dp)
                    else Text(if (isLogin) "دخول  •  Sign In" else "تسجيل  •  Register",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = RamiColors.Gold.copy(alpha = 0.3f))

                // Social login buttons
                Text(
                    "أو سجّل عبر  •  Or continue with",
                    color    = RamiColors.TextLight.copy(0.4f),
                    fontSize = 11.sp
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SocialButton(
                        label    = "Google",
                        emoji    = "🔴",
                        modifier = Modifier.weight(1f),
                        onClick  = {
                            scope.launch {
                                isLoading = true
                                authService.signInWithGoogle()
                                    .onSuccess { onSuccess() }
                                    .onFailure { errorMsg = it.message ?: "فشل Google" }
                                isLoading = false
                            }
                        }
                    )
                    SocialButton(
                        label    = "Guest",
                        emoji    = "👤",
                        modifier = Modifier.weight(1f),
                        onClick  = {
                            scope.launch {
                                isLoading = true
                                authService.signInAsGuest()
                                    .onSuccess { onSuccess() }
                                    .onFailure { errorMsg = it.message ?: "فشل" }
                                isLoading = false
                            }
                        }
                    )
                }

                Text(
                    "بالمتابعة، أنت توافق على شروط الخدمة وسياسة الخصوصية",
                    color     = RamiColors.TextLight.copy(0.25f),
                    fontSize  = 9.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SocialButton(label: String, emoji: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick  = onClick,
        modifier = modifier.height(48.dp),
        shape    = RoundedCornerShape(14.dp),
        border   = androidx.compose.foundation.BorderStroke(1.dp, RamiColors.Gold.copy(0.3f)),
        colors   = ButtonDefaults.outlinedButtonColors(contentColor = RamiColors.TextLight)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 16.sp)
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = RamiColors.Gold,
    unfocusedBorderColor = RamiColors.TextLight.copy(alpha = 0.3f),
    focusedLabelColor    = RamiColors.Gold,
    unfocusedLabelColor  = RamiColors.TextLight.copy(alpha = 0.5f),
    focusedTextColor     = RamiColors.TextLight,
    unfocusedTextColor   = RamiColors.TextLight
)
