package com.example.dwp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwp.data.DwpRepository
import com.example.dwp.theme.*
import kotlinx.coroutines.launch

@Composable
fun ConnectionDialog(
    repository: DwpRepository,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var projectsUrl by remember { mutableStateOf(repository.projectsApiUrl) }
    var jobsUrl by remember { mutableStateOf(repository.jobsApiUrl) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("SharePoint & Power Automate", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Configure Microsoft Power Automate flow URLs for SharePoint project & jobs synchronization.",
                    fontSize = 12.sp,
                    color = TextMuted
                )

                OutlinedTextField(
                    value = projectsUrl,
                    onValueChange = { projectsUrl = it },
                    label = { Text("Projects API URL", fontSize = 11.5.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    maxLines = 3
                )

                OutlinedTextField(
                    value = jobsUrl,
                    onValueChange = { jobsUrl = it },
                    label = { Text("Jobs API URL", fontSize = 11.5.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    maxLines = 3
                )

                Button(
                    onClick = {
                        isTesting = true
                        testResult = "Pinging Power Automate endpoint..."
                        coroutineScope.launch {
                            val res = repository.testConnection()
                            testResult = res
                            isTesting = false
                        }
                    },
                    enabled = !isTesting,
                    colors = ButtonDefaults.buttonColors(containerColor = NavySecondary),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_connection_button")
                ) {
                    Text(if (isTesting) "Testing Connection..." else "Test SharePoint Connection", fontSize = 12.sp)
                }

                if (testResult != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SteelLight, RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = testResult!!,
                            fontSize = 11.sp,
                            color = TextDark,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    repository.projectsApiUrl = projectsUrl.trim()
                    repository.jobsApiUrl = jobsUrl.trim()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavySecondary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
