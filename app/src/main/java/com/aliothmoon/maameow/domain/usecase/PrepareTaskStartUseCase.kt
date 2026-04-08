package com.aliothmoon.maameow.domain.usecase

import com.aliothmoon.maameow.data.model.TaskChainNode
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.domain.models.RunMode
import com.aliothmoon.maameow.domain.service.AppAliveChecker
import com.aliothmoon.maameow.remote.AppAliveStatus
import timber.log.Timber

class PrepareTaskStartUseCase(
    private val analyzeTaskChainUseCase: AnalyzeTaskChainUseCase,
    private val appAliveChecker: AppAliveChecker,
    private val appSettings: AppSettingsManager,
    private val isPackageInstalled: (String) -> Boolean = { true },
) {
    companion object {
        const val NO_WAKE_UP_WARNING_MESSAGE =
            "当前任务链不会启动游戏，且检测到游戏未运行。继续执行可能直接失败，是否仍要启动？"
        const val SCHEDULED_NO_WAKE_UP_FAILURE_MESSAGE =
            "未配置开始唤醒且游戏未运行，已取消本次定时执行"
        const val GAME_NOT_INSTALLED_WARNING_MESSAGE =
            "未检测到当前客户端类型对应的游戏安装包，请检查「开始唤醒」中的客户端类型设置是否正确。是否仍要启动？"
        const val SCHEDULED_GAME_NOT_INSTALLED_MESSAGE =
            "未检测到当前客户端类型对应的游戏安装包，请检查「开始唤醒」中的客户端类型设置，已取消本次定时执行"
    }

    suspend operator fun invoke(
        chain: List<TaskChainNode>,
        context: TaskStartContext,
    ): TaskStartDecision {
        val plan = when (val analyzeResult = analyzeTaskChainUseCase(chain)) {
            is AnalyzeTaskChainResult.Ready -> analyzeResult.plan
            is AnalyzeTaskChainResult.Blocked -> {
                return TaskStartDecision.Blocked(
                    reason = analyzeResult.reason.toDecisionReason(),
                    message = analyzeResult.message,
                )
            }
        }

        // 检查游戏安装包是否存在
        val packageName = plan.gamePackageName
        if (packageName != null
            && !isPackageInstalled(packageName)
            && !context.acknowledgements.contains(TaskStartAcknowledgement.GAME_NOT_INSTALLED)
        ) {
            return when (context.mode) {
                TaskStartMode.MANUAL -> TaskStartDecision.RequiresConfirmation(
                    reason = TaskStartDecisionReason.GAME_NOT_INSTALLED,
                    message = GAME_NOT_INSTALLED_WARNING_MESSAGE,
                    acknowledgement = TaskStartAcknowledgement.GAME_NOT_INSTALLED,
                )
                TaskStartMode.SCHEDULED -> TaskStartDecision.Blocked(
                    reason = TaskStartDecisionReason.GAME_NOT_INSTALLED,
                    message = SCHEDULED_GAME_NOT_INSTALLED_MESSAGE,
                )
            }
        }

        val runMode = appSettings.runMode.value
        if (plan.launchesGame ||
            runMode == RunMode.FOREGROUND ||
            context.acknowledgements.contains(TaskStartAcknowledgement.GAME_NOT_RUNNING_WITHOUT_WAKE_UP)
        ) {
            return TaskStartDecision.Ready(plan)
        }
        if (packageName == null) {
            Timber.w(
                "PrepareTaskStart: cannot resolve package name for clientType=%s",
                plan.clientType
            )
            return TaskStartDecision.Ready(plan)
        }

        return when (appAliveChecker.isAppAlive(packageName)) {
            AppAliveStatus.DEAD -> {
                when (context.mode) {
                    TaskStartMode.MANUAL -> {
                        TaskStartDecision.RequiresConfirmation(
                            reason = TaskStartDecisionReason.GAME_NOT_RUNNING_WITHOUT_WAKE_UP,
                            message = NO_WAKE_UP_WARNING_MESSAGE,
                            acknowledgement = TaskStartAcknowledgement.GAME_NOT_RUNNING_WITHOUT_WAKE_UP,
                        )
                    }

                    TaskStartMode.SCHEDULED -> {
                        TaskStartDecision.Blocked(
                            reason = TaskStartDecisionReason.GAME_NOT_RUNNING_WITHOUT_WAKE_UP,
                            message = SCHEDULED_NO_WAKE_UP_FAILURE_MESSAGE,
                        )
                    }
                }
            }

            AppAliveStatus.UNKNOWN -> {
                Timber.w("PrepareTaskStart: unable to determine whether %s is alive", packageName)
                TaskStartDecision.Ready(plan)
            }

            else -> TaskStartDecision.Ready(plan)
        }
    }

}

data class TaskStartContext(
    val mode: TaskStartMode,
    val acknowledgements: Set<TaskStartAcknowledgement> = emptySet(),
) {
    fun acknowledged(acknowledgement: TaskStartAcknowledgement): TaskStartContext {
        return copy(acknowledgements = acknowledgements + acknowledgement)
    }
}

enum class TaskStartMode {
    MANUAL,
    SCHEDULED,
}

enum class TaskStartAcknowledgement {
    GAME_NOT_RUNNING_WITHOUT_WAKE_UP,
    GAME_NOT_INSTALLED,
}

enum class TaskStartDecisionReason {
    INVALID_CHAIN,
    NO_EXECUTABLE_TASKS,
    GAME_NOT_RUNNING_WITHOUT_WAKE_UP,
    GAME_NOT_INSTALLED,
}

sealed interface TaskStartDecision {
    data class Ready(val plan: TaskChainPlan) : TaskStartDecision

    data class RequiresConfirmation(
        val reason: TaskStartDecisionReason,
        val message: String,
        val acknowledgement: TaskStartAcknowledgement,
    ) : TaskStartDecision

    data class Blocked(
        val reason: TaskStartDecisionReason,
        val message: String,
    ) : TaskStartDecision
}

private fun AnalyzeTaskChainFailureReason.toDecisionReason(): TaskStartDecisionReason {
    return when (this) {
        AnalyzeTaskChainFailureReason.INVALID_CHAIN -> TaskStartDecisionReason.INVALID_CHAIN
        AnalyzeTaskChainFailureReason.NO_EXECUTABLE_TASKS -> TaskStartDecisionReason.NO_EXECUTABLE_TASKS
    }
}
