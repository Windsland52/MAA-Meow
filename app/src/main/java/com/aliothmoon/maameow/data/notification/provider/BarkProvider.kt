package com.aliothmoon.maameow.data.notification.provider

import com.aliothmoon.maameow.data.api.HttpClientHelper
import com.aliothmoon.maameow.data.notification.NotificationSettings
import timber.log.Timber

class BarkProvider(
    private val httpClient: HttpClientHelper,
    private val settingsProvider: () -> NotificationSettings
) : NotificationProvider {

    override val id = "Bark"

    override suspend fun send(title: String, content: String): Boolean {
        val settings = settingsProvider()
        val barkServer = settings.barkServer.takeIf { it.isNotEmpty() } ?: return false
        val sendKey = settings.barkSendKey.takeIf { it.isNotEmpty() } ?: return false

        val url = "${barkServer.trimEnd('/')}/push"
        val body = """{"title":"$title","body":"$content","device_key":"$sendKey"}"""

        return runCatching {
            val response = httpClient.post(url, body)
            response.use { it.isSuccessful }
        }.getOrElse {
            Timber.e(it, "Bark send failed")
            false
        }
    }
}
