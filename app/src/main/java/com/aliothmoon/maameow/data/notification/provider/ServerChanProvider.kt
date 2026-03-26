package com.aliothmoon.maameow.data.notification.provider

import com.aliothmoon.maameow.data.api.HttpClientHelper
import com.aliothmoon.maameow.data.notification.NotificationSettings
import timber.log.Timber

class ServerChanProvider(
    private val httpClient: HttpClientHelper,
    private val settingsProvider: () -> NotificationSettings
) : NotificationProvider {

    override val id = "ServerChan"

    override suspend fun send(title: String, content: String): Boolean {
        val settings = settingsProvider()
        val sendKey = settings.serverChanSendKey.takeIf { it.isNotEmpty() } ?: return false

        val url = if (sendKey.startsWith("sctp")) {
            "https://$sendKey.push.ft07.com/send"
        } else {
            "https://sctapi.ftqq.com/$sendKey.send"
        }

        return runCatching {
            val response = httpClient.postForm(
                url,
                mapOf("text" to title.take(32).replace("\n", ""), "desp" to content)
            )
            response.use { it.isSuccessful }
        }.getOrElse {
            Timber.e(it, "ServerChan send failed")
            false
        }
    }
}
