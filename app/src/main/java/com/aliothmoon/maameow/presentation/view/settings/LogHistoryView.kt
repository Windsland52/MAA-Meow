package com.aliothmoon.maameow.presentation.view.settings

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import com.aliothmoon.maameow.presentation.components.AdaptiveTaskPromptDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aliothmoon.maameow.data.log.LogEntry
import com.aliothmoon.maameow.data.log.LogFileInfo
import com.aliothmoon.maameow.domain.service.LogExportService
import com.aliothmoon.maameow.presentation.components.TopAppBar
import com.aliothmoon.maameow.presentation.viewmodel.LogHistoryViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LogHistoryView(
    navController: NavController,
    viewModel: LogHistoryViewModel = koinViewModel(),
    logExportService: LogExportService = koinInject()
) {
    val logFiles by viewModel.logFiles.collectAsStateWithLifecycle()
    val selectedLogEntries by viewModel.selectedLogEntries.collectAsStateWithLifecycle()
    val selectedFileName by viewModel.selectedFileName.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 拦截系统返回键：详情页时先回到列表
    BackHandler(enabled = selectedLogEntries != null) {
        viewModel.clearSelectedLog()
    }

    // 根据是否选中日志显示不同页面
    if (selectedLogEntries != null) {
        LogDetailView(
            fileName = selectedFileName ?: "",
            entries = selectedLogEntries!!,
            onBack = { viewModel.clearSelectedLog() }
        )
    } else {
        LogFileListView(
            logFiles = logFiles,
            isLoading = isLoading,
            onFileClick = { viewModel.loadLogContent(it) },
            onFileDelete = { viewModel.deleteLogFile(it) },
            onCleanup = { viewModel.cleanupOldLogs() },
            onExport = {
                coroutineScope.launch {
                    val intent = logExportService.exportAllLogs()
                    if (intent != null) {
                        context.startActivity(Intent.createChooser(intent, "导出日志"))
                    }
                }
            },
            onBack = { navController.navigateUp() }
        )
    }
}

@Composable
private fun LogFileListView(
    logFiles: List<LogFileInfo>,
    isLoading: Boolean,
    onFileClick: (LogFileInfo) -> Unit,
    onFileDelete: (LogFileInfo) -> Unit,
    onCleanup: () -> Unit,
    onExport: () -> Unit,
    onBack: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf<LogFileInfo?>(null) }

    // 删除确认弹窗
    showDeleteConfirm?.let { logFile ->
        AdaptiveTaskPromptDialog(
            visible = true,
            title = "确认删除",
            message = "确定要删除日志文件 ${logFile.displayTime} 吗？",
            onConfirm = {
                onFileDelete(logFile)
                showDeleteConfirm = null
            },
            onDismissRequest = { showDeleteConfirm = null },
            confirmText = "删除",
            dismissText = "取消",
            icon = Icons.Rounded.Delete,
            confirmColor = MaterialTheme.colorScheme.error
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "历史日志",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onBack,
                actions = {
                    IconButton(onClick = onExport) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "导出",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = onCleanup) {
                        Text("清理30天前", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && logFiles.isEmpty()) {
                // 仅在列表为空且加载中时显示全屏 loading
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (logFiles.isEmpty()) {
                Text(
                    text = "暂无历史日志",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = logFiles,
                        key = { it.fileName }
                    ) { logFile ->
                        LogFileItem(
                            logFile = logFile,
                            onClick = { onFileClick(logFile) },
                            onDelete = { showDeleteConfirm = logFile }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogFileItem(
    logFile: LogFileInfo,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = logFile.displayTime,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${logFile.taskCount} 个任务 | ${formatFileSize(logFile.fileSize)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun LogDetailView(
    fileName: String,
    entries: List<LogEntry>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = "日志详情",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(
                items = entries,
                key = { entry ->
                    when (entry) {
                        is LogEntry.Header -> "header_${entry.startTime}"
                        is LogEntry.Log -> "log_${entry.time}_${entry.content.hashCode()}"
                        is LogEntry.Footer -> "footer_${entry.endTime}"
                    }
                }
            ) { entry ->
                when (entry) {
                    is LogEntry.Header -> {
                        Column {
                            Text(
                                text = "========== 任务开始 ==========",
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "时间: ${formatTime(entry.startTime)}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "任务: ${entry.tasks.joinToString(", ")}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "================================",
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    is LogEntry.Log -> {
                        val color = getLogLevelColor(entry.level)
                        Text(
                            text = "[${formatTimeShort(entry.time)}] [${entry.level}] ${entry.content}",
                            color = color,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                    is LogEntry.Footer -> {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "========== 任务结束 ==========",
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "结束时间: ${formatTime(entry.endTime)}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "状态: ${entry.status}",
                                color = if (entry.status == "COMPLETED") Color(0xFF4CAF50) else Color(0xFFF44336),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "================================",
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getLogLevelColor(level: String): Color {
    return when (level) {
        "ERROR" -> Color(0xFFF44336)
        "WARNING" -> Color(0xFFFF9800)
        "SUCCESS" -> Color(0xFF4CAF50)
        "INFO" -> Color(0xFF2196F3)
        "RARE" -> Color(0xFFE040FB)
        "TRACE" -> Color(0xFFC0C4CC)
        "MESSAGE" -> Color(0xFF909399)
        else -> Color(0xFF333333)
    }
}

private fun formatTime(timestamp: Long): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss (Z)"))
}

private fun formatTimeShort(timestamp: Long): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> "${size / (1024 * 1024)} MB"
    }
}
