package com.zenith90.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenith90.app.data.local.BadgeEntity
import com.zenith90.app.data.local.FullDaySnapshot
import com.zenith90.app.ui.viewmodel.ZenithUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun RewardsScreen(
    state: ZenithUiState,
    onUpdateReflection: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
    ) {
        item {
            Text(
                text = "STREAK & MILESTONE VAULT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StreakMetricTile("OVERALL STREAK", state.overallStreakDays, Modifier.weight(1f))
                StreakMetricTile("5 AM WAKE STREAK", state.wakeStreakDays, Modifier.weight(1f))
                StreakMetricTile("STUDY STREAK", state.studyStreakDays, Modifier.weight(1f))
            }
        }

        item {
            Text(
                text = "UNLOCKABLE MILESTONE BADGES",
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
                    BadgeVaultCard(badge = badge, modifier = Modifier.weight(1f))
                }
                if (badgePair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "IMMUTABLE DAILY LOGBOOK & AUDIT TRAIL",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search reflections, subjects, or dates...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
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

        items(filteredSnapshots, key = { it.dailyLog.dateEpochDay }) { snapshot ->
            ImmutableLogbookEntryCard(snapshot = snapshot)
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
            Spacer(Modifier.height(6.dp))
            Text(text = "$count", style = MaterialTheme.typography.displayMedium)
            Text(text = "DAYS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(text = badge.title, style = MaterialTheme.typography.labelSmall)
            Text(
                text = badge.criteriaDescription,
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
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${(snapshot.dailyLog.completionRatio * 100).toInt()}% SCORE",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Text(
                text = "Study: ${snapshot.dailyLog.totalStudyMinutes / 60}h ${snapshot.dailyLog.totalStudyMinutes % 60}m • " +
                    "Wake: ${snapshot.dailyLog.actualWakeTimeMinutes?.let { formatMinutesToTime(it) } ?: "Unlogged"} • " +
                    "Tasks Completed: ${snapshot.scheduleBlocks.count { it.isCompleted }}/${snapshot.scheduleBlocks.size}",
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
