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
import androidx.compose.ui.unit.sp
import com.zenith90.app.data.local.BadgeEntity
import com.zenith90.app.data.local.FullDaySnapshot
import com.zenith90.app.ui.theme.PixelHaptics
import com.zenith90.app.ui.theme.ZenithColors
import com.zenith90.app.ui.viewmodel.ZenithUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class BadgeFilterOption(val label: String) {
    ALL("ALL BADGES"),
    CLAIMED("CLAIMED (COLORFUL)"),
    LOCKED("LOCKED (GREY)")
}

@Composable
fun RewardsScreen(
    state: ZenithUiState,
    onUpdateReflection: (String) -> Unit,
    onClaimBadge: (String) -> Unit = {}
) {
    val view = LocalView.current
    var selectedFilter by remember { mutableStateOf(BadgeFilterOption.ALL) }
    var reflectionInput by remember(state.currentDaySnapshot?.dailyLog?.reflectionNote) {
        mutableStateOf(state.currentDaySnapshot?.dailyLog?.reflectionNote ?: "")
    }

    var selectedBadgeForDetail by remember { mutableStateOf<BadgeEntity?>(null) }
    var streakExplanationTitle by remember { mutableStateOf<Pair<String, String>?>(null) }

    val allBadges = state.badges
    val claimedBadges = allBadges.filter { it.isUnlocked }
    val lockedBadges = allBadges.filter { !it.isUnlocked }

    val displayedBadges = when (selectedFilter) {
        BadgeFilterOption.ALL -> allBadges
        BadgeFilterOption.CLAIMED -> claimedBadges
        BadgeFilterOption.LOCKED -> lockedBadges
    }

    // --- DIALOG: BADGE CRITERIA & GAMING STRATEGY MODAL ---
    if (selectedBadgeForDetail != null) {
        val badge = selectedBadgeForDetail!!
        val isUnlocked = badge.isUnlocked
        val canClaim = !isUnlocked && badge.progressCurrent >= badge.progressTarget

        AlertDialog(
            onDismissRequest = { selectedBadgeForDetail = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            if (isUnlocked) Brush.linearGradient(
                                listOf(Color(badge.accentHexStart), Color(badge.accentHexEnd))
                            ) else Brush.linearGradient(
                                listOf(ZenithColors.LockedGreySurface, ZenithColors.LockedGreySurface)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isUnlocked) Icons.Outlined.MilitaryTech else Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = if (isUnlocked) Color(0xFF101014) else ZenithColors.LockedGreyIcon,
                        modifier = Modifier.size(30.dp)
                    )
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = badge.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = badge.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUnlocked) Color(badge.accentHexStart) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = badge.criteriaDescription,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Battle Progress Indicator
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Mission Progress",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${badge.progressCurrent} / ${badge.progressTarget} Target Units",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (badge.progressCurrent.toFloat() / badge.progressTarget.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = if (isUnlocked) Color(badge.accentHexStart) else ZenithColors.LockedGreyIcon,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    // Strategic Intel Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "TACTICAL INTEL & HOW TO EARN:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val strategy = when (badge.badgeId) {
                                "first_blood" -> "Complete your very first day with registered study time or physical training. The hardest step is initiation."
                                "5am_club" -> "Log your wake-up time within 15 minutes of target (e.g. 05:15 - 05:45 AM) for 7 consecutive days unbroken."
                                "quad_pomodoro" -> "Log and verify 4 distinct 25-minute Pomodoro focus blocks during today's study grind."
                                "iron_and_ink" -> "Complete your daily physical workout block AND achieve at least 6 hours (360 mins) of deep study in a single calendar day."
                                "discipline_100" -> "Nail all 3 daily pillars completely in a single day to achieve a perfect 100% equilibrium ratio."
                                "night_architect" -> "Lock in your next-day blueprint using the Night Planner before 10:30 PM for 7 consecutive nights."
                                "relentless_30" -> "Achieve unbroken 3-pillar equilibrium for 30 consecutive days leading up to January."
                                "relentless_60" -> "Maintain iron discipline and high momentum for 60 consecutive days."
                                "relentless_90" -> "Complete the entire 90-day transformation protocol up to the exam."
                                else -> "Consistently execute your daily blueprint."
                            }
                            Text(text = strategy, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                if (canClaim) {
                    Button(
                        onClick = {
                            PixelHaptics.heavyUnlock(view)
                            onClaimBadge(badge.badgeId)
                            selectedBadgeForDetail = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ZenithColors.AmberStart)
                    ) {
                        Text("CLAIM RANK BADGE", style = MaterialTheme.typography.labelSmall, color = Color(0xFF101014), fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(onClick = { selectedBadgeForDetail = null }) {
                        Text("GOT IT", style = MaterialTheme.typography.labelSmall)
                    }
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
        // --- 1. SCREEN HEADER (MATERIAL YOU EXPRESSIVE TITLE) ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Text(
                        text = "TACTICAL TROPHY ROOM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "STREAK & BADGE VAULT",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Track your battle tiers, claimable ranks, and daily transformation logbook",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- 2. STREAK COUNTERS ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StreakMetricTile(
                    title = "OVERALL STREAK",
                    count = state.overallStreakDays,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
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
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
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
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            PixelHaptics.crispTick(view)
                            streakExplanationTitle = Pair(
                                "DEEP STUDY FOCUS STREAK",
                                "Increments every calendar day you log at least 4 hours (240 minutes) of validated deep study."
                            )
                        }
                )
            }
        }

        // --- 3. BADGES HEADER & FILTER CONTROLS (GREY VS COLORFUL) ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BATTLE ROYALE RANK BADGES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${claimedBadges.size}/${allBadges.size} CLAIMED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Filter Switcher: ALL / CLAIMED (COLORFUL) / LOCKED (GREY)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BadgeFilterOption.entries.forEach { option ->
                        val isSelected = selectedFilter == option
                        val count = when (option) {
                            BadgeFilterOption.ALL -> allBadges.size
                            BadgeFilterOption.CLAIMED -> claimedBadges.size
                            BadgeFilterOption.LOCKED -> lockedBadges.size
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    PixelHaptics.crispTick(view)
                                    selectedFilter = option
                                },
                            color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shadowElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Text(
                                text = "${option.label.substringBefore(" ")} ($count)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // --- 4. BATTLE ROYALE BADGES GRID ---
        if (displayedBadges.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (selectedFilter == BadgeFilterOption.CLAIMED) "No claimed badges yet. Complete daily protocol missions to claim ranks!" else "All badges have been claimed! Legendary conqueror status achieved.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(displayedBadges.chunked(2)) { badgePair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    badgePair.forEach { badge ->
                        PubgStyleBadgeCard(
                            badge = badge,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    PixelHaptics.crispTick(view)
                                    selectedBadgeForDetail = badge
                                },
                            onClaim = {
                                PixelHaptics.heavyUnlock(view)
                                onClaimBadge(badge.badgeId)
                            }
                        )
                    }
                    if (badgePair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // --- 5. DAILY 1-LINE REFLECTION EDITOR ---
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DAILY POST-ACTION REFLECTION",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "1 honest sentence on today's execution",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = reflectionInput,
                        onValueChange = { reflectionInput = it },
                        placeholder = { Text("What went well, what drifted, and what will you conquer tomorrow?") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp)
                    )

                    Button(
                        onClick = {
                            PixelHaptics.confirmCheck(view)
                            onUpdateReflection(reflectionInput)
                        },
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("SAVE REFLECTION", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // --- 6. IMMUTABLE HISTORICAL LOGBOOK ---
        item {
            Text(
                text = "HISTORICAL MISSION LOGBOOK",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(state.allLogsWithDetails) { snapshot ->
            ImmutableLogbookEntryCard(snapshot = snapshot)
        }
    }
}

@Composable
private fun PubgStyleBadgeCard(
    badge: BadgeEntity,
    modifier: Modifier = Modifier,
    onClaim: () -> Unit = {}
) {
    val isUnlocked = badge.isUnlocked
    val canClaim = !isUnlocked && badge.progressCurrent >= badge.progressTarget

    // Colorful gradient for claimed, dark steel grey for locked
    val glowBrush = if (isUnlocked) {
        Brush.linearGradient(listOf(Color(badge.accentHexStart), Color(badge.accentHexEnd)))
    } else {
        Brush.linearGradient(listOf(ZenithColors.LockedGreySurface, ZenithColors.LockedGreyCard))
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) {
                MaterialTheme.colorScheme.surface
            } else {
                ZenithColors.LockedGreyCard
            }
        ),
        border = BorderStroke(
            width = if (isUnlocked) 2.dp else 1.dp,
            color = if (isUnlocked) Color(badge.accentHexStart) else ZenithColors.LockedGreyStroke
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Crest & Stars / Lock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Battle Crest / Icon Box
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(glowBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isUnlocked) Icons.Outlined.MilitaryTech else Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = if (isUnlocked) Color(0xFF101014) else ZenithColors.LockedGreyIcon,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Battle Stars / Lock tag
                if (isUnlocked) {
                    Text(
                        text = "★★★★★",
                        fontSize = 11.sp,
                        color = Color(badge.accentHexStart),
                        fontWeight = FontWeight.Black
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ZenithColors.LockedGreySurface
                    ) {
                        Text(
                            text = "LOCKED",
                            style = MaterialTheme.typography.labelSmall,
                            color = ZenithColors.LockedGreyText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Title & Subtitle
            Text(
                text = badge.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else ZenithColors.LockedGreyText,
                maxLines = 1
            )
            Text(
                text = badge.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isUnlocked) Color(badge.accentHexStart) else ZenithColors.LockedGreyIcon,
                maxLines = 1
            )

            // Progress or Claim Button
            if (canClaim) {
                Button(
                    onClick = onClaim,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ZenithColors.AmberStart),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Text(
                        text = "CLAIM!",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF101014),
                        fontWeight = FontWeight.Black
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUnlocked) "CLAIMED" else "${badge.progressCurrent}/${badge.progressTarget}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUnlocked) Color(badge.accentHexStart) else ZenithColors.LockedGreyText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    LinearProgressIndicator(
                        progress = { (badge.progressCurrent.toFloat() / badge.progressTarget.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .width(55.dp)
                            .height(5.dp)
                            .clip(CircleShape),
                        color = if (isUnlocked) Color(badge.accentHexStart) else ZenithColors.LockedGreyIcon,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakMetricTile(title: String, count: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 9.sp
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
