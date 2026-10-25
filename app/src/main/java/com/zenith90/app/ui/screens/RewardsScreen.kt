package com.zenith90.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenith90.app.data.local.BadgeEntity
import com.zenith90.app.data.local.FullDaySnapshot
import com.zenith90.app.ui.theme.PixelHaptics
import com.zenith90.app.ui.theme.ZenithColors
import com.zenith90.app.ui.viewmodel.ZenithUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun RewardsScreen(
    state: ZenithUiState,
    onUpdateReflection: (String) -> Unit
) {
    val view = LocalView.current
    var searchQuery by remember { mutableStateOf("") }
    var reflectionInput by remember(state.currentDaySnapshot?.dailyLog?.reflectionNote) {
        mutableStateOf(state.currentDaySnapshot?.dailyLog?.reflectionNote ?: "")
    }

    var selectedBadgeForDetail by remember { mutableStateOf<BadgeEntity?>(null) }
    var streakExplanationTitle by remember { mutableStateOf<Pair<String, String>?>(null) }

    // --- DIALOG: BADGE CRITERIA & STRATEGY MODAL ---
    if (selectedBadgeForDetail != null) {
        val badge = selectedBadgeForDetail!!
        val isUnlocked = badge.isUnlocked
        AlertDialog(
            onDismissRequest = { selectedBadgeForDetail = null },
            icon = {
                Icon(
                    imageVector = if (isUnlocked) Icons.Outlined.Verified else Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = if (isUnlocked) Color(badge.accentHexStart) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = badge.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = badge.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = badge.criteriaDescription,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Progress bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Current Progress",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${badge.progressCurrent} / ${badge.progressTarget} Days",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (badge.progressCurrent.toFloat() / badge.progressTarget.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = Color(badge.accentHexStart),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "HOW TO EARN:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val strategy = when (badge.badgeId) {
                                "5am_club" -> "Log your wake-up time within 15 minutes of target (e.g. 05:15 - 05:45 AM) for 7 consecutive days unbroken."
                                "iron_and_ink" -> "Complete your daily physical workout block AND achieve at least 6 hours (360 mins) of deep study in a single calendar day."
                                "night_architect" -> "Lock in your next-day blueprint using the Night Planner before 10:30 PM for 7 consecutive nights."
                                "relentless_30" -> "Achieve unbroken 3-pillar equilibrium for 30 consecutive days."
                                "relentless_60" -> "Achieve unbroken 3-pillar equilibrium for 60 consecutive days."
                                "relentless_90" -> "Complete the entire 90-day transformation protocol up to the exam."
                                else -> "Consistently execute your daily blueprint."
                            }
                            Text(text = strategy, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedBadgeForDetail = null }) {
                    Text("GOT IT", style = MaterialTheme.typography.labelSmall)
                }
            }
        )
    }

    // --- DIALOG: STREAK CALCULATION EXPLANATION ---
    if (streakExplanationTitle != null) {
        val (streakName, explanation) = streakExplanationTitle!!
        AlertDialog(
            onDismissRequest = { streakExplanationTitle = null },
            icon = { Icon(Icons.Outlined.Info, contentDescription = null, tint = ZenithColors.AmberStart) },
            title = { Text(text = streakName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = { Text(text = explanation, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(onClick = { streakExplanationTitle = null }) {
                    Text("CLOSE", style = MaterialTheme.typography.labelSmall)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(top = 22.dp, bottom = 100.dp)
    ) {
        // --- STREAK COUNTERS ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "STREAK & MILESTONE VAULT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap any metric or badge to view evaluation rules",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StreakMetricTile(
                        title = "OVERALL STREAK",
                        count = state.overallStreakDays,
                        modifier = Modifier.weight(1f).clickable {
                            PixelHaptics.crispTick(view)
                            streakExplanationTitle = Pair(
                                "OVERALL EQUILIBRIUM STREAK",
                                "Evaluated nightly: Increments by 1 every day you achieve all 3 pillars: (1) wake goal met, (2) study hours >= 4 hours, and (3) physical training marked complete. Any missed pillar day resets this streak."
                            )
                        }
                    )
                    StreakMetricTile(
                        title = "5 AM WAKE STREAK",
                        count = state.wakeStreakDays,
                        modifier = Modifier.weight(1f).clickable {
                            PixelHaptics.crispTick(view)
                            streakExplanationTitle = Pair(
                                "5 AM CIRCADIAN WAKE STREAK",
                                "Increments every day you log your wake-up time within 15 minutes of your target wake window. Failing to log or waking up late resets this streak."
                            )
                        }
                    )
                    StreakMetricTile(
                        title = "DEEP STUDY STREAK",
                        count = state.studyStreakDays,
                        modifier = Modifier.weight(1f).clickable {
                            PixelHaptics.crispTick(view)
                            streakExplanationTitle = Pair(
                                "DEEP STUDY FOCUS STREAK",
                                "Increments every calendar day you log at least 4 hours (240 minutes) of validated deep study."
                            )
                        }
                    )
                }
            }
        }

        // --- MILESTONE BADGES ---
        item {
            Text(
                text = "UNLOCKABLE MILESTONE BADGES (TAP FOR DETAILS)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(state.badges.chunked(2)) { badgePair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                badgePair.forEach { badge ->
                    BadgeVaultCard(
                        badge = badge,
                        modifier = Modifier.weight(1f).clickable {
                            PixelHaptics.crispTick(view)
                            selectedBadgeForDetail = badge
                        }
                    )
                }
                if (badgePair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // --- DAILY 1-LINE REFLECTION EDITOR ---
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "TODAY'S 1-LINE AUDIT REFLECTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = reflectionInput,
                            onValueChange = { reflectionInput = it },
                            placeholder = { Text("What went well today? Lock in your audit note...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        FilledIconButton(onClick = {
                            PixelHaptics.confirmCheck(view)
                            onUpdateReflection(reflectionInput)
                        }) {
                            Icon(Icons.Outlined.Check, contentDescription = "Save Reflection")
                        }
                    }
                }
            }
        }

        // --- SEARCHABLE LOGBOOK ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "IMMUTABLE DAILY LOGBOOK & AUDIT TRAIL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search reflections, subjects, or dates...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) }
                )
            }
        }

        val filteredSnapshots = state.allLogsWithDetails.filter { snap ->
            if (searchQuery.isBlank()) true
            else {
                snap.dailyLog.reflectionNote.contains(searchQuery, ignoreCase = true) ||
                    snap.scheduleBlocks.any {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                            (it.examSubjectTag?.contains(searchQuery, ignoreCase = true) == true)
                    }
            }
        }

        if (filteredSnapshots.isEmpty()) {
            item {
                Text(
                    text = "No logbook entries match \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(filteredSnapshots, key = { it.dailyLog.dateEpochDay }) { snapshot ->
                ImmutableLogbookEntryCard(snapshot = snapshot)
            }
        }
    }
}

