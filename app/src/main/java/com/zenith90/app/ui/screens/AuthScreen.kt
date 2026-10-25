package com.zenith90.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.zenith90.app.ui.theme.PixelHaptics
import com.zenith90.app.ui.theme.ZenithColors

@Composable
fun AuthScreen(
    authError: String?,
    isLoading: Boolean,
    onSignIn: (email: String, pass: String) -> Unit,
    onSignUp: (name: String, email: String, pass: String) -> Unit,
    onContinueOffline: (name: String) -> Unit
) {
    val view = LocalView.current
    var isSignUpMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(ZenithColors.SageStart)
                    )
                    Text(
                        text = "ZENITH90 • PIXEL CLOUD IDENTITY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = if (isSignUpMode) "Create Scholar Vault" else "Sign In to Zenith90",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Sync your 90-day exam blueprint, workout sets, and 5 AM streaks across Firebase Cloud and local Room SQLite.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AnimatedVisibility(visible = isSignUpMode) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Your Name") },
                        placeholder = { Text("Suvankar Das") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    placeholder = { Text("scholar@caluniv.ac.in") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (6+ characters)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                if (!authError.isNullOrBlank()) {
                    Text(
                        text = authError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ZenithColors.CoralEnd
                    )
                }

                Button(
                    onClick = {
                        PixelHaptics.heavyUnlock(view)
                        if (isSignUpMode) {
                            onSignUp(name.ifBlank { "Scholar" }, email, password)
                        } else {
                            onSignIn(email, password)
                        }
                    },
                    enabled = !isLoading && email.isNotBlank() && password.length >= 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Outlined.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isLoading) "AUTHENTICATING..."
                        else if (isSignUpMode) "CREATE FIREBASE ACCOUNT"
                        else "SIGN IN WITH FIREBASE",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        PixelHaptics.crispTick(view)
                        isSignUpMode = !isSignUpMode
                    }) {
                        Text(
                            text = if (isSignUpMode) "ALREADY HAVE AN ACCOUNT? SIGN IN" else "NEW SCHOLAR? CREATE ACCOUNT",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                FilledTonalButton(
                    onClick = {
                        PixelHaptics.confirmCheck(view)
                        onContinueOffline(name.ifBlank { "Scholar" })
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Outlined.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "INSTANT PIXEL OFFLINE MODE (NO CLOUD REQUIRED)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
