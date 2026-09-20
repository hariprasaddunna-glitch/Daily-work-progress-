package com.example.dwp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dwp.data.DwpRepository
import com.example.dwp.model.CurrentUser
import com.example.dwp.model.UserRole
import com.example.dwp.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordManagementDialog(
    currentUser: CurrentUser,
    repository: DwpRepository,
    inchargeRoster: List<String>,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    // Login state
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var loginSuccess by remember { mutableStateOf<String?>(null) }

    // Change password state
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var changeError by remember { mutableStateOf<String?>(null) }
    var changeSuccess by remember { mutableStateOf<String?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    // Engineer selection if logging out or operating as engineer
    var selectedEngineer by remember {
        mutableStateOf(currentUser.engineerName.ifBlank { inchargeRoster.firstOrNull() ?: "Hari Prasad" })
    }

    val isPasswordDefault by repository.isPasswordDefault.collectAsStateWithLifecycle()
    val passwordLastUpdated by repository.passwordLastUpdated.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = NavyPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Login & Password Management",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Text(
                        text = "Shipyard Access & Administrative Security",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Navigation tabs inside dialog
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SurfaceCard,
                    contentColor = NavyPrimary,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            loginError = null
                            loginSuccess = null
                        },
                        text = { Text("Login / Access", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("tab_login")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            changeError = null
                            changeSuccess = null
                        },
                        text = { Text("Change Password", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("tab_change_pwd")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Security Info", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("tab_security_info")
                    )
                }

                when (selectedTab) {
                    0 -> {
                        // TAB 0: LOGIN & ACCESS MANAGEMENT
                        // Current session banner
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentUser.role == UserRole.MANAGEMENT) Color(0xFFE8F5E9) else Color(0xFFE3F2FD)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (currentUser.role == UserRole.MANAGEMENT) Icons.Default.CheckCircle else Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = if (currentUser.role == UserRole.MANAGEMENT) TealSuccess else NavySecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (currentUser.role == UserRole.MANAGEMENT) "Management Active (Full Access)" else "Engineer Active (${currentUser.displayName})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextDark
                                    )
                                    Text(
                                        text = if (currentUser.role == UserRole.MANAGEMENT)
                                            "All administrative privileges unlocked."
                                        else
                                            "Scoped to assigned projects and jobs only.",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }

                        if (currentUser.role == UserRole.MANAGEMENT) {
                            // User is currently logged in as management
                            Text(
                                text = "You are currently authenticated as Shipyard Management. You can lock your session or switch to an Engineer view below to test restricted job progress updates.",
                                fontSize = 12.sp,
                                color = TextDark
                            )

                            // Engineer roster to switch to
                            Text(
                                text = "Select In-charge Engineer to switch to:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NavyPrimary
                            )

                            inchargeRoster.forEach { engineerName ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedEngineer = engineerName }
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedEngineer == engineerName,
                                        onClick = { selectedEngineer = engineerName }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(engineerName, fontSize = 12.5.sp, color = TextDark)
                                }
                            }

                            Button(
                                onClick = {
                                    repository.setCurrentUser(UserRole.ENGINEER, selectedEngineer)
                                    loginSuccess = "Locked management session. Switched to $selectedEngineer."
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("lock_session_btn")
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lock Management & Switch to Field View", fontSize = 12.5.sp)
                            }
                        } else {
                            // User is in Engineer mode -> Show management login form
                            Text(
                                text = "Enter Management Password to unlock administrative controls (creating projects, deleting, SharePoint API setup):",
                                fontSize = 12.sp,
                                color = TextDark
                            )

                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = {
                                    loginPassword = it
                                    loginError = null
                                },
                                label = { Text("Management Password") },
                                singleLine = true,
                                visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                                        Icon(
                                            imageVector = if (loginPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password visibility"
                                        )
                                    }
                                },
                                isError = loginError != null,
                                supportingText = {
                                    if (loginError != null) {
                                        Text(loginError!!, color = RedAlert, fontSize = 11.sp)
                                    } else {
                                        Text(
                                            text = if (isPasswordDefault) "Default shipyard password is: admin" else "Enter custom management password",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_password_input")
                            )

                            Button(
                                onClick = {
                                    if (repository.verifyManagementPassword(loginPassword)) {
                                        repository.setCurrentUser(UserRole.MANAGEMENT)
                                        loginSuccess = "Authenticated successfully as Management!"
                                        loginPassword = ""
                                        loginError = null
                                    } else {
                                        loginError = "Incorrect password. Default is 'admin'."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TealSuccess),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_management_btn")
                            ) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Log In as Management", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }
                        }

                        if (loginSuccess != null) {
                            Text(
                                text = loginSuccess!!,
                                color = TealSuccess,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    1 -> {
                        // TAB 1: CHANGE PASSWORD
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPasswordDefault) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (isPasswordDefault) "Default Password Active ('admin')" else "Custom Password Active",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = if (isPasswordDefault) Color(0xFFE65100) else TealSuccess
                                )
                                Text(
                                    text = if (isPasswordDefault)
                                        "For security, we recommend replacing the default 'admin' password with a strong password."
                                    else
                                        passwordLastUpdated,
                                    fontSize = 11.sp,
                                    color = TextDark
                                )
                            }
                        }

                        OutlinedTextField(
                            value = currentPassword,
                            onValueChange = {
                                currentPassword = it
                                changeError = null
                                changeSuccess = null
                            },
                            label = { Text("Current Password") },
                            singleLine = true,
                            visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                                    Icon(
                                        imageVector = if (currentPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("current_password_input")
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                changeError = null
                                changeSuccess = null
                            },
                            label = { Text("New Password (min 4 chars)") },
                            singleLine = true,
                            visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                    Icon(
                                        imageVector = if (newPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_password_input")
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                changeError = null
                                changeSuccess = null
                            },
                            label = { Text("Confirm New Password") },
                            singleLine = true,
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirm_password_input")
                        )

                        if (changeError != null) {
                            Text(
                                text = changeError!!,
                                color = RedAlert,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (changeSuccess != null) {
                            Text(
                                text = changeSuccess!!,
                                color = TealSuccess,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                if (newPassword.trim() != confirmPassword.trim()) {
                                    changeError = "New passwords do not match."
                                    return@Button
                                }
                                val result = repository.updateManagementPassword(currentPassword, newPassword)
                                if (result.isSuccess) {
                                    changeSuccess = "Password updated successfully!"
                                    changeError = null
                                    currentPassword = ""
                                    newPassword = ""
                                    confirmPassword = ""
                                } else {
                                    changeError = result.exceptionOrNull()?.message ?: "Failed to update password."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavySecondary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("update_password_btn")
                        ) {
                            Text("Update Password")
                        }

                        Divider(color = SteelLight)

                        OutlinedButton(
                            onClick = { showResetConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_password_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Password to Default ('admin')", fontSize = 11.5.sp)
                        }
                    }

                    2 -> {
                        // TAB 2: SECURITY & PERMISSIONS INFO
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Why Management is Password Protected:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                            Text(
                                text = "Daily Work Progress is utilized on the drydock floor across vessel berths. To prevent accidental edits or unauthorized changes, Management privileges are secured with a password.",
                                fontSize = 11.5.sp,
                                color = TextDark
                            )

                            Card(
                                colors = CardDefaults.cardColors(containerColor = HullBackground),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Protected Capabilities:", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = NavyPrimary)
                                    Text("• Creating new vessel projects and drydock berths", fontSize = 11.sp, color = TextDark)
                                    Text("• Deleting existing projects and archived records", fontSize = 11.sp, color = TextDark)
                                    Text("• Posting Yard Manager priority broadcast directives", fontSize = 11.sp, color = TextDark)
                                    Text("• Configuring SharePoint Power Automate sync webhooks", fontSize = 11.sp, color = TextDark)
                                    Text("• Editing tasks outside an engineer's assigned vessel", fontSize = 11.sp, color = TextDark)
                                }
                            }

                            Text(
                                text = "Engineers can freely view all project dashboards, inspect job sheets, and update progress percentages on their assigned vessels without needing the Management password.",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NavySecondary)
            ) {
                Text("Close")
            }
        }
    )

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset to Default Password?") },
            text = { Text("This will reset the Management password back to 'admin'. Are you sure?", fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        repository.resetManagementPassword()
                        showResetConfirm = false
                        changeSuccess = "Password reset to default 'admin'."
                        changeError = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert)
                ) {
                    Text("Reset to 'admin'")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
