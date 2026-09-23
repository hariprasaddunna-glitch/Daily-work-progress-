package com.example.dwp.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import kotlin.math.min
import kotlin.math.roundToInt
import com.example.dwp.model.*
import com.example.dwp.theme.*
import com.example.dwp.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSheetScreen(
    projects: List<Project>,
    tasksMap: Map<String, List<Task>>,
    selectedProjectId: String?,
    currentUser: CurrentUser,
    onSelectProject: (String) -> Unit,
    onSaveTask: (Task) -> Unit,
    onCancelTask: (String, String) -> Unit,
    onReinstateTask: (String, String) -> Unit
) {
    val context = LocalContext.current

    val scopedProjects = remember(projects, currentUser) {
        if (currentUser.role == UserRole.ENGINEER) {
            projects.filter { currentUser.canEditProject(it) }
        } else {
            projects
        }
    }

    val currentProject = remember(scopedProjects, selectedProjectId) {
        scopedProjects.find { it.id == selectedProjectId } ?: scopedProjects.firstOrNull()
    }

    var showAddJobForm by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }

    // Search and filters for tasks
    var taskSearchQuery by remember { mutableStateOf("") }
    var selectedTaskStatusFilter by remember { mutableStateOf("") }
    var selectedLocationFilter by remember { mutableStateOf("") }

    if (currentProject == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No projects available or assigned to you.", color = TextMuted, fontSize = 14.sp)
        }
        return
    }

    val allTasks = tasksMap[currentProject.id] ?: emptyList()
    val canEdit = currentUser.canEditProject(currentProject)

    val filteredTasks = remember(
        allTasks,
        taskSearchQuery,
        selectedTaskStatusFilter,
        selectedLocationFilter
    ) {
        allTasks.filter { t ->
            val status = t.effectiveStatus()
            if (selectedTaskStatusFilter.isNotBlank() && status != selectedTaskStatusFilter) return@filter false
            if (selectedLocationFilter.isNotBlank() && !t.location.equals(selectedLocationFilter, ignoreCase = true)) return@filter false

            if (taskSearchQuery.isNotBlank()) {
                val q = taskSearchQuery.trim().lowercase()
                val match = t.job.lowercase().contains(q) ||
                        t.displayLocation.lowercase().contains(q) ||
                        t.owner.lowercase().contains(q) ||
                        t.remarks.lowercase().contains(q)
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
        // Project Selector & Share Bar
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Project Sheet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        IconButton(
                            onClick = {
                                val active = allTasks.filter { !it.cancelled }
                                val text = buildString {
                                    appendLine("DAILY WORK PROGRESS REPORT")
                                    appendLine("Project: ${currentProject.name}")
                                    appendLine("In-charge: ${currentProject.incharge}")
                                    appendLine("Foreman: ${currentProject.foreman}")
                                    appendLine("ATD: ${currentProject.atd} | ETD: ${currentProject.etdExp}")
                                    appendLine("Overall Progress: ${currentProject.overallProgress(allTasks)}%")
                                    appendLine("---")
                                    appendLine("Active Jobs (${active.size}):")
                                    active.forEachIndexed { i, t ->
                                        appendLine("${i + 1}. [${t.displayLocation}] ${t.job} - Progress: ${t.totalProgress}% (Rem: ${t.removal}, Fab: ${t.fab}, Inst: ${t.install} of ${t.qty})")
                                    }
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, text)
                                    putExtra(Intent.EXTRA_SUBJECT, "Daily Work Progress - ${currentProject.name}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Project Summary"))
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share report", tint = NavySecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Project dropdown selector
                    ProjectDropdownSelector(
                        projects = scopedProjects,
                        selectedId = currentProject.id,
                        onSelect = { onSelectProject(it) }
                    )
                }
            }
        }

        // Project Metadata banner
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = SteelLight),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("In-charge: ${currentProject.incharge.ifBlank { "—" }}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("Foreman: ${currentProject.foreman.ifBlank { "—" }}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ATD: ${currentProject.atd.ifBlank { "—" }}", fontSize = 11.5.sp, color = TextMuted)
                        Text("Expected ETD: ${currentProject.etdExp.ifBlank { "—" }}", fontSize = 11.5.sp, color = TextMuted)
                    }
                    if (currentProject.estMH > 0) {
                        Text("Est. Manhours: ${currentProject.estMH.toInt()} MH", fontSize = 11.5.sp, color = TextMuted)
                    }
                }
            }
        }

        // Manager Message if present
        if (currentProject.managerMessage.isNotBlank()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AmberWarningBg, RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("📌", fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Manager's Message:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                            Text(currentProject.managerMessage, fontSize = 12.sp, color = TextDark)
                        }
                    }
                }
            }
        }

        // Add Job Button & Excel-Type Multi-Line Entry
        if (canEdit) {
            item {
                Button(
                    onClick = { showAddJobForm = !showAddJobForm },
                    colors = ButtonDefaults.buttonColors(containerColor = NavySecondary),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("toggle_add_job_button")
                ) {
                    Icon(
                        if (showAddJobForm) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showAddJobForm) "Close Excel Entry" else "＋ Add Jobs (Excel Entry) to ${currentProject.name}")
                }
            }

            if (showAddJobForm) {
                item {
                    ExcelJobEntryCard(
                        projectId = currentProject.id,
                        currentUser = currentUser,
                        onCancel = { showAddJobForm = false },
                        onSaveAll = { tasks ->
                            tasks.forEach { onSaveTask(it) }
                            showAddJobForm = false
                        }
                    )
                }
            }
        }

        // Filter Bar for tasks
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = taskSearchQuery,
                        onValueChange = { taskSearchQuery = it },
                        placeholder = { Text("Search jobs, location, remarks...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (taskSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { taskSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterDropdown(
                            label = "Status",
                            selected = selectedTaskStatusFilter,
                            options = listOf("" to "All Status", "done" to "Completed", "progress" to "In Progress", "behind" to "Behind", "cancelled" to "Cancelled"),
                            onSelected = { selectedTaskStatusFilter = it },
                            modifier = Modifier.weight(1f)
                        )
                        FilterDropdown(
                            label = "Location",
                            selected = selectedLocationFilter,
                            options = listOf("" to "All Locations", "Engine Room" to "Engine Room", "Main Deck" to "Main Deck", "Pump Room" to "Pump Room", "Tank" to "Tank", "Other" to "Other"),
                            onSelected = { selectedLocationFilter = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Job entries list
        if (filteredTasks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No job entries match the current filters.", color = TextMuted, fontSize = 13.sp)
                }
            }
        } else {
            items(filteredTasks, key = { it.id }) { task ->
                JobItemCard(
                    task = task,
                    canEdit = canEdit,
                    onEdit = { editingTask = task },
                    onCancel = { onCancelTask(task.projectId, task.id) },
                    onReinstate = { onReinstateTask(task.projectId, task.id) }
                )
            }
        }
    }

    // Edit Job Dialog
    if (editingTask != null) {
        AlertDialog(
            onDismissRequest = { editingTask = null },
            title = { Text("Edit Job Entry", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyPrimary) },
            text = {
                JobFormCard(
                    projectId = currentProject.id,
                    existingJob = editingTask,
                    onCancel = { editingTask = null },
                    onSave = { updated ->
                        onSaveTask(updated)
                        editingTask = null
                    }
                )
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}

@Composable
fun JobItemCard(
    task: Task,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onReinstate: () -> Unit
) {
    val status = task.effectiveStatus()

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.cancelled) Color(0xFFFAFAF8) else SurfaceCard
        ),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Location & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📍 ${task.displayLocation}" + if (task.owner.isNotBlank()) " · #${task.owner}" else "",
                    fontSize = 11.5.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )
                StatusChip(status = status)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Job description (strikethrough if cancelled)
            Text(
                text = task.job,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (task.cancelled) TextMuted else TextDark,
                textDecoration = if (task.cancelled) TextDecoration.LineThrough else TextDecoration.None
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Dates & Stage counters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (task.start.isNotBlank() || task.end.isNotBlank()) "${task.start} → ${task.end}" else "Dates not set",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
                Text(
                    text = "Qty: ${task.qty} ${task.unit}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress Stages breakdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SteelLight.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Removal", fontSize = 10.sp, color = TextMuted)
                    Text("${task.removal.toInt()}/${task.qty.toInt()}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Fab/OH", fontSize = 10.sp, color = TextMuted)
                    Text("${task.fab.toInt()}/${task.qty.toInt()}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Install", fontSize = 10.sp, color = TextMuted)
                    Text("${task.install.toInt()}/${task.qty.toInt()}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            ProgressBarWithText(progress = task.totalProgress)

            if (task.remarks.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Remarks: ${task.remarks}",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            if (canEdit) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = BorderLine.copy(alpha = 0.5f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (task.cancelled) {
                        TextButton(
                            onClick = onReinstate,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Reinstate", fontSize = 11.5.sp, color = TealSuccess)
                        }
                    } else {
                        TextButton(
                            onClick = onEdit,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Edit", fontSize = 11.5.sp, color = NavySecondary)
                        }
                        TextButton(
                            onClick = onCancel,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Cancel", fontSize = 11.5.sp, color = RedAlert)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JobFormCard(
    projectId: String,
    existingJob: Task?,
    onCancel: () -> Unit,
    onSave: (Task) -> Unit
) {
    var location by remember { mutableStateOf(existingJob?.location ?: "Engine Room") }
    var locationOther by remember { mutableStateOf(existingJob?.locationOther ?: "") }
    var owner by remember { mutableStateOf(existingJob?.owner ?: "") }
    var job by remember { mutableStateOf(existingJob?.job ?: "") }
    var start by remember { mutableStateOf(existingJob?.start ?: java.time.LocalDate.now().toString()) }
    var end by remember { mutableStateOf(existingJob?.end ?: java.time.LocalDate.now().plusDays(5).toString()) }
    var unit by remember { mutableStateOf(existingJob?.unit ?: "pcs") }
    var qtyText by remember { mutableStateOf(if ((existingJob?.qty ?: 0.0) > 0) existingJob?.qty?.toInt()?.toString() ?: "" else "1") }
    var removalText by remember { mutableStateOf(existingJob?.removal?.toInt()?.toString() ?: "0") }
    var fabText by remember { mutableStateOf(existingJob?.fab?.toInt()?.toString() ?: "0") }
    var installText by remember { mutableStateOf(existingJob?.install?.toInt()?.toString() ?: "0") }
    var mhCal by remember { mutableStateOf(existingJob?.mhCal ?: "") }
    var mhJob by remember { mutableStateOf(existingJob?.mhJob ?: "") }
    var remarks by remember { mutableStateOf(existingJob?.remarks ?: "") }

    var formError by remember { mutableStateOf<String?>(null) }

    // Live preview calculation
    val qty = qtyText.toDoubleOrNull() ?: 0.0
    val removal = removalText.toDoubleOrNull() ?: 0.0
    val fab = fabText.toDoubleOrNull() ?: 0.0
    val install = installText.toDoubleOrNull() ?: 0.0

    val previewProgress = remember(qty, removal, fab, install) {
        computeTaskProgress(qty, removal, fab, install)
    }

    val stageError = remember(qty, removal, fab, install) {
        when {
            qty <= 0 -> "Qty must be greater than 0."
            removal < 0 || fab < 0 || install < 0 -> "Stage quantities cannot be negative."
            removal > qty -> "Removal cannot exceed Qty."
            fab > qty -> "Fabrication/OH cannot exceed Qty."
            install > qty -> "Installation cannot exceed Qty."
            else -> null
        }
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (existingJob == null) "New Job Entry" else "Edit Job Entry",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )

            if (formError != null) {
                Text(formError!!, color = RedAlert, fontSize = 11.5.sp)
            }

            // Location selector
            FilterDropdown(
                label = "Location *",
                selected = location,
                options = listOf(
                    "Engine Room" to "Engine Room",
                    "Main Deck" to "Main Deck",
                    "Pump Room" to "Pump Room",
                    "Tank" to "Tank",
                    "Other" to "Other"
                ),
                onSelected = { location = it },
                modifier = Modifier.fillMaxWidth()
            )

            if (location == "Other") {
                OutlinedTextField(
                    value = locationOther,
                    onValueChange = { locationOther = it },
                    label = { Text("Specify Location", fontSize = 11.5.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = job,
                onValueChange = { job = it },
                label = { Text("Job Description *", fontSize = 11.5.sp) },
                placeholder = { Text("e.g. Renew 4\" Cargo line elbow", fontSize = 11.5.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = owner,
                    onValueChange = { owner = it },
                    label = { Text("Owner #", fontSize = 11.sp) },
                    placeholder = { Text("OWN-4521", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                FilterDropdown(
                    label = "Unit",
                    selected = unit,
                    options = listOf("pcs" to "pcs", "m" to "m", "set" to "set", "lot" to "lot"),
                    onSelected = { unit = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = { Text("Start Date", fontSize = 11.sp) },
                    placeholder = { Text("YYYY-MM-DD", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = { Text("End Date", fontSize = 11.sp) },
                    placeholder = { Text("YYYY-MM-DD", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Qty & Stages
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("Qty *", fontSize = 10.5.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = removalText,
                    onValueChange = { removalText = it },
                    label = { Text("Removal", fontSize = 10.5.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = fabText,
                    onValueChange = { fabText = it },
                    label = { Text("Fab/OH", fontSize = 10.5.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = installText,
                    onValueChange = { installText = it },
                    label = { Text("Install", fontSize = 10.5.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Live progress preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SteelLight, RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                if (stageError != null) {
                    Text(stageError, color = RedAlert, fontSize = 11.sp)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto Progress Preview:", fontSize = 11.sp, color = TextDark)
                        Text("$previewProgress%", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TealSuccess)
                    }
                }
            }

            OutlinedTextField(
                value = remarks,
                onValueChange = { remarks = it },
                label = { Text("Remarks", fontSize = 11.5.sp) },
                placeholder = { Text("Inspection notes, hydro test results...", fontSize = 11.5.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Submit Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (job.isBlank()) {
                            formError = "Job description is required."
                            return@Button
                        }
                        if (start.isNotBlank() && end.isNotBlank() && end < start) {
                            formError = "End date cannot be before start date."
                            return@Button
                        }
                        if (stageError != null) {
                            formError = stageError
                            return@Button
                        }

                        val id = existingJob?.id ?: "DWP-${System.currentTimeMillis().toString().takeLast(6)}"
                        val task = Task(
                            id = id,
                            projectId = projectId,
                            sharePointId = existingJob?.sharePointId ?: "",
                            location = location,
                            locationOther = locationOther.trim(),
                            owner = owner.trim(),
                            job = job.trim(),
                            start = start.trim(),
                            end = end.trim(),
                            unit = unit,
                            qty = qty,
                            removal = removal,
                            fab = fab,
                            install = install,
                            totalProgress = previewProgress,
                            status = if (previewProgress >= 100) "done" else "progress",
                            cancelled = existingJob?.cancelled ?: false,
                            mhCal = mhCal.trim(),
                            mhJob = mhJob.trim(),
                            remarks = remarks.trim()
                        )
                        onSave(task)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavySecondary),
                    modifier = Modifier.testTag("save_job_button")
                ) {
                    Text(if (existingJob == null) "Add Job Entry" else "Save Changes")
                }
            }
        }
    }
}

data class ExcelDraftLine(
    val id: String = UUID.randomUUID().toString(),
    var location: String = "Engine Room",
    var locationOther: String = "",
    var owner: String = "",
    var job: String = "",
    var start: String = "",
    var end: String = "",
    var unit: String = "pcs",
    var qtyText: String = "1",
    var removalText: String = "0",
    var fabText: String = "0",
    var installText: String = "0",
    var mhCal: String = "",
    var mhJob: String = "",
    var remarks: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelJobEntryCard(
    projectId: String,
    currentUser: CurrentUser,
    onCancel: () -> Unit,
    onSaveAll: (List<Task>) -> Unit
) {
    val draftLines = remember {
        mutableStateListOf(ExcelDraftLine())
    }
    var formError by remember { mutableStateOf<String?>(null) }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("excel_job_entry_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Excel-Type Job Entry",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Text(
                        text = "Spreadsheet multi-line entry (${draftLines.size} line${if (draftLines.size == 1) "" else "s"})",
                        fontSize = 11.5.sp,
                        color = TextMuted
                    )
                }
                Button(
                    onClick = {
                        draftLines.add(ExcelDraftLine())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Line", fontSize = 12.sp)
                }
            }

            if (formError != null) {
                Text(formError!!, color = RedAlert, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
            }

            // Each Line Card
            draftLines.forEachIndexed { index, line ->
                val lineKey = line.id
                var loc by remember(lineKey) { mutableStateOf(line.location) }
                var locOther by remember(lineKey) { mutableStateOf(line.locationOther) }
                var owner by remember(lineKey) { mutableStateOf(line.owner) }
                var jobDesc by remember(lineKey) { mutableStateOf(line.job) }
                var startDate by remember(lineKey) { mutableStateOf(line.start) }
                var endDate by remember(lineKey) { mutableStateOf(line.end) }
                var unit by remember(lineKey) { mutableStateOf(line.unit) }
                var qty by remember(lineKey) { mutableStateOf(line.qtyText) }
                var rem by remember(lineKey) { mutableStateOf(line.removalText) }
                var fab by remember(lineKey) { mutableStateOf(line.fabText) }
                var inst by remember(lineKey) { mutableStateOf(line.installText) }
                var mhCal by remember(lineKey) { mutableStateOf(line.mhCal) }
                var mhJob by remember(lineKey) { mutableStateOf(line.mhJob) }
                var remarks by remember(lineKey) { mutableStateOf(line.remarks) }

                // Synchronize back to draftLines
                LaunchedEffect(loc, locOther, owner, jobDesc, startDate, endDate, unit, qty, rem, fab, inst, mhCal, mhJob, remarks) {
                    line.location = loc
                    line.locationOther = locOther
                    line.owner = owner
                    line.job = jobDesc
                    line.start = startDate
                    line.end = endDate
                    line.unit = unit
                    line.qtyText = qty
                    line.removalText = rem
                    line.fabText = fab
                    line.installText = inst
                    line.mhCal = mhCal
                    line.mhJob = mhJob
                    line.remarks = remarks
                }

                val qNum = qty.toDoubleOrNull() ?: 0.0
                val rNum = rem.toDoubleOrNull() ?: 0.0
                val fNum = fab.toDoubleOrNull() ?: 0.0
                val iNum = inst.toDoubleOrNull() ?: 0.0
                val progressPct = if (qNum > 0.0) {
                    val raw = ((rNum / qNum) * 30.0) + ((fNum / qNum) * 30.0) + ((iNum / qNum) * 40.0)
                    min(100, kotlin.math.round(raw).toInt())
                } else 0

                Card(
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = SteelLight.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Line #${index + 1}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = NavyPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Progress: $progressPct%",
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (progressPct >= 100) TealSuccess else NavySecondary
                                )
                                Button(
                                    onClick = {
                                        if (draftLines.size > 1) {
                                            draftLines.removeAt(index)
                                        } else {
                                            draftLines[0] = ExcelDraftLine()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RedAlert.copy(alpha = 0.15f),
                                        contentColor = RedAlert
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Line", modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Delete Line", fontSize = 11.sp)
                                }
                            }
                        }

                        // Fields
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterDropdown(
                                label = "Location",
                                selected = loc,
                                options = listOf(
                                    "Engine Room" to "Engine Room",
                                    "Main Deck" to "Main Deck",
                                    "Pump Room" to "Pump Room",
                                    "Tank" to "Tank",
                                    "Other" to "Other"
                                ),
                                onSelected = { loc = it },
                                modifier = Modifier.weight(1.2f)
                            )
                            OutlinedTextField(
                                value = owner,
                                onValueChange = { owner = it },
                                label = { Text("Owner #", fontSize = 10.5.sp) },
                                placeholder = { Text("OWN-4521", fontSize = 10.5.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        if (loc == "Other") {
                            OutlinedTextField(
                                value = locOther,
                                onValueChange = { locOther = it },
                                label = { Text("Specify Location", fontSize = 10.5.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = jobDesc,
                            onValueChange = { jobDesc = it },
                            label = { Text("Job Description *", fontSize = 11.sp) },
                            placeholder = { Text("e.g. Renew 4\" Cargo line elbow", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = startDate,
                                onValueChange = { startDate = it },
                                label = { Text("Start", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = endDate,
                                onValueChange = { endDate = it },
                                label = { Text("End", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            FilterDropdown(
                                label = "Unit",
                                selected = unit,
                                options = listOf("pcs" to "pcs", "m" to "m", "set" to "set", "lot" to "lot"),
                                onSelected = { unit = it },
                                modifier = Modifier.weight(0.9f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = qty,
                                onValueChange = { qty = it },
                                label = { Text("Qty", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = rem,
                                onValueChange = { rem = it },
                                label = { Text("Removal", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = fab,
                                onValueChange = { fab = it },
                                label = { Text("Fab/OH", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = inst,
                                onValueChange = { inst = it },
                                label = { Text("Install", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = remarks,
                            onValueChange = { remarks = it },
                            label = { Text("Remarks", fontSize = 10.5.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // Bottom action toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { draftLines.add(ExcelDraftLine()) },
                    colors = ButtonDefaults.buttonColors(containerColor = NavySecondary),
                    shape = RoundedCornerShape(5.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Line", fontSize = 12.sp)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val filled = draftLines.filter { it.job.isNotBlank() }
                            if (filled.isEmpty()) {
                                formError = "Please enter at least one Job Description."
                                return@Button
                            }
                            // Validation
                            for ((i, l) in filled.withIndex()) {
                                val q = l.qtyText.toDoubleOrNull() ?: -1.0
                                val r = l.removalText.toDoubleOrNull() ?: 0.0
                                val f = l.fabText.toDoubleOrNull() ?: 0.0
                                val inst = l.installText.toDoubleOrNull() ?: 0.0
                                if (q < 0) {
                                    formError = "Line #${i + 1}: Qty must be a non-negative number."
                                    return@Button
                                }
                                if (r > q) {
                                    formError = "Line #${i + 1}: Removal cannot exceed Qty."
                                    return@Button
                                }
                                if (f > q) {
                                    formError = "Line #${i + 1}: Fab/OH cannot exceed Qty."
                                    return@Button
                                }
                                if (inst > q) {
                                    formError = "Line #${i + 1}: Install cannot exceed Qty."
                                    return@Button
                                }
                            }

                            val tasksToSave = filled.map { l ->
                                val q = l.qtyText.toDoubleOrNull() ?: 1.0
                                val r = l.removalText.toDoubleOrNull() ?: 0.0
                                val f = l.fabText.toDoubleOrNull() ?: 0.0
                                val inst = l.installText.toDoubleOrNull() ?: 0.0
                                val rawProg = if (q > 0.0) {
                                    val raw = ((r / q) * 30.0) + ((f / q) * 30.0) + ((inst / q) * 40.0)
                                    min(100, kotlin.math.round(raw).toInt())
                                } else 0

                                Task(
                                    id = "DWP-${System.currentTimeMillis().toString().takeLast(6)}-${UUID.randomUUID().toString().take(4).uppercase()}",
                                    projectId = projectId,
                                    location = l.location,
                                    locationOther = l.locationOther.trim(),
                                    owner = l.owner.trim(),
                                    job = l.job.trim(),
                                    start = l.start.trim(),
                                    end = l.end.trim(),
                                    unit = l.unit,
                                    qty = q,
                                    removal = r,
                                    fab = f,
                                    install = inst,
                                    totalProgress = rawProg,
                                    status = if (rawProg >= 100) "done" else "progress",
                                    mhCal = l.mhCal.trim(),
                                    mhJob = l.mhJob.trim(),
                                    remarks = l.remarks.trim(),
                                    createdAt = System.currentTimeMillis(),
                                    createdBy = currentUser.displayName,
                                    createdByRole = currentUser.role.label
                                )
                            }
                            onSaveAll(tasksToSave)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealSuccess),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text("Save All Jobs (${draftLines.filter { it.job.isNotBlank() }.size})")
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectDropdownSelector(
    projects: List<Project>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val current = projects.find { it.id == selectedId } ?: projects.firstOrNull()

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = current?.name ?: "Select Project",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NavyPrimary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            projects.forEach { proj ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(proj.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "In-charge: ${proj.incharge.ifBlank { "—" }} · ETD: ${proj.etdExp}",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    },
                    onClick = {
                        onSelect(proj.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
