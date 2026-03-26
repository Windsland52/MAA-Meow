package com.aliothmoon.maameow.domain.service

import com.aliothmoon.maameow.data.api.HttpClientHelper
import com.aliothmoon.maameow.data.notification.NotificationSettings
import com.aliothmoon.maameow.data.notification.NotificationSettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ExternalNotificationService(
    private val settingsManager: NotificationSettingsManager,
    private val httpClientHelper: HttpClientHelper,
    private val runtimeLogCenter: RuntimeLogCenter,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun send(title: String, content: String) {
        scope.launch {
            dispatchToProviders(title, content)
        }
    }

    fun sendWithLogs(title: String, content: String) {
        scope.launch {
            val settings = settingsManager.settings.first()
            val body = if (settings.includeLogDetails.toBooleanStrictOrNull() == true) {
                val logs = runtimeLogCenter.logs.first()
                    .takeLast(20)
                    .joinToString("\n") { "[${it.formattedTime}] ${it.content}" }
                if (logs.isNotEmpty()) "$logs\n$content" else content
            } else {
                content
            }
            dispatchToProviders(title, body)
        }
    }

    private suspend fun dispatchToProviders(title: String, content: String) {
        val settings = settingsManager.settings.first()
        val enabledIds = settings.enabledProviders
            .split(",")
            .filter { it.isNotBlank() }

        if (enabledIds.isEmpty()) return

        val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        for (id in enabledIds) {
            runCatching {
                when (id) {
                    "ServerChan" -> sendServerChan(settings, title, content)
                    "Bark" -> sendBark(settings, title, content)
                    "Telegram" -> sendTelegram(settings, title, content)
                    "DingTalk" -> sendDingTalk(settings, title, content)
                    "CustomWebhook" -> sendCustomWebhook(settings, title, content, time)
                    else -> Timber.w("未知通知渠道: $id")
                }
            }.onFailure {
                Timber.e(it, "通知渠道 $id 发送失败")
            }
        }
    }

    private suspend fun sendServerChan(settings: NotificationSettings, title: String, content: String) {
        val sendKey = settings.serverChanSendKey
        if (sendKey.isBlank()) return
        val url = "https://sctapi.ftqq.com/$sendKey.send"
        httpClientHelper.post(
            url = url,
            body = """{"title":"$title","desp":"$content"}"""
        )
    }

    private suspend fun sendBark(settings: NotificationSettings, title: String, content: String) {
        val server = settings.barkServer.trimEnd('/')
        val sendKey = settings.barkSendKey
        if (sendKey.isBlank()) return
        val url = "$server/$sendKey/$title/$content"
        httpClientHelper.get(url)
    }

    private suspend fun sendTelegram(settings: NotificationSettings, title: String, content: String) {
        val botToken = settings.telegramBotToken
        val chatId = settings.telegramChatId
        if (botToken.isBlank() || chatId.isBlank()) return
        val text = "$title\n$content"
        val url = "https://api.telegram.org/bot$botToken/sendMessage"
        val topicId = settings.telegramTopicId
        val body = if (topicId.isNotBlank()) {
            """{"chat_id":"$chatId","text":"$text","message_thread_id":$topicId}"""
        } else {
            """{"chat_id":"$chatId","text":"$text"}"""
        }
        httpClientHelper.post(url = url, body = body)
    }

    private suspend fun sendDingTalk(settings: NotificationSettings, title: String, content: String) {
        val accessToken = settings.dingTalkAccessToken
        if (accessToken.isBlank()) return
        val url = "https://oapi.dingtalk.com/robot/send?access_token=$accessToken"
        val body = """{"msgtype":"text","text":{"content":"$title\n$content"}}"""
        httpClientHelper.post(url = url, body = body)
    }

    private suspend fun sendCustomWebhook(
        settings: NotificationSettings,
        title: String,
        content: String,
        time: String
    ) {
        val webhookUrl = settings.customWebhookUrl
        if (webhookUrl.isBlank()) return
        val bodyTemplate = settings.customWebhookBody.ifBlank {
            """{"title":"{title}","content":"{content}","time":"{time}"}"""
        }
        val body = bodyTemplate
            .replace("{title}", title)
            .replace("{content}", content)
            .replace("{time}", time)
        httpClientHelper.post(url = webhookUrl, body = body)
    }
}
