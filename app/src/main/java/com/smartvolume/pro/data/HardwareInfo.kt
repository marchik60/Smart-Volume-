package com.smartvolume.pro.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest

object HardwareInfo {
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        val board = Build.BOARD ?: ""
        val hardware = Build.HARDWARE ?: ""
        val serial = getSystemProperty("ro.serialno").ifEmpty { getSystemProperty("ro.boot.serialno") }
        val platform = getSystemProperty("ro.board.platform")

        val raw = "$androidId|$board|$hardware|$serial|$platform"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        val hex = digest.joinToString("") { "%02X".format(it) }
        return "GU-${hex.substring(0, 4)}-${hex.substring(4, 8)}"
    }

    fun getPlatformSummary(context: Context): String {
        val platform = getSystemProperty("ro.board.platform").ifEmpty { Build.HARDWARE }
        val dm = context.resources.displayMetrics
        val isFyt = isFytPlatform()
        return "CPU/Platform: $platform (FYT/Teyes=$isFyt) | Android ${Build.VERSION.RELEASE} | Screen: ${dm.widthPixels}x${dm.heightPixels} (${dm.densityDpi}dpi)"
    }

    fun isFytPlatform(): Boolean {
        val platform = getSystemProperty("ro.board.platform").lowercase()
        val fytVer = getSystemProperty("ro.build.fytmanufacturer")
        return platform.contains("ums512") || platform.contains("uis7862") || platform.contains("uis8581") || fytVer.isNotEmpty()
    }

    fun isLowEndCpu(): Boolean {
        val cores = Runtime.getRuntime().availableProcessors()
        val platform = getSystemProperty("ro.board.platform").lowercase()
        return cores <= 4 || platform.contains("8227") || platform.contains("t3")
    }

    @SuppressLint("PrivateApi")
    private fun getSystemProperty(key: String): String {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val get = clazz.getMethod("get", String::class.java)
            (get.invoke(null, key) as? String)?.trim() ?: ""
        } catch (_: Exception) {
            ""
        }
    }
}
