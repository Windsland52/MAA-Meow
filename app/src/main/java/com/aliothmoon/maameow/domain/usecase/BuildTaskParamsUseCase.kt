package com.aliothmoon.maameow.domain.usecase

import com.aliothmoon.maameow.data.model.MallConfig
import com.aliothmoon.maameow.data.model.WakeUpConfig
import com.aliothmoon.maameow.data.preferences.TaskChainState
import com.aliothmoon.maameow.domain.models.resolveMallCreditFightAvailability
import com.aliothmoon.maameow.maa.task.MaaTaskParams
import timber.log.Timber

class BuildTaskParamsUseCase(private val chainState: TaskChainState) {
    operator fun invoke(): List<MaaTaskParams> {
        val enabledNodes = chainState.chain.value
            .filter { it.enabled }
            .sortedBy { it.order }
        val creditFightAvailability = resolveMallCreditFightAvailability(enabledNodes)
        val clientType = chainState.getClientType()

        if (!creditFightAvailability.isAvailable && enabledNodes.any { (it.config as? MallConfig)?.creditFight == true }) {
            Timber.w(
                "Credit fight disabled because a fight task has no resolvable active stage. task=%s order=%d",
                creditFightAvailability.blockingTaskName ?: "unknown",
                creditFightAvailability.blockingTaskOrder ?: -1,
            )
        }

        return enabledNodes.map { node ->
            when (val config = node.config) {
                is MallConfig -> config.toTaskParams(
                    creditFightEnabled = config.creditFight && creditFightAvailability.isAvailable,
                    clientType = clientType,
                )

                else -> config.toTaskParams()
            }
        }
    }
}