@Composable
private fun StreakMetricTile(title: String, count: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.Start) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(text = "$count", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Text(text = "DAYS • TAP INFO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun BadgeVaultCard(badge: BadgeEntity, modifier: Modifier = Modifier) {
    val brush = Brush.linearGradient(
        colors = if (badge.isUnlocked) {
            listOf(Color(badge.accentHexStart), Color(badge.accentHexEnd))
        } else {
            listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
        }
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (badge.isUnlocked) 1.5.dp else 1.dp,
            color = if (badge.isUnlocked) Color(badge.accentHexStart) else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(brush),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (badge.isUnlocked) Icons.Outlined.Verified else Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = if (badge.isUnlocked) Color(0xFF0F0F12) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "${badge.progressCurrent}/${badge.progressTarget}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(text = badge.title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(
                text = badge.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ImmutableLogbookEntryCard(snapshot: FullDaySnapshot) {
    val date = LocalDate.ofEpochDay(snapshot.dailyLog.dateEpochDay)
    val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = date.format(formatter).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(snapshot.dailyLog.completionRatio * 100).toInt()}% SCORE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Study: ${snapshot.dailyLog.totalStudyMinutes / 60}h ${snapshot.dailyLog.totalStudyMinutes % 60}m • " +
                    "Wake: ${snapshot.dailyLog.actualWakeTimeMinutes?.let { formatMinutesToTime(it) } ?: "Unlogged"} • " +
                    "Tasks: ${snapshot.scheduleBlocks.count { it.isCompleted }}/${snapshot.scheduleBlocks.size}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (snapshot.dailyLog.reflectionNote.isNotBlank()) {
                Text(
                    text = "\"${snapshot.dailyLog.reflectionNote}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
