package com.example.dwp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwp.model.*
import com.example.dwp.theme.*
import com.example.dwp.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    projects: List<Project>,
    tasksMap: Map<String, List<Task>>,
    currentUser: CurrentUser,
    onOpenProject: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("") }
    var activeOnly by remember { mutableStateOf(false) }

    val scopedProjects = remember(projects, currentUser) {
        if (currentUser.role == UserRole.ENGINEER) {
            projects.filter { currentUser.canEditProject(it) }
        } else {
            projects
        }
    }

    // Filter projects and active jobs
    val filteredProjectGroups = remember(
        scopedProjects,
        tasksMap,
        searchQuery,
        selectedStatusFilter,
        activeOnly
    ) {
        scopedProjects.sortedBy { it.seq }.mapNotNull { project ->
            val progress = project.overallProgress(tasksMap[project.id] ?: emptyList())
            val status = project.status(progress)

            if (activeOnly && status == "done") return@mapNotNull null

            val allProjectTasks = tasksMap[project.id] ?: emptyList()
            // In original app: cancelled tasks are excluded from summary
            val activeTasks = allProjectTasks.filter { !it.cancelled }

            val filteredTasks = activeTasks.filter { t ->
                val taskStatus = t.effectiveStatus()
                if (selectedStatusFilter.isNotBlank() && taskStatus != selectedStatusFilter) return@filter false

                if (searchQuery.isNotBlank()) {
                    val q = searchQuery.trim().lowercase()
                    val match = t.displayLocation.lowercase().contains(q) ||
                            t.job.lowercase().contains(q) ||
                            t.owner.lowercase().contains(q) ||
                            t.remarks.lowercase().contains(q)
                    if (!match) return@filter false
                }
                true
            }

            if (filteredTasks.isNotEmpty()) {
                project to filteredTasks
            } else {
                null
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Summary — Grouped by Display Order",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )
        }

        // Filter Controls
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("summary_search_input"),
                        placeholder = { Text("Search jobs, location, remarks...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterDropdown(
                            label = "Status",
                            selected = selectedStatusFilter,
                            options = listOf("" to "All Status", "done" to "Completed", "progress" to "In Progress", "behind" to "Behind"),
                            onSelected = { selectedStatusFilter = it },
                            modifier = Modifier.width(150.dp)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = activeOnly,
                                onCheckedChange = { activeOnly = it },
                                modifier = Modifier.testTag("active_only_checkbox")
                            )
                            Text("Active projects only", fontSize = 11.5.sp, color = TextDark)
                        }
                    }
                }
            }
        }

        if (filteredProjectGroups.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No job entries match the current filters.", color = TextMuted, fontSize = 13.sp)
                }
            }
        } else {
            items(filteredProjectGroups, key = { it.first.id }) { (project, tasks) ->
                val progress = project.overallProgress(tasksMap[project.id] ?: emptyList())
                val status = project.status(progress)
                val priority = project.priority()

                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Project Group Header
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SteelLight)
                                .clickable { onOpenProject(project.id) }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${project.name} (#${project.seq})",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    PriorityChip(priority = priority)
                                    StatusChip(status = status)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${project.incharge.ifBlank { "—" }} · ${project.foreman.ifBlank { "—" }} · ETD ${project.etdExp.ifBlank { "—" }}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = "$progress% overall",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NavySecondary
                                )
                            }
                        }

                        // Jobs within group
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            tasks.forEach { task ->
                                SummaryTaskRow(task = task)
                                HorizontalDivider(color = BorderLine.copy(alpha = 0.4f), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryTaskRow(task: Task) {
    val status = task.effectiveStatus()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = task.job,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark,
                modifier = Modifier.weight(1f)
            )
            StatusChip(status = status)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "📍 ${task.displayLocation}" + if (task.owner.isNotBlank()) " · Owner: ${task.owner}" else "",
                fontSize = 11.sp,
                color = TextMuted
            )
            Text(
                text = "${task.start} → ${task.end}",
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Stages breakdown: Qty, Removal, Fab, Install
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HullBackground, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Qty: ${task.qty} ${task.unit}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text("Rem: ${task.removal}/${task.qty}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text("Fab: ${task.fab}/${task.qty}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text("Inst: ${task.install}/${task.qty}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(4.dp))
        ProgressBarWithText(progress = task.totalProgress)

        if (task.remarks.isNotBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Note: ${task.remarks}",
                fontSize = 10.5.sp,
                color = TextMuted,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}
