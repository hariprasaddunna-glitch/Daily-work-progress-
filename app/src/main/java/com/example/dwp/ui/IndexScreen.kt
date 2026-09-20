package com.example.dwp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun IndexScreen(
    projects: List<Project>,
    tasksMap: Map<String, List<Task>>,
    currentUser: CurrentUser,
    inchargeRoster: List<String>,
    foremanRoster: List<String>,
    onOpenProject: (String) -> Unit,
    onSaveProject: (Project) -> Unit,
    onDeleteProject: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("") }
    var selectedPriorityFilter by remember { mutableStateOf("") }
    var selectedInchargeFilter by remember { mutableStateOf("") }

    var showProjectDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<Project?>(null) }
    var projectToDelete by remember { mutableStateOf<Project?>(null) }

    // Filter projects based on user permissions and search/filters
    val scopedProjects = remember(projects, currentUser) {
        if (currentUser.role == UserRole.ENGINEER) {
            projects.filter { currentUser.canEditProject(it) }
        } else {
            projects
        }
    }

    val filteredProjects = remember(
        scopedProjects,
        searchQuery,
        selectedStatusFilter,
        selectedPriorityFilter,
        selectedInchargeFilter,
        tasksMap
    ) {
        scopedProjects.filter { p ->
            val progress = p.overallProgress(tasksMap[p.id] ?: emptyList())
            val status = p.status(progress)
            val priority = p.priority()

            if (selectedStatusFilter.isNotBlank() && status != selectedStatusFilter) return@filter false
            if (selectedPriorityFilter.isNotBlank() && priority != selectedPriorityFilter) return@filter false
            if (selectedInchargeFilter.isNotBlank() && p.incharge != selectedInchargeFilter) return@filter false

            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                val match = p.name.lowercase().contains(q) ||
                        p.incharge.lowercase().contains(q) ||
                        p.foreman.lowercase().contains(q) ||
                        p.atd.contains(q) ||
                        p.etdExp.contains(q)
                if (!match) return@filter false
            }
            true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Action & Metrics Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Index — All Projects",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
                if (currentUser.canManageProjects()) {
                    Button(
                        onClick = {
                            editingProject = null
                            showProjectDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavySecondary),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("new_project_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Project", fontSize = 12.5.sp)
                    }
                }
            }
        }

        // Metrics Row
        item {
            val total = scopedProjects.size
            val done = scopedProjects.count { p -> p.status(p.overallProgress(tasksMap[p.id] ?: emptyList())) == "done" }
            val inProgress = scopedProjects.count { p -> p.status(p.overallProgress(tasksMap[p.id] ?: emptyList())) == "progress" }
            val behind = scopedProjects.count { p -> p.status(p.overallProgress(tasksMap[p.id] ?: emptyList())) == "behind" }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard(label = "Total", value = "$total", modifier = Modifier.weight(1f))
                MetricCard(label = "Done", value = "$done", valueColor = TealSuccess, modifier = Modifier.weight(1f))
                MetricCard(label = "In Prog", value = "$inProgress", valueColor = NavySecondary, modifier = Modifier.weight(1f))
                MetricCard(label = "Behind", value = "$behind", valueColor = RedAlert, modifier = Modifier.weight(1f))
            }
        }

        // Filter Bar Card
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
                            .testTag("index_search_input"),
                        placeholder = { Text("Search by name, in-charge, dates...", fontSize = 12.5.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status filter
                        FilterDropdown(
                            label = "Status",
                            selected = selectedStatusFilter,
                            options = listOf("" to "All Status", "done" to "Completed", "progress" to "In Progress", "behind" to "Behind"),
                            onSelected = { selectedStatusFilter = it },
                            modifier = Modifier.weight(1f)
                        )
                        // Priority filter
                        FilterDropdown(
                            label = "Priority",
                            selected = selectedPriorityFilter,
                            options = listOf("" to "All Priority", "high" to "High", "medium" to "Medium", "low" to "Low"),
                            onSelected = { selectedPriorityFilter = it },
                            modifier = Modifier.weight(1f)
                        )
                        // Clear
                        if (searchQuery.isNotEmpty() || selectedStatusFilter.isNotEmpty() || selectedPriorityFilter.isNotEmpty() || selectedInchargeFilter.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedStatusFilter = ""
                                    selectedPriorityFilter = ""
                                    selectedInchargeFilter = ""
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("Clear", fontSize = 11.sp, color = RedAlert)
                            }
                        }
                    }
                }
            }
        }

        // Projects List
        if (filteredProjects.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No projects match the current filters.", color = TextMuted, fontSize = 13.sp)
                }
            }
        } else {
            items(filteredProjects, key = { it.id }) { project ->
                val progress = project.overallProgress(tasksMap[project.id] ?: emptyList())
                val status = project.status(progress)
                val priority = project.priority()
                val daysDuration = project.daysDuration()

                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Title row with chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${project.seq} · ${project.name}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PriorityChip(priority = priority)
                                StatusChip(status = status)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Incharge and dates
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "In-charge: ${project.incharge.ifBlank { "—" }} · Foreman: ${project.foreman.ifBlank { "—" }}",
                                fontSize = 11.5.sp,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ATD: ${project.atd.ifBlank { "—" }}  |  ETD: ${project.etdExp.ifBlank { "—" }}" +
                                        if (daysDuration != null) " ($daysDuration days)" else "",
                                fontSize = 11.5.sp,
                                color = TextMuted
                            )
                            if (project.estMH > 0) {
                                Text(
                                    text = "Est: ${project.estMH.toInt()} MH",
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Steel
                                )
                            }
                        }

                        if (project.managerMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AmberWarningBg, RoundedCornerShape(4.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "📌 ${project.managerMessage}",
                                    fontSize = 11.sp,
                                    color = TextDark,
                                    maxLines = 2
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        ProgressBarWithText(progress = progress)

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = BorderLine.copy(alpha = 0.5f), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(6.dp))

                        // Action buttons row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { onOpenProject(project.id) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("open_project_${project.id}")
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open Sheet", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NavySecondary)
                            }

                            if (currentUser.canManageProjects()) {
                                TextButton(
                                    onClick = {
                                        editingProject = project
                                        showProjectDialog = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("edit_project_${project.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit", fontSize = 12.sp, color = NavySecondary)
                                }

                                TextButton(
                                    onClick = { projectToDelete = project },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("delete_project_${project.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = RedAlert)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete", fontSize = 12.sp, color = RedAlert)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Project Dialog
    if (showProjectDialog) {
        ProjectEditDialog(
            project = editingProject,
            existingProjectsCount = projects.size,
            inchargeRoster = inchargeRoster,
            foremanRoster = foremanRoster,
            onDismiss = { showProjectDialog = false },
            onSave = { saved ->
                onSaveProject(saved)
                showProjectDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (projectToDelete != null) {
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete Project?") },
            text = { Text("Are you sure you want to delete \"${projectToDelete?.name}\" and all of its job entries? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        projectToDelete?.let { onDeleteProject(it.id) }
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FilterDropdown(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val displayLabel = options.find { it.first == selected }?.second ?: label

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(displayLabel, fontSize = 11.5.sp, maxLines = 1, color = TextDark)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, title) ->
                DropdownMenuItem(
                    text = { Text(title, fontSize = 12.sp) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectEditDialog(
    project: Project?,
    existingProjectsCount: Int,
    inchargeRoster: List<String>,
    foremanRoster: List<String>,
    onDismiss: () -> Unit,
    onSave: (Project) -> Unit
) {
    var seqText by remember { mutableStateOf(project?.seq?.toString() ?: (existingProjectsCount + 1).toString()) }
    var name by remember { mutableStateOf(project?.name ?: "") }
    var incharge by remember { mutableStateOf(project?.incharge ?: "") }
    var foreman by remember { mutableStateOf(project?.foreman ?: "") }
    var estMhText by remember { mutableStateOf(if ((project?.estMH ?: 0.0) > 0) project?.estMH?.toInt()?.toString() ?: "" else "") }
    var atd by remember { mutableStateOf(project?.atd ?: java.time.LocalDate.now().toString()) }
    var etdOrig by remember { mutableStateOf(project?.etdOrig ?: "") }
    var etdExp by remember { mutableStateOf(project?.etdExp ?: java.time.LocalDate.now().plusDays(14).toString()) }
    var managerMessage by remember { mutableStateOf(project?.managerMessage ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (project == null) "New Project" else "Edit Project",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    item {
                        Text(errorMessage!!, color = RedAlert, fontSize = 12.sp)
                    }
                }
                item {
                    OutlinedTextField(
                        value = seqText,
                        onValueChange = { seqText = it },
                        label = { Text("Display Order", fontSize = 11.5.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Project No & Name *", fontSize = 11.5.sp) },
                        placeholder = { Text("e.g. PRJ-005 Bulk Carrier Piping", fontSize = 11.5.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    RosterAutoCompleteField(
                        label = "Project In-charge",
                        value = incharge,
                        onValueChange = { incharge = it },
                        roster = inchargeRoster
                    )
                }
                item {
                    RosterAutoCompleteField(
                        label = "Project Foreman",
                        value = foreman,
                        onValueChange = { foreman = it },
                        roster = foremanRoster
                    )
                }
                item {
                    OutlinedTextField(
                        value = estMhText,
                        onValueChange = { estMhText = it },
                        label = { Text("Estimated Manhours", fontSize = 11.5.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = atd,
                            onValueChange = { atd = it },
                            label = { Text("Arrival (ATD) *", fontSize = 11.sp) },
                            placeholder = { Text("YYYY-MM-DD", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = etdExp,
                            onValueChange = { etdExp = it },
                            label = { Text("Expected ETD *", fontSize = 11.sp) },
                            placeholder = { Text("YYYY-MM-DD", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = etdOrig,
                        onValueChange = { etdOrig = it },
                        label = { Text("Original Departure (ETD)", fontSize = 11.5.sp) },
                        placeholder = { Text("YYYY-MM-DD", fontSize = 11.5.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = managerMessage,
                        onValueChange = { managerMessage = it },
                        label = { Text("Manager Note to Engineer", fontSize = 11.5.sp) },
                        placeholder = { Text("Priorities, site conditions, inspection deadlines...", fontSize = 11.5.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || atd.isBlank() || etdExp.isBlank()) {
                        errorMessage = "Project name, arrival date, and expected departure are required."
                        return@Button
                    }
                    val id = project?.id ?: "PRJ-${System.currentTimeMillis().toString().takeLast(5)}"
                    val seq = seqText.toIntOrNull() ?: (existingProjectsCount + 1)
                    val estMh = estMhText.toDoubleOrNull() ?: 0.0

                    val result = Project(
                        id = id,
                        seq = seq,
                        name = name.trim(),
                        incharge = incharge.trim(),
                        foreman = foreman.trim(),
                        estMH = estMh,
                        atd = atd.trim(),
                        etdOrig = etdOrig.trim(),
                        etdExp = etdExp.trim(),
                        managerMessage = managerMessage.trim()
                    )
                    onSave(result)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavySecondary)
            ) {
                Text("Save Project")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RosterAutoCompleteField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    roster: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label, fontSize = 11.5.sp) },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        if (expanded && roster.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(4.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    roster.forEach { name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onValueChange(name)
                                    expanded = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(name, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
