package com.example.dwp.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

enum class UserRole(val label: String) {
    MANAGEMENT("Management"),
    ENGINEER("Engineer")
}

data class CurrentUser(
    val role: UserRole = UserRole.MANAGEMENT,
    val engineerName: String = ""
) {
    val displayName: String
        get() = if (role == UserRole.MANAGEMENT) "Management" else engineerName.ifEmpty { "Engineer" }

    fun canEditProject(project: Project): Boolean {
        if (role == UserRole.MANAGEMENT) return true
        return project.incharge.trim().equals(engineerName.trim(), ignoreCase = true)
    }

    fun canManageProjects(): Boolean {
        return role == UserRole.MANAGEMENT
    }
}

data class Project(
    val id: String,
    val seq: Int = 1,
    val name: String,
    val incharge: String = "",
    val foreman: String = "",
    val estMH: Double = 0.0,
    val atd: String = "",
    val etdOrig: String = "",
    val etdExp: String = "",
    val managerMessage: String = ""
) {
    fun overallProgress(tasks: List<Task>): Int {
        val active = tasks.filter { !it.cancelled }
        if (active.isEmpty()) return 0
        return (active.sumOf { it.totalProgress }.toDouble() / active.size).roundToInt()
    }

    fun status(progress: Int): String {
        if (progress >= 100) return "done"
        val today = LocalDate.now().toString()
        if (etdExp.isNotBlank() && today >= etdExp) return "behind"
        return "progress"
    }

    fun priority(): String {
        if (etdExp.isBlank()) return "low"
        val days = daysRemaining(etdExp) ?: return "low"
        return when {
            days <= 3 -> "high"
            days <= 7 -> "medium"
            else -> "low"
        }
    }

    fun daysToArrival(): Long? = daysRemaining(atd)
    fun daysToDeparture(): Long? = daysRemaining(etdExp)

    fun daysDuration(): Long? = daysBetween(atd, etdExp)
}

data class Task(
    val id: String,
    val projectId: String,
    val sharePointId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val createdByRole: String = "",
    val location: String = "Engine Room",
    val locationOther: String = "",
    val owner: String = "",
    val job: String = "",
    val start: String = "",
    val end: String = "",
    val unit: String = "pcs",
    val qty: Double = 0.0,
    val removal: Double = 0.0,
    val fab: Double = 0.0,
    val install: Double = 0.0,
    val totalProgress: Int = 0,
    val status: String = "progress",
    val cancelled: Boolean = false,
    val mhCal: String = "",
    val mhJob: String = "",
    val remarks: String = ""
) {
    val displayLocation: String
        get() = if (location.equals("Other", ignoreCase = true) && locationOther.isNotBlank()) locationOther else location

    fun effectiveStatus(): String {
        if (cancelled) return "cancelled"
        if (totalProgress >= 100) return "done"
        val today = LocalDate.now().toString()
        if (end.isNotBlank() && today >= end) return "behind"
        return if (status.equals("behind", ignoreCase = true)) "behind" else "progress"
    }
}

data class InchargeWorkload(
    val name: String,
    val running: Int = 0,
    val completed: Int = 0,
    val upcoming: Int = 0,
    val total: Int = 0
)

fun stageFraction(value: Double, qty: Double): Double {
    if (qty <= 0) return 0.0
    return (value / qty * 100.0).coerceIn(0.0, 100.0)
}

fun computeTaskProgress(qty: Double, removal: Double, fab: Double, install: Double): Int {
    if (qty <= 0) return 0
    val stages = listOf(
        stageFraction(removal, qty),
        stageFraction(fab, qty),
        stageFraction(install, qty)
    )
    return (stages.sum() / 3.0).roundToInt()
}

fun daysRemaining(targetIso: String): Long? {
    if (targetIso.isBlank()) return null
    return try {
        val target = LocalDate.parse(targetIso.take(10), DateTimeFormatter.ISO_LOCAL_DATE)
        val today = LocalDate.now()
        ChronoUnit.DAYS.between(today, target)
    } catch (_: Exception) {
        null
    }
}

fun daysBetween(fromIso: String, toIso: String): Long? {
    if (fromIso.isBlank() || toIso.isBlank()) return null
    return try {
        val from = LocalDate.parse(fromIso.take(10), DateTimeFormatter.ISO_LOCAL_DATE)
        val to = LocalDate.parse(toIso.take(10), DateTimeFormatter.ISO_LOCAL_DATE)
        ChronoUnit.DAYS.between(from, to)
    } catch (_: Exception) {
        null
    }
}
