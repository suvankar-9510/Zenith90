package com.zenith90.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenith90.app.ui.theme.PixelHaptics
import com.zenith90.app.ui.theme.ZenithColors

@Composable
fun ZenithLogo(
    modifier: Modifier = Modifier,
    sizeDp: Int = 76
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape((sizeDp * 0.28f).dp))
            .background(ZenithColors.ForestGreenPrimary),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size((sizeDp * 0.65f).dp)) {
            val w = size.width
            val h = size.height

            // 1. Upward Zenith Apex Arrow (Peak of Mastery)
            val apexPath = Path().apply {
                moveTo(w * 0.5f, h * 0.08f)
                lineTo(w * 0.85f, h * 0.45f)
                lineTo(w * 0.68f, h * 0.45f)
                lineTo(w * 0.68f, h * 0.82f)
                lineTo(w * 0.32f, h * 0.82f)
                lineTo(w * 0.32f, h * 0.45f)
                lineTo(w * 0.15f, h * 0.45f)
                close()
            }
            drawPath(
                path = apexPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ZenithColors.MintAccent.copy(alpha = 0.40f),
                        Color.White.copy(alpha = 0.08f)
                    )
                )
            )

            // 2. Bold Stylized "Z" (Zenith Geometric Geometry)
            val zPath = Path().apply {
                moveTo(w * 0.20f, h * 0.25f)
                lineTo(w * 0.80f, h * 0.25f)
                lineTo(w * 0.28f, h * 0.72f)
                lineTo(w * 0.82f, h * 0.72f)
            }
            drawPath(
                path = zPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        ZenithColors.MintAccent,
                        Color.White
                    )
                ),
                style = Stroke(
                    width = 4.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // 3. Peak Star Anchor (North Star / Apex Point)
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = Offset(w * 0.5f, h * 0.08f)
            )
        }
    }
}

@Composable
fun GoogleBrandIcon(sizeDp: Int = 20) {
    Canvas(modifier = Modifier.size(sizeDp.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val stroke = w * 0.18f

        // Blue top-right to bottom-right
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(stroke)
        )
        // Green bottom
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(stroke)
        )
        // Yellow bottom-left
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 135f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(stroke)
        )
        // Red top-left
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(stroke)
        )
        // Center crossbar
        drawLine(
            color = Color(0xFF4285F4),
            startAngle = 0f,
            start = Offset(cx, cy),
            end = Offset(w - stroke / 2, cy),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun AuthScreen(
    authError: String?,
    isLoading: Boolean,
    onSignIn: (email: String, pass: String) -> Unit,
    onSignUp: (name: String, email: String, pass: String) -> Unit,
    onSignInWithGoogle: () -> Unit,
    onContinueAsGuest: () -> Unit,
    onContinueOffline: (name: String) -> Unit
) {
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    var isSignUpMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZenithColors.BotanicalBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // --- ZENITH 90 LOGO & BRAND HEADER ---
            ZenithLogo(sizeDp = 78)
            Spacer(Modifier.height(14.dp))

            Text(
                text = "Zenith90",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = ZenithColors.TextHeading
            )

            Text(
                text = "Schedule Smarter, Work Better",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = ZenithColors.ForestGreenPrimary
            )

            Text(
                text = "Handle your 90-day trajectory in a more convenient way",
                style = MaterialTheme.typography.bodySmall,
                color = ZenithColors.TextMuted,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(Modifier.height(22.dp))

            // --- AUTH CARD ---
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = ZenithColors.BotanicalSurface),
                border = BorderStroke(1.dp, ZenithColors.BotanicalBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Mode Toggle (Sign In / Create Account)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ZenithColors.BotanicalBg)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TabButton(
                            title = "SIGN IN",
                            isSelected = !isSignUpMode,
                            onClick = {
                                PixelHaptics.crispTick(view)
                                isSignUpMode = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                        TabButton(
                            title = "CREATE ACCOUNT",
                            isSelected = isSignUpMode,
                            onClick = {
                                PixelHaptics.crispTick(view)
                                isSignUpMode = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = if (isSignUpMode) "Begin 90-Day Transformation" else "Welcome Back, Scholar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ZenithColors.TextHeading
                    )

                    // Optional Name field (Sign Up Mode)
                    AnimatedVisibility(visible = isSignUpMode) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name") },
                            placeholder = { Text("Suvankar Das") },
                            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = ZenithColors.TextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                    }

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("scholar@domain.com") },
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = ZenithColors.TextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    }

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = ZenithColors.TextMuted) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = ZenithColors.TextMuted
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (email.isNotBlank() && password.isNotBlank()) {
                                    if (isSignUpMode) {
                                        onSignUp(name.ifBlank { "Scholar" }, email, password)
                                    } else {
                                        onSignIn(email, password)
                                    }
                                }
                            }
                        )
                    )

                    // Error text if present
                    if (!authError.isNullOrBlank()) {
                        Text(
                            text = authError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ZenithColors.PastelRoseText
                        )
                    }

                    // Main Action Button (Sign In / Sign Up)
                    Button(
                        onClick = {
                            PixelHaptics.heavyUnlock(view)
                            focusManager.clearFocus()
                            if (isSignUpMode) {
                                onSignUp(name.ifBlank { "Scholar" }, email, password)
                            } else {
                                onSignIn(email, password)
                            }
                        },
                        enabled = !isLoading && email.isNotBlank() && password.length >= 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ZenithColors.ForestGreenPrimary)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("AUTHENTICATING...", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        } else {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isSignUpMode) "CREATE ACCOUNT" else "LET'S GO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Divider with label
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = ZenithColors.BotanicalBorder)
                        Text(
                            text = "  OR CONTINUE WITH  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = ZenithColors.TextMuted
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = ZenithColors.BotanicalBorder)
                    }

                    // Google Sign-In Button
                    OutlinedButton(
                        onClick = {
                            PixelHaptics.crispTick(view)
                            onSignInWithGoogle()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ZenithColors.BotanicalBorder)
                    ) {
                        GoogleBrandIcon(sizeDp = 18)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Continue with Google",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = ZenithColors.TextHeading
                        )
                    }

                    // Continue as Guest Button (Instant Sandbox Bypass)
                    FilledTonalButton(
                        onClick = {
                            PixelHaptics.confirmCheck(view)
                            onContinueAsGuest()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ZenithColors.BotanicalBg,
                            contentColor = ZenithColors.TextHeading
                        )
                    ) {
                        Icon(
                            Icons.Outlined.Bolt,
                            contentDescription = null,
                            tint = ZenithColors.PastelAmberText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Continue as Guest (Instant Access)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Footer note
            Text(
                text = "Local Room SQLite active • Auto-syncs to Cloud Firestore when connected",
                style = MaterialTheme.typography.labelSmall,
                color = ZenithColors.TextMuted
            )
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) ZenithColors.BotanicalSurface else Color.Transparent,
            contentColor = if (isSelected) ZenithColors.ForestGreenPrimary else ZenithColors.TextMuted
        ),
        shape = RoundedCornerShape(10.dp),
        elevation = if (isSelected) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else ButtonDefaults.buttonElevation(),
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = modifier
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
