package com.example.ui.screens.projects

import androidx.compose.ui.graphics.Color
import com.example.data.local.entities.ProjectEntity
import com.example.data.local.entities.ProjectStageEntity

data class ProjectChipDisplay(
    val label: String,
    val colorHex: String? = null,
    val isCompleted: Boolean = false,
    val isPending: Boolean = false,
    val isCancelled: Boolean = false
)

object ProjectStatusHelper {

    fun getCurrentStage(
        project: ProjectEntity,
        stages: List<ProjectStageEntity>
    ): ProjectStageEntity? {
        val sortedStages = stages.sortedBy { it.displayOrder }
        val completedIds = project.completedStageIds.split(",").filter { it.isNotBlank() }.toSet()
        return sortedStages.firstOrNull { !completedIds.contains(it.id) }
    }

    fun getProjectChip(
        project: ProjectEntity,
        stages: List<ProjectStageEntity>
    ): ProjectChipDisplay {
        val isLifecycleCompleted = project.status == "COMPLETED" || project.status == "تکمیل شده" || project.status == "تکمیل‌شده"
        val isLifecyclePending = project.status == "PENDING" || project.status == "معلق"
        val isLifecycleCancelled = project.status == "CANCELLED" || project.status == "لغوشده" || project.status == "لغو شده"

        if (isLifecycleCompleted) {
            return ProjectChipDisplay(label = "تکمیل‌شده", isCompleted = true, colorHex = "#10B981")
        }
        if (isLifecyclePending) {
            return ProjectChipDisplay(label = "معلق", isPending = true, colorHex = "#F59E0B")
        }
        if (isLifecycleCancelled) {
            return ProjectChipDisplay(label = "لغوشده", isCancelled = true, colorHex = "#EF4444")
        }

        // Active project:
        val currentStage = getCurrentStage(project, stages)
        return if (currentStage != null) {
            ProjectChipDisplay(
                label = "در مرحله ${currentStage.name}",
                colorHex = currentStage.colorHex
            )
        } else {
            ProjectChipDisplay(label = "تکمیل‌شده", isCompleted = true, colorHex = "#10B981")
        }
    }

    /**
     * Checks whether a project matches a selected filter chip.
     * Filter options:
     * - "همه"
     * - "در مرحله [نام مرحله]" -> only matches active projects currently in this stage
     * - "تکمیل‌شده" -> lifecycle COMPLETED
     * - "معلق" -> lifecycle PENDING
     * - "لغوشده" -> lifecycle CANCELLED
     */
    fun matchesFilter(
        project: ProjectEntity,
        stages: List<ProjectStageEntity>,
        selectedFilter: String
    ): Boolean {
        if (selectedFilter == "همه") return true

        val isLifecycleCompleted = project.status == "COMPLETED" || project.status == "تکمیل شده" || project.status == "تکمیل‌شده"
        val isLifecyclePending = project.status == "PENDING" || project.status == "معلق"
        val isLifecycleCancelled = project.status == "CANCELLED" || project.status == "لغوشده" || project.status == "لغو شده"
        val isLifecycleActive = !isLifecycleCompleted && !isLifecyclePending && !isLifecycleCancelled

        if (selectedFilter == "تکمیل‌شده" || selectedFilter == "تکمیل شده") {
            return isLifecycleCompleted
        }
        if (selectedFilter == "معلق") {
            return isLifecyclePending
        }
        if (selectedFilter == "لغوشده" || selectedFilter == "لغو شده") {
            return isLifecycleCancelled
        }

        if (selectedFilter.startsWith("در مرحله ")) {
            if (!isLifecycleActive) return false
            val currentStage = getCurrentStage(project, stages)
            return currentStage != null && "در مرحله ${currentStage.name}" == selectedFilter
        }

        return project.status == selectedFilter
    }
}
