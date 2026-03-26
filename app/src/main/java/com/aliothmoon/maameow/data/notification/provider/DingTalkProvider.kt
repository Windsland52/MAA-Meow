package com.aliothmoon.maameow.data.notification.provider

import android.util.Base64
import com.aliothmoon.maameow.data.api.HttpClientHelper
import com.aliothmoon.maameow.data.notification.NotificationSettings
import timber.log.Timber
import java.net.URLEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class DingTalkProvider(
    private val httpClient: HttpClientHelper,
    private val settingsProvider: () -> NotificationSettings
) : NotificationProvider {

    override val id = "DingTalk"

    override suspend fun send(title: String, content: String): Boolean {
        val settings = settingsProvider()
        val accessToken = settings.dingTalkAccessToken.takeIf { it.isNotEmpty() } ?: return false

        var url = "https://oapi.dingtalk.com/robot/send?access_token=$accessToken"
        val secret = settings.dingTalkSecret.takeIf { it.isNotEmpty() }
        if (secret != null) {
            val timestamp = System.currentTimeMillis()
            val stringToSign = "$timestamp\n$secret"
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
            val sign = URLEncoder.encode(
                Base64.encodeToString(mac.doFinal(stringToSign.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP),
                "UTF-8"
            )
            url += "&timestamp=$timestamp&sign=$sign"
        }

        val body = """{"msgtype":"text","text":{"content":"$title: $content"}}"""

        return runCatching {
            val response = httpClient.post(url, body)
            response.use { it.isSuccessful }
        }.getOrElse {
            Timber.e(it, "DingTalk send failed")
            false
        }
    }
}
