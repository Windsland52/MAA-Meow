package com.aliothmoon.maameow.manager

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.aliothmoon.maameow.BuildConfig
import com.aliothmoon.maameow.ILogcatService
import com.aliothmoon.maameow.remote.LogcatCaptureServiceImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

object LogcatServiceManager {

    private val serviceTag = UUID.randomUUID().toString()
    private val serviceVersion = AtomicInteger(100)

    private var currentServiceArgs: Shizuku.UserServiceArgs? = null

    private val _service = MutableStateFlow<ILogcatService?>(null)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Timber.i("LogcatService connected: %s", name)
            _service.value = ILogcatService.Stub.asInterface(binder)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Timber.i("LogcatService disconnected: %s", name)
            _service.value = null
        }
    }

    fun bind() {
        if (_service.value != null) return

        val args = Shizuku.UserServiceArgs(
            ComponentName(BuildConfig.APPLICATION_ID, LogcatCaptureServiceImpl::class.java.name)
        ).apply {
            processNameSuffix("logcat")
            daemon(false)
            tag(serviceTag)
            version(serviceVersion.incrementAndGet())
            debuggable(BuildConfig.DEBUG)
        }
        currentServiceArgs = args

        try {
            Shizuku.bindUserService(args, connection)
        } catch (e: Exception) {
            Timber.e(e, "bindLogcatService failed")
        }
    }

    suspend fun startCapture(appPid: Int, servicePid: Int, userDir: String) {
        withTimeout(10_000) {
            _service.first { it != null }
        }?.startCapture(appPid, servicePid, userDir)
    }
}
