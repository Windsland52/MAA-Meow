package com.aliothmoon.maameow.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.aliothmoon.maameow.data.datasource.ResourceDownloader
import com.aliothmoon.maameow.data.model.update.UpdateInfo

/**
 * 资源更新确认弹窗
 */
@Composable
fun UpdateConfirmDialog(
    updateInfo: UpdateInfo,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val displayVersion = ResourceDownloader.formatVersionForDisplay(updateInfo.version)
    
    AdaptiveTaskPromptDialog(
        visible = true,
        title = "发现资源更新",
        message = "检测到新版本资源: $displayVersion\n\n建议立即更新以获取最新的任务配置和功能支持。",
        onConfirm = onConfirm,
        onDismissRequest = onDismiss,
        confirmText = "立即下载",
        confirmColor = Color(0xFF4CAF50),
        dismissText = "稍后再说",
        icon = Icons.Rounded.Info
    )
}
