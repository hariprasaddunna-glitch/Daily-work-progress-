package com.example.dwp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwp.model.*
import com.example.dwp.theme.*
import com.example.dwp.ui.components.*

@Composable
fun DashboardScreen(
    projects: List<Project>,
    tasksMap: Map<String, List<Task>>,
    onOpenProject: (String) -> Unit
) {
    val total = projects.size
    val done = projects.count { p -> p.status(p.overallProgress(tasksMap[p.id] ?: emptyList())) == "done" }
    val inProgress = projects.count { p -> p.status(p.overallProgress(tasksMap[p.id] ?: emptyList())) == "progress" }
    val behind = projects.count { p -> p.status(p.overallProgress(tasksMap[p.id] ?: emptyList())) == "behind" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Metrics 4-grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        label = "Total Projects",
                        value = "$total",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label = "Completed",
                        value = "$done",
                        valueColor = TealSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        label = "In Progress",
                        value = "$inProgress",
                        valueColor = NavySecondary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label = "Behind",
                        value = "$behind",
                        valueColor = RedAlert,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Progress by Project card
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Progress by Project",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val sortedByProgress = projects.sortedBy { p ->
                        p.overallProgress(tasksMap[p.id] ?: emptyList())
                    }

                    if (sortedByProgress.isEmpty()) {
                        Text(
                            text = "No projects yet.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    } else {
                        sortedByProgress.forEach { proj ->
                            val progress = proj.overallProgress(tasksMap[proj.id] ?: emptyList())
                            val status = proj.status(progress)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenProject(proj.id) }
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = proj.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextDark,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatusChip(status = status)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                ProgressBarWithText(progress = progress)
                            }
                            HorizontalDivider(color = BorderLine.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }

        // In-charge Workload table card
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "In-charge Workload",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Compute workload
                    val workloadMap = mutableMapOf<String, InchargeWorkload>()
                    projects.forEach { p ->
                        val name = p.incharge.ifBlank { "Unassigned" }
                        val cur = workloadMap[name] ?: InchargeWorkload(name)
                        val progress = p.overallProgress(tasksMap[p.id] ?: emptyList())
                        val status = p.status(progress)
                        val daysToAtd = p.daysToArrival()

                        val isDone = status == "done"
                        val isUpcoming = !isDone && daysToAtd != null && daysToAtd > 0
                        val isRunning = !isDone && !isUpcoming

                        workloadMap[name] = cur.copy(
                            total = cur.total + 1,
                            completed = cur.completed + if (isDone) 1 else 0,
                            upcoming = cur.upcoming + if (isUpcoming) 1 else 0,
                            running = cur.running + if (isRunning) 1 else 0
                        )
                    }

                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SteelLight, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("In-charge", modifier = Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Text("Running", modifier = Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Text("Done", modifier = Modifier.weight(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Text("Upcoming", modifier = Modifier.weight(0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Text("Total", modifier = Modifier.weight(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    }

                    workloadMap.values.sortedBy { it.name }.forEach { w ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(w.name, modifier = Modifier.weight(1.5f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("${w.running}", modifier = Modifier.weight(0.8f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text("${w.completed}", modifier = Modifier.weight(0.7f), fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = TealSuccess)
                            Text("${w.upcoming}", modifier = Modifier.weight(0.9f), fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = AmberWarning)
                            Text("${w.total}", modifier = Modifier.weight(0.7f), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = BorderLine.copy(alpha = 0.3f), thickness = 0.5.dp)
                    }
                }
            }
        }

        // Upcoming projects & Sailing out soon cards
        item {
            val today = java.time.LocalDate.now().toString()
            val upcomingProjects = projects.filter {
                val d = it.daysToArrival()
                d != null && d > 0
            }.sortedBy { it.daysToArrival() }

            val sailOutProjects = projects.filter {
                val progress = it.overallProgress(tasksMap[it.id] ?: emptyList())
                val status = it.status(progress)
                val d = it.daysToDeparture()
                status != "done" && d != null && d in 0..7
            }.sortedBy { it.daysToDeparture() }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Upcoming
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DirectionsBoat, contentDescription = null, tint = NavySecondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upcoming Projects (Not Yet Arrived)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        if (upcomingProjects.isEmpty()) {
                            Text("No upcoming arrivals.", fontSize = 12.sp, color = TextMuted)
                        } else {
                            upcomingProjects.forEach { p ->
                                val days = p.daysToArrival() ?: 0
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenProject(p.id) }
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(p.name, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = TextDark)
                                        Text("In-charge: ${p.incharge.ifBlank { "—" }}", fontSize = 11.sp, color = TextMuted)
                                    }
                                    Text("Arrives in $days d (${p.atd})", fontSize = 11.5.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                // Sailing out soon
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sailing Out Soon (Within 7 Days)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        if (sailOutProjects.isEmpty()) {
                            Text("Nothing sailing out in the next 7 days.", fontSize = 12.sp, color = TextMuted)
                        } else {
                            sailOutProjects.forEach { p ->
                                val days = p.daysToDeparture() ?: 0
                                val progress = p.overallProgress(tasksMap[p.id] ?: emptyList())
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenProject(p.id) }
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(if (days <= 2) RedAlert else AmberWarning)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(p.name, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = TextDark)
                                            Text("${p.incharge.ifBlank { "—" }} · $progress% done", fontSize = 11.sp, color = TextMuted)
                                        }
                                    }
                                    Text(
                                        if (days == 0L) "Today" else "In $days d (${p.etdExp})",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (days <= 2) RedAlert else AmberWarning
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
