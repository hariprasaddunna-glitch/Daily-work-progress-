package com.example.dwp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwp.model.CurrentUser
import com.example.dwp.model.UserRole
import com.example.dwp.theme.*

@Composable
fun RoleDialog(
    currentUser: CurrentUser,
    inchargeRoster: List<String>,
    onDismiss: () -> Unit,
    onSelectRole: (UserRole, String) -> Unit,
    verifyPassword: (String) -> Boolean,
    onOpenPasswordManagement: () -> Unit
) {
    var selectedRole by remember { mutableStateOf(currentUser.role) }
    var selectedEngineer by remember {
        mutableStateOf(currentUser.engineerName.ifBlank { inchargeRoster.firstOrNull() ?: "Hari Prasad" })
    }
    var managementPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Switch User Role", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select operational role. Management role is password-protected to safeguard project administration and sync configurations.",
                    fontSize = 11.5.sp,
                    color = TextMuted
                )

                // Management Radio Option
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedRole == UserRole.MANAGEMENT) SteelLight else HullBackground
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedRole = UserRole.MANAGEMENT
                            passwordError = null
                        }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedRole == UserRole.MANAGEMENT,
                                onClick = {
                                    selectedRole = UserRole.MANAGEMENT
                                    passwordError = null
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Management (Administrator)", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                                Text("Full administrative access to all projects, jobs & settings", fontSize = 10.5.sp, color = TextMuted)
                            }
                        }

                        // Password field if switching to Management from Engineer
                        if (selectedRole == UserRole.MANAGEMENT && currentUser.role != UserRole.MANAGEMENT) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = managementPassword,
                                onValueChange = {
                                    managementPassword = it
                                    passwordError = null
                                },
                                label = { Text("Management Password") },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle visibility"
                                        )
                                    }
                                },
                                isError = passwordError != null,
                                supportingText = {
                                    if (passwordError != null) {
                                        Text(passwordError!!, color = RedAlert, fontSize = 11.sp)
                                    } else {
                                        Text("Default initial password is: admin", color = TextMuted, fontSize = 10.5.sp)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("role_management_password_input")
                            )
                        } else if (selectedRole == UserRole.MANAGEMENT && currentUser.role == UserRole.MANAGEMENT) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Currently authenticated as Management",
                                fontSize = 11.sp,
                                color = TealSuccess,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 36.dp)
                            )
                        }
                    }
                }

                // Engineer Radio Option
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedRole == UserRole.ENGINEER) SteelLight else HullBackground
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedRole = UserRole.ENGINEER
                            passwordError = null
                        }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedRole == UserRole.ENGINEER,
                                onClick = {
                                    selectedRole = UserRole.ENGINEER
                                    passwordError = null
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Engineer", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                                Text("Assigned vessel project updates only", fontSize = 10.5.sp, color = TextMuted)
                            }
                        }

                        if (selectedRole == UserRole.ENGINEER) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Select In-charge Engineer:", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
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
                                    Text(engineerName, fontSize = 12.sp, color = TextDark)
                                }
                            }
                        }
                    }
                }

                // Direct shortcut to full password management
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenPasswordManagement()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_password_mgmt_from_role_btn")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(15.dp), tint = NavySecondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Password Management & Settings", fontSize = 11.5.sp, color = NavySecondary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedRole == UserRole.MANAGEMENT && currentUser.role != UserRole.MANAGEMENT) {
                        if (!verifyPassword(managementPassword)) {
                            passwordError = "Incorrect password. Default is 'admin'."
                            return@Button
                        }
                    }
                    onSelectRole(selectedRole, if (selectedRole == UserRole.ENGINEER) selectedEngineer else "")
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavySecondary),
                modifier = Modifier.testTag("confirm_role_switch_btn")
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
