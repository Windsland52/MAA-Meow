package com.aliothmoon.maameow.data.notification.provider

import com.aliothmoon.maameow.data.api.HttpClientHelper
import com.aliothmoon.maameow.data.notification.NotificationSettings
import timber.log.Timber

class TelegramProvider(
    private val httpClient: HttpClientHelper,
    private val settingsProvider: () -> NotificationSettings
) : NotificationProvider {

    override val id = "Telegram"

    override suspend fun send(title: String, content: String): Boolean {
        val settings = settingsProvider()
        val botToken = settings.telegramBotToken.takeIf { it.isNotEmpty() } ?: return false
        val chatId = settings.telegramChatId.takeIf { it.isNotEmpty() } ?: return false

        val url = "https://api.telegram.org/bot$botToken/sendMessage"
        val topicId = settings.telegramTopicId.takeIf { it.isNotEmpty() }
        val threadField = if (topicId != null) ""","message_thread_id":"$topicId"""" else ""
        val body = """{"chat_id":"$chatId","text":"$title: $content"$threadField}"""

        return runCatching {
            val response = httpClient.post(url, body)
            response.use { it.body.string().contains("\"ok\":false").not() }
        }.getOrElse {
            Timber.e(it, "Telegram send failed")
            false
        }
    }
}
