package com.example.dwp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dwp.data.DwpRepository
import com.example.dwp.model.UserRole
import com.example.dwp.theme.*
import com.example.dwp.ui.components.LiveStatusBadge

enum class NavTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    INDEX("Index", Icons.Default.List),
    SUMMARY("Summary", Icons.Default.Assessment),
    PROJECT_SHEET("Project Sheet", Icons.Default.Assignment)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DwpApp(repository: DwpRepository) {
    var currentTab by remember { mutableStateOf(NavTab.DASHBOARD) }
    var selectedProjectId by remember { mutableStateOf<String?>(null) }

    val projects by repository.projects.collectAsStateWithLifecycle()
    val tasksMap by repository.tasks.collectAsStateWithLifecycle()
    val currentUser by repository.currentUser.collectAsStateWithLifecycle()
    val isLiveConnected by repository.isLiveConnected.collectAsStateWithLifecycle()
    val inchargeRoster by repository.inchargeRoster.collectAsStateWithLifecycle()
    val foremanRoster by repository.foremanRoster.collectAsStateWithLifecycle()

    var showConnectionDialog by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Daily Work Progress",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            LiveStatusBadge(
                                isLive = isLiveConnected,
                                modifier = Modifier.clickable { showConnectionDialog = true }
                            )
                        }
                        Text(
                            text = "Pipe & Outfitting · ${currentUser.displayName}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showPasswordDialog = true },
                        modifier = Modifier.testTag("open_password_management_button")
                    ) {
                        Icon(
                            imageVector = if (currentUser.role == UserRole.MANAGEMENT) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = "Login & Password Management",
                            tint = if (currentUser.role == UserRole.MANAGEMENT) TealSuccess else NavySecondary
                        )
                    }
                    IconButton(
                        onClick = { showRoleDialog = true },
                        modifier = Modifier.testTag("switch_role_button")
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Switch user role", tint = NavySecondary)
                    }
                    IconButton(
                        onClick = { showConnectionDialog = true },
                        modifier = Modifier.testTag("open_connection_button")
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = "SharePoint settings", tint = NavySecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceCard,
                    titleContentColor = NavyPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceCard,
                tonalElevation = 4.dp
            ) {
                NavTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 10.5.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NavyPrimary,
                            selectedTextColor = NavyPrimary,
                            indicatorColor = SteelLight,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        },
        containerColor = HullBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavTab.DASHBOARD -> DashboardScreen(
                    projects = projects,
                    tasksMap = tasksMap,
                    onOpenProject = { projId ->
                        selectedProjectId = projId
                        currentTab = NavTab.PROJECT_SHEET
                    }
                )
                NavTab.INDEX -> IndexScreen(
                    projects = projects,
                    tasksMap = tasksMap,
                    currentUser = currentUser,
                    inchargeRoster = inchargeRoster,
                    foremanRoster = foremanRoster,
                    onOpenProject = { projId ->
                        selectedProjectId = projId
                        currentTab = NavTab.PROJECT_SHEET
                    },
                    onSaveProject = { repository.addOrUpdateProject(it) },
                    onDeleteProject = { repository.deleteProject(it) }
                )
                NavTab.SUMMARY -> SummaryScreen(
                    projects = projects,
                    tasksMap = tasksMap,
                    currentUser = currentUser,
                    onOpenProject = { projId ->
                        selectedProjectId = projId
                        currentTab = NavTab.PROJECT_SHEET
                    }
                )
                NavTab.PROJECT_SHEET -> ProjectSheetScreen(
                    projects = projects,
                    tasksMap = tasksMap,
                    selectedProjectId = selectedProjectId,
                    currentUser = currentUser,
                    onSelectProject = { selectedProjectId = it },
                    onSaveTask = { repository.addOrUpdateTask(it) },
                    onCancelTask = { pId, tId -> repository.cancelTask(pId, tId) },
                    onReinstateTask = { pId, tId -> repository.reinstateTask(pId, tId) }
                )
            }
        }

        if (showConnectionDialog) {
            ConnectionDialog(
                repository = repository,
                onDismiss = { showConnectionDialog = false }
            )
        }

        if (showRoleDialog) {
            RoleDialog(
                currentUser = currentUser,
                inchargeRoster = inchargeRoster,
                onDismiss = { showRoleDialog = false },
                onSelectRole = { role, engineer ->
                    repository.setCurrentUser(role, engineer)
                },
                verifyPassword = { repository.verifyManagementPassword(it) },
                onOpenPasswordManagement = { showPasswordDialog = true }
            )
        }

        if (showPasswordDialog) {
            PasswordManagementDialog(
                currentUser = currentUser,
                repository = repository,
                inchargeRoster = inchargeRoster,
                onDismiss = { showPasswordDialog = false }
            )
        }
    }
}
