package com.example.dwp.data

import android.content.Context
import com.example.dwp.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

class DwpRepository(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val prefs = context.getSharedPreferences("dwp_storage", Context.MODE_PRIVATE)
    private val projectsFile = File(context.filesDir, "dwp_projects.json")
    private val tasksFile = File(context.filesDir, "dwp_tasks.json")

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _tasks = MutableStateFlow<Map<String, List<Task>>>(emptyMap())
    val tasks: StateFlow<Map<String, List<Task>>> = _tasks.asStateFlow()

    private val _inchargeRoster = MutableStateFlow<List<String>>(emptyList())
    val inchargeRoster: StateFlow<List<String>> = _inchargeRoster.asStateFlow()

    private val _foremanRoster = MutableStateFlow<List<String>>(emptyList())
    val foremanRoster: StateFlow<List<String>> = _foremanRoster.asStateFlow()

    private val _currentUser = MutableStateFlow(CurrentUser(UserRole.MANAGEMENT))
    val currentUser: StateFlow<CurrentUser> = _currentUser.asStateFlow()

    private val _isPasswordDefault = MutableStateFlow(true)
    val isPasswordDefault: StateFlow<Boolean> = _isPasswordDefault.asStateFlow()

    private val _passwordLastUpdated = MutableStateFlow("Default setup (admin)")
    val passwordLastUpdated: StateFlow<String> = _passwordLastUpdated.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Offline — local data")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _isLiveConnected = MutableStateFlow(false)
    val isLiveConnected: StateFlow<Boolean> = _isLiveConnected.asStateFlow()

    var projectsApiUrl: String
        get() = prefs.getString("projects_api_url", DEFAULT_PROJECTS_API_URL) ?: DEFAULT_PROJECTS_API_URL
        set(value) = prefs.edit().putString("projects_api_url", value).apply()

    var jobsApiUrl: String
        get() = prefs.getString("jobs_api_url", DEFAULT_JOBS_API_URL) ?: DEFAULT_JOBS_API_URL
        set(value) = prefs.edit().putString("jobs_api_url", value).apply()

    var createProjectApiUrl: String
        get() = prefs.getString("create_project_api_url", DEFAULT_CREATE_PROJECT_API_URL) ?: DEFAULT_CREATE_PROJECT_API_URL
        set(value) = prefs.edit().putString("create_project_api_url", value).apply()

    init {
        loadFromStorage()
    }

    private fun loadFromStorage() {
        // Load user role & password status
        val savedRoleName = prefs.getString(KEY_CURRENT_ROLE, UserRole.MANAGEMENT.name) ?: UserRole.MANAGEMENT.name
        val savedRole = try { UserRole.valueOf(savedRoleName) } catch (_: Exception) { UserRole.MANAGEMENT }
        val savedEngineer = prefs.getString(KEY_CURRENT_ENGINEER, "") ?: ""
        _currentUser.value = CurrentUser(savedRole, savedEngineer)

        val storedPass = prefs.getString(KEY_MANAGEMENT_PASSWORD, DEFAULT_MANAGEMENT_PASSWORD) ?: DEFAULT_MANAGEMENT_PASSWORD
        _isPasswordDefault.value = (storedPass == DEFAULT_MANAGEMENT_PASSWORD)
        _passwordLastUpdated.value = prefs.getString(KEY_PASSWORD_LAST_CHANGED, if (storedPass == DEFAULT_MANAGEMENT_PASSWORD) "Default setup (admin)" else "Custom password") ?: "Default"

        if (!projectsFile.exists()) {
            seedInitialData()
        } else {
            try {
                val projJson = projectsFile.readText()
                val taskJson = if (tasksFile.exists()) tasksFile.readText() else "{}"
                val loadedProjects = parseProjectsJson(projJson)
                val loadedTasks = parseTasksJson(taskJson)
                _projects.value = loadedProjects.sortedBy { it.seq }
                _tasks.value = loadedTasks
                updateRosters()
            } catch (e: Exception) {
                seedInitialData()
            }
        }
    }

    private fun seedInitialData() {
        val today = LocalDate.now()
        val defaultProjects = listOf(
            Project(
                id = "PRJ-001",
                seq = 1,
                name = "PRJ-001 LNG Carrier Cargo Line",
                incharge = "Hari Prasad",
                foreman = "Mubarak",
                estMH = 1250.0,
                atd = today.minusDays(10).toString(),
                etdOrig = today.plusDays(15).toString(),
                etdExp = today.plusDays(12).toString(),
                managerMessage = "Priority: Complete cryo valve replacement in Cargo Compressor Room before pressure test."
            ),
            Project(
                id = "PRJ-002",
                seq = 2,
                name = "PRJ-002 Ferry Drydock Piping",
                incharge = "Salim Al-Harthy",
                foreman = "Ali",
                estMH = 820.0,
                atd = today.minusDays(5).toString(),
                etdOrig = today.plusDays(6).toString(),
                etdExp = today.plusDays(4).toString(),
                managerMessage = "Inspect seawater cooling lines in Main Engine Room for pitting and wall thinning."
            ),
            Project(
                id = "PRJ-003",
                seq = 3,
                name = "PRJ-003 Bulk Carrier Tank Heating Coils",
                incharge = "Khalfan Al-Farsi",
                foreman = "Suresh",
                estMH = 1600.0,
                atd = today.minusDays(2).toString(),
                etdOrig = today.plusDays(20).toString(),
                etdExp = today.plusDays(18).toString(),
                managerMessage = "Hydrostatic test required for Fuel Oil Settling Tank coils prior to tank closing."
            ),
            Project(
                id = "PRJ-004",
                seq = 4,
                name = "PRJ-004 Chemical Tanker Pump Room",
                incharge = "Rajesh Kumar",
                foreman = "Babu",
                estMH = 950.0,
                atd = today.plusDays(3).toString(),
                etdOrig = today.plusDays(25).toString(),
                etdExp = today.plusDays(24).toString(),
                managerMessage = "Vessel scheduled to dock in 3 days. Prepare spool drawings and pipe pre-fabs."
            )
        )

        val defaultTasks = mapOf(
            "PRJ-001" to listOf(
                Task(
                    id = "DWP-2026-001",
                    projectId = "PRJ-001",
                    location = "Engine Room",
                    owner = "OWN-4101",
                    job = "Renew 6\" Carbon Steel Seawater Cooling Overboard Spool",
                    start = today.minusDays(8).toString(),
                    end = today.plusDays(2).toString(),
                    unit = "pcs",
                    qty = 4.0,
                    removal = 4.0,
                    fab = 4.0,
                    install = 3.0,
                    totalProgress = computeTaskProgress(4.0, 4.0, 4.0, 3.0),
                    status = "progress",
                    mhCal = "36",
                    mhJob = "32",
                    remarks = "Final bolt torquing and gasket fit-up in progress"
                ),
                Task(
                    id = "DWP-2026-002",
                    projectId = "PRJ-001",
                    location = "Pump Room",
                    owner = "OWN-4105",
                    job = "Cargo Stripping Pump Suction Strainer Overhaul",
                    start = today.minusDays(5).toString(),
                    end = today.plusDays(1).toString(),
                    unit = "set",
                    qty = 2.0,
                    removal = 2.0,
                    fab = 2.0,
                    install = 2.0,
                    totalProgress = 100,
                    status = "done",
                    mhCal = "24",
                    mhJob = "20",
                    remarks = "Passed hydro test 10 bar. Signed off by surveyor."
                ),
                Task(
                    id = "DWP-2026-003",
                    projectId = "PRJ-001",
                    location = "Main Deck",
                    owner = "OWN-4112",
                    job = "Hydraulic Deck Winch Return Pipe Line 1.5\" Stainless Steel",
                    start = today.minusDays(3).toString(),
                    end = today.plusDays(6).toString(),
                    unit = "m",
                    qty = 30.0,
                    removal = 30.0,
                    fab = 15.0,
                    install = 0.0,
                    totalProgress = computeTaskProgress(30.0, 30.0, 15.0, 0.0),
                    status = "progress",
                    mhCal = "60",
                    mhJob = "55",
                    remarks = "TIG welding at workshop"
                )
            ),
            "PRJ-002" to listOf(
                Task(
                    id = "DWP-2026-004",
                    projectId = "PRJ-002",
                    location = "Engine Room",
                    owner = "OWN-3201",
                    job = "Auxiliary Engine Exhaust Bellow & Flange Renewal",
                    start = today.minusDays(4).toString(),
                    end = today.plusDays(2).toString(),
                    unit = "pcs",
                    qty = 2.0,
                    removal = 2.0,
                    fab = 1.0,
                    install = 1.0,
                    totalProgress = computeTaskProgress(2.0, 2.0, 1.0, 1.0),
                    status = "progress",
                    mhCal = "18",
                    mhJob = "16",
                    remarks = "Awaiting insulation jacket delivery"
                )
            ),
            "PRJ-003" to listOf(
                Task(
                    id = "DWP-2026-005",
                    projectId = "PRJ-003",
                    location = "Tank",
                    owner = "OWN-5501",
                    job = "Heavy Fuel Oil Settling Tank Heating Grid Renewal 2\" Seamless",
                    start = today.minusDays(1).toString(),
                    end = today.plusDays(10).toString(),
                    unit = "m",
                    qty = 120.0,
                    removal = 40.0,
                    fab = 30.0,
                    install = 0.0,
                    totalProgress = computeTaskProgress(120.0, 40.0, 30.0, 0.0),
                    status = "progress",
                    mhCal = "180",
                    mhJob = "160",
                    remarks = "Gas-free certificate issued, hot work cleared"
                )
            )
        )

        _projects.value = defaultProjects
        _tasks.value = defaultTasks
        saveToStorage()
        updateRosters()
    }

    private fun updateRosters() {
        val incharges = mutableSetOf("Hari Prasad", "Salim Al-Harthy", "Khalfan Al-Farsi", "Rajesh Kumar")
        val foremen = mutableSetOf("Mubarak", "Ali", "Suresh", "Babu")
        _projects.value.forEach {
            if (it.incharge.isNotBlank()) incharges.add(it.incharge.trim())
            if (it.foreman.isNotBlank()) foremen.add(it.foreman.trim())
        }
        _inchargeRoster.value = incharges.filter { it.isNotBlank() }.sorted()
        _foremanRoster.value = foremen.filter { it.isNotBlank() }.sorted()
    }

    private fun saveToStorage() {
        scope.launch {
            try {
                val projJson = serializeProjects(_projects.value)
                projectsFile.writeText(projJson)

                val taskJson = serializeTasks(_tasks.value)
                tasksFile.writeText(taskJson)
            } catch (_: Exception) {}
        }
    }

    fun setCurrentUser(role: UserRole, engineerName: String = "") {
        _currentUser.value = CurrentUser(role, engineerName)
        prefs.edit()
            .putString(KEY_CURRENT_ROLE, role.name)
            .putString(KEY_CURRENT_ENGINEER, engineerName)
            .apply()
    }

    fun verifyManagementPassword(input: String): Boolean {
        val stored = prefs.getString(KEY_MANAGEMENT_PASSWORD, DEFAULT_MANAGEMENT_PASSWORD) ?: DEFAULT_MANAGEMENT_PASSWORD
        val trimmed = input.trim()
        if (stored == DEFAULT_MANAGEMENT_PASSWORD) {
            return trimmed == "admin" || trimmed == "admin123"
        }
        return trimmed == stored
    }

    fun updateManagementPassword(currentPassword: String, newPassword: String): Result<String> {
        if (!verifyManagementPassword(currentPassword)) {
            return Result.failure(IllegalArgumentException("Current password is incorrect."))
        }
        val trimmed = newPassword.trim()
        if (trimmed.length < 4) {
            return Result.failure(IllegalArgumentException("New password must be at least 4 characters."))
        }
        val dateStr = LocalDate.now().toString()
        prefs.edit()
            .putString(KEY_MANAGEMENT_PASSWORD, trimmed)
            .putString(KEY_PASSWORD_LAST_CHANGED, "Set on $dateStr")
            .apply()
        _isPasswordDefault.value = (trimmed == DEFAULT_MANAGEMENT_PASSWORD)
        _passwordLastUpdated.value = "Updated on $dateStr"
        return Result.success("Management password updated successfully.")
    }

    fun resetManagementPassword() {
        prefs.edit()
            .putString(KEY_MANAGEMENT_PASSWORD, DEFAULT_MANAGEMENT_PASSWORD)
            .putString(KEY_PASSWORD_LAST_CHANGED, "Reset to default (admin)")
            .apply()
        _isPasswordDefault.value = true
        _passwordLastUpdated.value = "Reset to default (admin)"
    }

    fun addOrUpdateProject(project: Project) {
        val current = _projects.value.toMutableList()
        val index = current.indexOfFirst { it.id == project.id }
        if (index >= 0) {
            current[index] = project
        } else {
            current.add(project)
        }
        _projects.value = current.sortedBy { it.seq }
        updateRosters()
        saveToStorage()

        // Sync new/updated project to SharePoint in background via Power Automate
        if (index < 0) {
            scope.launch {
                syncProjectToSharePoint(project)
            }
        }
    }

    fun deleteProject(projectId: String) {
        val current = _projects.value.toMutableList()
        current.removeAll { it.id == projectId }
        _projects.value = current

        val taskMap = _tasks.value.toMutableMap()
        taskMap.remove(projectId)
        _tasks.value = taskMap

        saveToStorage()
    }

    fun addOrUpdateTask(task: Task) {
        val taskMap = _tasks.value.toMutableMap()
        val projectTasks = taskMap[task.projectId]?.toMutableList() ?: mutableListOf()
        val index = projectTasks.indexOfFirst { it.id == task.id }
        if (index >= 0) {
            projectTasks[index] = task
        } else {
            projectTasks.add(task)
        }
        taskMap[task.projectId] = projectTasks
        _tasks.value = taskMap
        saveToStorage()

        // Sync to SharePoint in background with null-safe payload
        scope.launch {
            syncTaskToSharePoint(task, action = if (index >= 0) "update" else "create")
        }
    }

    fun cancelTask(projectId: String, taskId: String) {
        val taskMap = _tasks.value.toMutableMap()
        val projectTasks = taskMap[projectId]?.toMutableList() ?: return
        val index = projectTasks.indexOfFirst { it.id == taskId }
        if (index >= 0) {
            val old = projectTasks[index]
            val updated = old.copy(cancelled = true, status = "cancelled")
            projectTasks[index] = updated
            taskMap[projectId] = projectTasks
            _tasks.value = taskMap
            saveToStorage()

            // Sync cancellation to SharePoint with null-safe payload
            scope.launch {
                syncTaskToSharePoint(updated, action = "cancel")
            }
        }
    }

    fun reinstateTask(projectId: String, taskId: String) {
        val taskMap = _tasks.value.toMutableMap()
        val projectTasks = taskMap[projectId]?.toMutableList() ?: return
        val index = projectTasks.indexOfFirst { it.id == taskId }
        if (index >= 0) {
            val old = projectTasks[index]
            val updated = old.copy(cancelled = false, status = "progress")
            projectTasks[index] = updated
            taskMap[projectId] = projectTasks
            _tasks.value = taskMap
            saveToStorage()

            scope.launch {
                syncTaskToSharePoint(updated, action = "reinstate")
            }
        }
    }

    suspend fun syncTaskToSharePoint(task: Task, action: String = "update"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL(jobsApiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            // Crucial: Guarantee NO null values are sent. Power Automate JSON Schema
            // rejects null for type: 'string' and throws HTTP 400 TriggerInputSchemaMismatch.
            val payload = JSONObject().apply {
                put("action", action)
                put("id", task.id)
                put("projectId", task.projectId)
                put("sharePointId", task.sharePointId)
                put("createdAt", task.createdAt.toString())
                put("createdBy", task.createdBy)
                put("createdByRole", task.createdByRole)
                put("location", task.location)
                put("locationOther", task.locationOther)
                put("owner", task.owner)
                put("job", task.job)
                put("start", task.start)
                put("end", task.end)
                put("unit", task.unit)
                put("qty", task.qty)
                put("removal", task.removal)
                put("fab", task.fab)
                put("install", task.install)
                put("totalProgress", task.totalProgress)
                put("status", task.status)
                put("cancelled", if (task.cancelled) "true" else "false")
                put("mhCal", task.mhCal)
                put("mhJob", task.mhJob)
                put("remarks", task.remarks)
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray())
            }

            val code = conn.responseCode
            val responseBody = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
            }

            if (code in 200..299) {
                _isLiveConnected.value = true
                _connectionStatus.value = "Live — connected to SharePoint"
                Result.success("Synced to SharePoint (HTTP $code)")
            } else {
                Result.failure(Exception("HTTP $code: $responseBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncProjectToSharePoint(project: Project): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL(createProjectApiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("ProjectName", project.name)
                put("ProjectID", project.id)
                put("DisplayOrder", project.seq)
                put("InCharge", project.incharge)
                put("Foreman", project.foreman)
                put("EstimatedMH", project.estMH)
                put("ATD", project.atd)
                put("OriginalETD", project.etdOrig)
                put("ExpectedETD", project.etdExp)
                put("ManagerMessage", project.managerMessage)
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray())
            }

            val code = conn.responseCode
            val responseBody = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
            }

            if (code in 200..299) {
                _isLiveConnected.value = true
                _connectionStatus.value = "Live — connected to SharePoint"
                Result.success("Project synced to SharePoint (HTTP $code)")
            } else {
                Result.failure(Exception("HTTP $code: $responseBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    enum class TestEndpoint { PROJECTS, JOBS, CREATE_PROJECT }

    suspend fun testConnection(endpoint: TestEndpoint = TestEndpoint.PROJECTS): String = withContext(Dispatchers.IO) {
        try {
            val targetUrl = when (endpoint) {
                TestEndpoint.PROJECTS -> projectsApiUrl
                TestEndpoint.JOBS -> jobsApiUrl
                TestEndpoint.CREATE_PROJECT -> createProjectApiUrl
            }
            val url = URL(targetUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val probeBody = when (endpoint) {
                TestEndpoint.JOBS -> {
                    JSONObject().apply {
                        put("action", "test")
                        put("id", "TEST-000")
                        put("projectId", "TEST")
                        put("sharePointId", "")
                        put("createdAt", System.currentTimeMillis().toString())
                        put("createdBy", "Test User")
                        put("createdByRole", "Management")
                        put("location", "Engine Room")
                        put("locationOther", "")
                        put("owner", "")
                        put("job", "Connection Test Job")
                        put("start", LocalDate.now().toString())
                        put("end", LocalDate.now().toString())
                        put("unit", "pcs")
                        put("qty", 1.0)
                        put("removal", 0.0)
                        put("fab", 0.0)
                        put("install", 0.0)
                        put("totalProgress", 0)
                        put("status", "progress")
                        put("cancelled", "false")
                        put("mhCal", "")
                        put("mhJob", "")
                        put("remarks", "Connection test probe")
                    }.toString()
                }
                TestEndpoint.CREATE_PROJECT -> {
                    JSONObject().apply {
                        put("ProjectName", "Connection Test Project")
                        put("ProjectID", "TEST-PROJ")
                        put("DisplayOrder", 999)
                        put("InCharge", "Test Lead")
                        put("Foreman", "Test Foreman")
                        put("EstimatedMH", 100)
                        put("ATD", LocalDate.now().toString())
                        put("OriginalETD", LocalDate.now().plusDays(10).toString())
                        put("ExpectedETD", LocalDate.now().plusDays(10).toString())
                        put("ManagerMessage", "Probe connection test")
                    }.toString()
                }
                TestEndpoint.PROJECTS -> "{}"
            }

            conn.outputStream.use { os ->
                os.write(probeBody.toByteArray())
            }

            val code = conn.responseCode
            val endpointName = when (endpoint) {
                TestEndpoint.PROJECTS -> "Projects endpoint"
                TestEndpoint.JOBS -> "Jobs endpoint"
                TestEndpoint.CREATE_PROJECT -> "Create Project endpoint"
            }
            if (code in 200..299) {
                _isLiveConnected.value = true
                _connectionStatus.value = "Live — connected to SharePoint"
                "Success! $endpointName responded with HTTP $code."
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                "Returned HTTP $code: $err"
            }
        } catch (e: Exception) {
            _isLiveConnected.value = false
            _connectionStatus.value = "Offline — local storage active"
            "Connection test error: ${e.message ?: "Network unreachable. Local storage active."}"
        }
    }

    suspend fun testConnection(testJobs: Boolean): String =
        testConnection(if (testJobs) TestEndpoint.JOBS else TestEndpoint.PROJECTS)

    // JSON serialization helpers
    private fun serializeProjects(list: List<Project>): String {
        val arr = JSONArray()
        list.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("seq", p.seq)
                put("name", p.name)
                put("incharge", p.incharge)
                put("foreman", p.foreman)
                put("estMH", p.estMH)
                put("atd", p.atd)
                put("etdOrig", p.etdOrig)
                put("etdExp", p.etdExp)
                put("managerMessage", p.managerMessage)
            }
            arr.put(obj)
        }
        return arr.toString(2)
    }

    private fun parseProjectsJson(json: String): List<Project> {
        val arr = JSONArray(json)
        val list = mutableListOf<Project>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                Project(
                    id = obj.optString("id"),
                    seq = obj.optInt("seq", i + 1),
                    name = obj.optString("name"),
                    incharge = obj.optString("incharge"),
                    foreman = obj.optString("foreman"),
                    estMH = obj.optDouble("estMH", 0.0),
                    atd = obj.optString("atd"),
                    etdOrig = obj.optString("etdOrig"),
                    etdExp = obj.optString("etdExp"),
                    managerMessage = obj.optString("managerMessage")
                )
            )
        }
        return list
    }

    private fun serializeTasks(map: Map<String, List<Task>>): String {
        val root = JSONObject()
        map.forEach { (projId, tasks) ->
            val arr = JSONArray()
            tasks.forEach { t ->
                val obj = JSONObject().apply {
                    put("id", t.id)
                    put("projectId", t.projectId)
                    put("sharePointId", t.sharePointId)
                    put("createdAt", t.createdAt)
                    put("createdBy", t.createdBy)
                    put("createdByRole", t.createdByRole)
                    put("location", t.location)
                    put("locationOther", t.locationOther)
                    put("owner", t.owner)
                    put("job", t.job)
                    put("start", t.start)
                    put("end", t.end)
                    put("unit", t.unit)
                    put("qty", t.qty)
                    put("removal", t.removal)
                    put("fab", t.fab)
                    put("install", t.install)
                    put("totalProgress", t.totalProgress)
                    put("status", t.status)
                    put("cancelled", t.cancelled)
                    put("mhCal", t.mhCal)
                    put("mhJob", t.mhJob)
                    put("remarks", t.remarks)
                }
                arr.put(obj)
            }
            root.put(projId, arr)
        }
        return root.toString(2)
    }

    private fun parseTasksJson(json: String): Map<String, List<Task>> {
        val root = JSONObject(json)
        val map = mutableMapOf<String, List<Task>>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val projId = keys.next()
            val arr = root.getJSONArray(projId)
            val list = mutableListOf<Task>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Task(
                        id = obj.optString("id"),
                        projectId = obj.optString("projectId", projId),
                        sharePointId = obj.optString("sharePointId"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        createdBy = obj.optString("createdBy"),
                        createdByRole = obj.optString("createdByRole"),
                        location = obj.optString("location", "Engine Room"),
                        locationOther = obj.optString("locationOther"),
                        owner = obj.optString("owner"),
                        job = obj.optString("job"),
                        start = obj.optString("start"),
                        end = obj.optString("end"),
                        unit = obj.optString("unit", "pcs"),
                        qty = obj.optDouble("qty", 0.0),
                        removal = obj.optDouble("removal", 0.0),
                        fab = obj.optDouble("fab", 0.0),
                        install = obj.optDouble("install", 0.0),
                        totalProgress = obj.optInt("totalProgress", 0),
                        status = obj.optString("status", "progress"),
                        cancelled = obj.optBoolean("cancelled", false),
                        mhCal = obj.optString("mhCal"),
                        mhJob = obj.optString("mhJob"),
                        remarks = obj.optString("remarks")
                    )
                )
            }
            map[projId] = list
        }
        return map
    }

    companion object {
        const val DEFAULT_MANAGEMENT_PASSWORD = "admin"
        const val KEY_MANAGEMENT_PASSWORD = "management_password"
        const val KEY_PASSWORD_LAST_CHANGED = "password_last_changed"
        const val KEY_CURRENT_ROLE = "current_role"
        const val KEY_CURRENT_ENGINEER = "current_engineer"

        const val DEFAULT_PROJECTS_API_URL =
            "https://defaulte1db4b0fa7714773a87bf2bada9d03.c6.environment.api.powerplatform.com:443/powerautomate/automations/direct/cu/00/workflows/d7b2295325124cbcaf87bdcc93d85c49/triggers/manual/paths/invoke?api-version=1&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=Bwc2ZEh3fZ3mMHJskYUyAVt0NQCyjhHGSey37RH8Rq0"
        const val DEFAULT_JOBS_API_URL =
            "https://defaulte1db4b0fa7714773a87bf2bada9d03.c6.environment.api.powerplatform.com:443/powerautomate/automations/direct/cu/25/workflows/9f54ae5d368240ee8a5ee703d4c4d820/triggers/manual/paths/invoke?api-version=1&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=LnuVu6OZzq1tcZ8YC0Z1wijsJiq7jnvQLXcZN6Givqc"
        const val DEFAULT_CREATE_PROJECT_API_URL =
            "https://defaulte1db4b0fa7714773a87bf2bada9d03.c6.environment.api.powerplatform.com:443/powerautomate/automations/direct/cu/03/workflows/ae744768599a4a6f9cf2402416f5d66f/triggers/manual/paths/invoke?api-version=1&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=RqtsDb8ZDdshSsKL7XVKZcPAwYixa1PZN1-bbqgA8yI"
    }
}
