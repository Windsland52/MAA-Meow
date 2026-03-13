package com.aliothmoon.maameow.remote

import com.aliothmoon.maameow.maa.MaaCoreLibrary
import com.aliothmoon.maameow.third.Ln
import com.sun.jna.Native

object MaaCoreManager {
    private const val TAG = "MaaCoreManager"

    val MaaContext: MaaCoreLibrary? by lazy {
        runCatching {
            System.setProperty("jna.tmpdir", "/data/local/tmp")
            Ln.i("$TAG: Loading MaaCore...")
            Native.load("MaaCore", MaaCoreLibrary::class.java).also {
                Ln.i("$TAG: MaaCore loaded successfully")
            }
        }.onFailure {
            Ln.e("$TAG: Failed to load MaaCore: ${it.message}")
            Ln.e(it.stackTraceToString())
        }.getOrNull()
    }

    val maaService: MaaCoreServiceImpl = MaaCoreServiceImpl(MaaContext)

    fun destroy() {
        Ln.i("$TAG: destroy()")
        maaService.DestroyInstance()
    }
}