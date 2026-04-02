package com.aliothmoon.maameow.presentation.view.panel.fight

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.data.model.FightConfig
import com.aliothmoon.maameow.presentation.components.CheckBoxWithLabel
import com.aliothmoon.maameow.presentation.components.INumericField

/**
 * 理智药/源石/次数区域
 */
@Composable
fun MedicineAndStoneSection(
    config: FightConfig,
    onConfigChange: (FightConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 使用理智药
        CheckBoxWithLabel(
            checked = config.useMedicine,
            onCheckedChange = {
                onConfigChange(
                    config.copy(
                        useMedicine = it,
                        useStone = if (!it) false else config.useStone
                    )
                )
            },
            label = "使用理智药",
            enabled = !config.useStone,
        )
        AnimatedVisibility(visible = config.useMedicine) {
            INumericField(
                value = config.medicineNumber,
                onValueChange = { onConfigChange(config.copy(medicineNumber = it)) },
                label = "最多使用 N 个理智药",
                minimum = 0,
                maximum = 999,
                enabled = !config.useStone,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }

        // 使用源石 TODO 暂时不支持使用
//        UseStoneSection(config, onConfigChange)

        // 战斗次数限制
        CheckBoxWithLabel(
            checked = config.hasTimesLimited,
            onCheckedChange = { onConfigChange(config.copy(hasTimesLimited = it)) },
            label = "指定次数",
        )
        AnimatedVisibility(visible = config.hasTimesLimited) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                INumericField(
                    value = config.maxTimes,
                    onValueChange = { onConfigChange(config.copy(maxTimes = it)) },
                    label = "战斗 N 次后停止",
                    minimum = 0,
                    maximum = 999,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )

                // 代理倍率整除警告
                val showSeriesWarning = config.series > 0 &&
                        config.maxTimes % config.series != 0
                if (showSeriesWarning) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "战斗次数 ${config.maxTimes} 无法被代理倍率 ${config.series} 整除，可能无法完全消耗理智",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}
