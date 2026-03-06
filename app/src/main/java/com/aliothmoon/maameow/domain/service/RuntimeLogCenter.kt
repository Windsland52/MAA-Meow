package com.aliothmoon.maameow.domain.service

import com.aliothmoon.maameow.constant.LogConfig
import com.aliothmoon.maameow.data.log.TaskLogWriter
import com.aliothmoon.maameow.data.model.LogItem
import com.aliothmoon.maameow.data.model.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class RuntimeLogCenter(
    private val taskLogWriter: TaskLogWriter?
) {
    private companion object {
        private const val LOG_FLUSH_INTERVAL_MS = 75L
    }

    private val serialDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val serialScope = CoroutineScope(SupervisorJob() + serialDispatcher)

    private val _logs = MutableStateFlow<List<LogItem>>(emptyList())
    val logs: StateFlow<List<LogItem>> = _logs.asStateFlow()
    private val pendingLogs = ArrayDeque<LogItem>()
    private var flushJob: Job? = null

    @Volatile
    private var sessionActive = false

    suspend fun startSession(taskNames: List<String>): Boolean = withContext(serialDispatcher) {
        cancelScheduledFlushLocked()
        pendingLogs.clear()
        _logs.value = emptyList()
        sessionActive = true
        taskLogWriter?.startSession(taskNames) ?: true
    }

    fun append(content: String, level: LogLevel = LogLevel.MESSAGE, tooltip: String? = null) {
        append(LogItem(content = content, level = level, tooltip = tooltip))
    }

    fun append(logItem: LogItem) {
        serialScope.launch {
            pendingLogs.addLast(logItem)
            scheduleFlushLocked()
        }
    }

    suspend fun appendAndWait(
        content: String,
        level: LogLevel = LogLevel.MESSAGE,
        tooltip: String? = null
    ) {
        appendAndWait(LogItem(content = content, level = level, tooltip = tooltip))
    }

    suspend fun appendAndWait(logItem: LogItem) {
        withContext(serialDispatcher) {
            flushNowLocked()
            appendDirectLocked(logItem)
        }
    }

    fun clearRuntimeLogs() {
        serialScope.launch {
            cancelScheduledFlushLocked()
            pendingLogs.clear()
            _logs.value = emptyList()
        }
    }

    suspend fun clearRuntimeLogsAndWait() {
        withContext(serialDispatcher) {
            cancelScheduledFlushLocked()
            pendingLogs.clear()
            _logs.value = emptyList()
        }
    }

    /** 仅写入日志文件，不在运行时 UI 中展示 */
    fun appendToFileOnly(content: String, level: LogLevel = LogLevel.TRACE) {
        val logItem = LogItem(content = content, level = level)
        serialScope.launch {
            taskLogWriter?.appendLog(logItem)
        }
    }

    fun endSession(status: String = "COMPLETED") {
        serialScope.launch {
            endSessionLocked(status)
        }
    }

    suspend fun endSessionAndWait(status: String = "COMPLETED") {
        withContext(serialDispatcher) {
            endSessionLocked(status)
        }
    }

    fun completeSession(
        status: String,
        finalLog: String? = null,
        level: LogLevel = LogLevel.ERROR
    ) {
        serialScope.launch {
            completeSessionLocked(status, finalLog, level)
        }
    }

    suspend fun completeSessionAndWait(
        status: String,
        finalLog: String? = null,
        level: LogLevel = LogLevel.ERROR
    ) {
        withContext(serialDispatcher) {
            completeSessionLocked(status, finalLog, level)
        }
    }

    private fun scheduleFlushLocked() {
        if (flushJob?.isActive == true) return
        flushJob = serialScope.launch {
            try {
                delay(LOG_FLUSH_INTERVAL_MS)
                flushPendingLocked()
            } finally {
                flushJob = null
            }
        }
    }

    private fun cancelScheduledFlushLocked() {
        flushJob?.cancel()
        flushJob = null
    }

    private suspend fun flushNowLocked() {
        cancelScheduledFlushLocked()
        flushPendingLocked()
    }

    private suspend fun flushPendingLocked() {
        if (pendingLogs.isEmpty()) return
        val batch = pendingLogs.toList()
        pendingLogs.clear()
        val current = _logs.value
        val merged = if (current.isEmpty()) batch else current + batch
        _logs.value = if (merged.size > LogConfig.MAX_LOG_COUNT) {
            merged.takeLast(LogConfig.MAX_LOG_COUNT)
        } else {
            merged
        }
        taskLogWriter?.let { writer ->
            batch.forEach { writer.appendLog(it) }
        }
    }

    private suspend fun appendDirectLocked(logItem: LogItem) {
        val current = _logs.value
        _logs.value = if (current.size >= LogConfig.MAX_LOG_COUNT) {
            current.drop(current.size - LogConfig.MAX_LOG_COUNT + 1) + logItem
        } else {
            current + logItem
        }
        taskLogWriter?.appendLog(logItem)
    }

    private suspend fun completeSessionLocked(
        status: String,
        finalLog: String?,
        level: LogLevel
    ) {
        if (!sessionActive) return
        finalLog?.let {
            appendDirectLocked(LogItem(content = it, level = level))
        }
        endSessionLocked(status)
    }

    private suspend fun endSessionLocked(status: String) {
        if (!sessionActive) return
        flushNowLocked()
        taskLogWriter?.endSession(status)
        sessionActive = false
    }
}
