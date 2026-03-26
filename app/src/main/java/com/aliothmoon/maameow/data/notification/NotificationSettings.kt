package com.aliothmoon.maameow.data.notification

import com.aliothmoon.preferences.PrefKey
import com.aliothmoon.preferences.PrefSchema

@PrefSchema(name = "NotificationSettings")
data class NotificationSettings(
    @PrefKey(default = "true") val sendOnComplete: String = "true",
    @PrefKey(default = "true") val sendOnError: String = "true",
    @PrefKey(default = "false") val sendOnServiceDied: String = "false",
    @PrefKey(default = "false") val includeLogDetails: String = "false",
    @PrefKey(default = "") val enabledProviders: String = "",
    @PrefKey(default = "") val serverChanSendKey: String = "",
    @PrefKey(default = "https://api.day.app") val barkServer: String = "https://api.day.app",
    @PrefKey(default = "") val barkSendKey: String = "",
    @PrefKey(default = "") val telegramBotToken: String = "",
    @PrefKey(default = "") val telegramChatId: String = "",
    @PrefKey(default = "") val telegramTopicId: String = "",
    @PrefKey(default = "") val dingTalkAccessToken: String = "",
    @PrefKey(default = "") val dingTalkSecret: String = "",
    @PrefKey(default = "") val customWebhookUrl: String = "",
    @PrefKey(default = "") val customWebhookBody: String = "",
)
