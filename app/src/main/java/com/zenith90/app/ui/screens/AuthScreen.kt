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
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1E1E26),
                        Color(0xFF121216)
                    )
                )
            ),
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
                        Color(0xFF84FAB0).copy(alpha = 0.30f),
                        Color(0xFF8FD3F4).copy(alpha = 0.05f)
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
                        ZenithColors.AmberStart,
                        ZenithColors.CoralEnd
                    )
                ),
                style = Stroke(
                    width = 4.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // 3. Central Catalyst Star dot at Apex
            drawCircle(
                color = ZenithColors.SageStart,
                radius = 3.dp.toPx(),
                center = Offset(w * 0.5f, h * 0.08f)
            )
        }
    }
}

@Composable
fun GoogleBrandIcon(modifier: Modifier = Modifier, sizeDp: Int = 18) {
    Canvas(modifier = modifier.size(sizeDp.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val stroke = 2.4.dp.toPx()

        // Google 4-Color Circular Mark
        drawArc(
            color = Color(0xFF4285F4), // Google Blue
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(stroke)
        )
        drawArc(
            color = Color(0xFF34A853), // Google Green
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(stroke)
        )
        drawArc(
            color = Color(0xFFFBBC05), // Google Yellow
            startAngle = 135f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(stroke)
        )
        drawArc(
            color = Color(0xFFEA4335), // Google Red
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(stroke)
        )
        // Center crossbar
        drawLine(
            color = Color(0xFF4285F4),
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
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // --- ZENITH 90 LOGO & BRAND HEADER ---
            ZenithLogo(sizeDp = 78)
            Spacer(Modifier.height(14.dp))

            Text(
                text = "ZENITH 90",
                style = MaterialTheme.typography.displayMedium,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "90-DAY CIRCADIAN & DEEP WORK PROTOCOL",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(22.dp))

            // --- AUTH CARD ---
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                            .background(MaterialTheme.colorScheme.surfaceVariant)
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
                        fontWeight = FontWeight.SemiBold
                    )

                    // Optional Name field (Sign Up Mode)
                    AnimatedVisibility(visible = isSignUpMode) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name") },
                            placeholder = { Text("Suvankar Das") },
                            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
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
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
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
                            color = ZenithColors.CoralEnd
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
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("AUTHENTICATING...", style = MaterialTheme.typography.labelSmall)
                        } else {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isSignUpMode) "CREATE ACCOUNT" else "SIGN IN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Divider with label
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "  OR CONTINUE WITH  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
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
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        GoogleBrandIcon(sizeDp = 18)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Continue with Google",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
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
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Bolt,
                            contentDescription = null,
                            tint = ZenithColors.AmberStart,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            containerColor = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
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
