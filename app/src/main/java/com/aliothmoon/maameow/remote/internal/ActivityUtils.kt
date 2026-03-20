package com.aliothmoon.maameow.remote.internal

import android.app.ActivityOptions
import android.content.Intent
import com.aliothmoon.maameow.third.FakeContext
import com.aliothmoon.maameow.third.Ln
import com.aliothmoon.maameow.third.wrappers.ServiceManager

object ActivityUtils {
    /**
     * 以 shell 身份启动指定 Intent 的 Activity，绕过 BAL 限制。
     */
    @JvmStatic
    @JvmOverloads
    fun startActivity(intent: Intent, displayId: Int = 0): Boolean {
        val am = ServiceManager.getActivityManager()
        try {
            val launchOptions = ActivityOptions.makeBasic()
            if (displayId != 0) {
                launchOptions.setLaunchDisplayId(displayId)
            }
            val ret = am.startActivity(intent, launchOptions.toBundle())
            if (ret != 0) {
                return startViaAmCommand(intent, displayId)
            }
            return true
        } catch (e: Exception) {
            Ln.w("startActivity failed, fallback to am command", e)
            return startViaAmCommand(intent, displayId)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun startApp(
        packageName: String,
        displayId: Int,
        forceStop: Boolean = true,
        excludeFromRecents: Boolean = true
    ): Boolean {
        val pm = FakeContext.get().packageManager

        val intent = pm.getLaunchIntentForPackage(packageName) ?: run {
            pm.getLeanbackLaunchIntentForPackage(packageName)
        }

        if (intent == null) {
            Ln.w("Cannot create launch intent for app $packageName")
            return false
        }

        var flag = Intent.FLAG_ACTIVITY_NEW_TASK
        if (excludeFromRecents) {
            flag = flag or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }
        if (displayId != 0) {
            flag = flag or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        }
        intent.addFlags(flag)

        if (forceStop) {
            ServiceManager.getActivityManager().forceStopPackage(packageName)
        }
        Ln.i("startApp ${intent.component?.flattenToShortString()}")

        return startActivity(intent, displayId)
    }

    private fun startViaAmCommand(intent: Intent, displayId: Int): Boolean {
        try {
            val component = intent.component ?: return false
            val cmd = buildString {
                append("am start")
                append(" --display $displayId")
                intent.action?.let { append(" -a $it") }
                append(" -n ${component.flattenToShortString()}")
                append(" -f ${intent.flags}")
            }
            Ln.d("Executing: $cmd")
            val process = Runtime.getRuntime().exec(cmd)
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                Ln.w("am command exited with code $exitCode")
                return false
            }
            return true
        } catch (e: Exception) {
            Ln.e("am command fallback also failed", e)
            return false
        }
    }
}
