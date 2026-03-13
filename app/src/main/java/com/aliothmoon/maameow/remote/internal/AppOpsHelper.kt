package com.aliothmoon.maameow.remote.internal

import com.aliothmoon.maameow.third.Ln

object AppOpsHelper {
    private const val TAG = "AppOpsHelper"

    fun setPlayAudioOpAllowed(packageName: String?, isAllowed: Boolean) {
        if (packageName.isNullOrBlank()) return
        val op = if (isAllowed) "allow" else "deny"
        try {
            val process = Runtime.getRuntime()
                .exec(arrayOf("sh", "-c", "appops set $packageName PLAY_AUDIO $op"))
            val exitCode = process.waitFor()
            Ln.i("$TAG: appops set $packageName PLAY_AUDIO $op -> exitCode=$exitCode")
        } catch (e: Exception) {
            Ln.e("$TAG: setPlayAudioOpAllowed failed: ${e.message}")
        }
    }
}