package com.aliothmoon.maameow.remote.internal

import android.graphics.Bitmap
import android.graphics.Rect
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.view.Surface
import com.aliothmoon.maameow.bridge.NativeBridgeLib
import com.aliothmoon.maameow.third.Ln

object FrameCaptureHelper {

    fun processImage(reader: ImageReader) {
        val image = reader.acquireLatestImage() ?: return
        try {
            val hb = image.hardwareBuffer ?: run {
                Ln.w("processImage: hardwareBuffer is null")
                return
            }
            try {
                NativeBridgeLib.copyFrameFromHardwareBuffer(hb)
            } catch (e: Exception) {
                Ln.w("processImage copyFrame failed: ${e.message}")
            } finally {
                hb.close()
            }

        } catch (e: IllegalStateException) {
            Ln.w("processImage failed: ${e.message}")
        } finally {
            image.close()
        }
    }

    fun createCaptureHandler(name: String): Handler {
        val thread = object : HandlerThread(name, Process.THREAD_PRIORITY_URGENT_DISPLAY) {
            override fun onLooperPrepared() {
                // Double check and ensure the priority is set correctly
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY)
            }
        }
        thread.start()
        return Handler(thread.looper)
    }
}
