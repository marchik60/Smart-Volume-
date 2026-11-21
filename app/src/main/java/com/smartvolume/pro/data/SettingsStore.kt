package com.smartvolume.pro.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppThemeMode(val title: String) {
    AIR_3D("Air 3D (Светлая)"),
    GRAPHITE("Graphite (Темная)"),
    DUDU_OEM("DUDU / OEM Minimal"),
    WARM_AMBER("Warm Amber (Ночь)"),
    EMERALD_SPORT("Emerald Sport")
}

enum class DayNightAutoMode(val title: String) {
    MANUAL("Вручную"),
    SYSTEM("По фарам / Системе"),
    SCHEDULE("По времени (07:00 / 20:00)")
}

enum class SpeedSourceMode(val title: String) {
    HYBRID("Гибрид (GPS + CAN)"),
    GPS_ONLY("Только GPS"),
    CAN_ONLY("Только CAN (FYT/DUDU)")
}

enum class ScreenOverrideMode(val title: String) {
    AUTO("Авто (по системе)"),
    FORCE_LANDSCAPE("Принудительно Горизонтальный"),
    FORCE_PORTRAIT("Принудительно Вертикальный (Tesla)")
}

object SettingsStore {
    var isEnabled by mutableStateOf(true)
    var baseVolume by mutableIntStateOf(12)
    var currentSpeedKmh by mutableFloatStateOf(0f)
    var currentVolumeBoost by mutableIntStateOf(0)
    var activeSpeedSource by mutableStateOf("ОЖИДАНИЕ")

    // 5 точек прибавки громкости для скоростей [0, 40, 80, 120, 150] км/ч
    val curvePoints = mutableStateListOf(0, 1, 3, 5, 7)
    var activePreset by mutableStateOf("Город")

    var smoothingSeconds by mutableIntStateOf(3)
    var safeStartEnabled by mutableStateOf(true)
    var safeStartVolume by mutableIntStateOf(10)
    var trafficLightMuteEnabled by mutableStateOf(true)

    var speedSourceMode by mutableStateOf(SpeedSourceMode.HYBRID)
    var speedCalibrationPercent by mutableIntStateOf(0) // -10% .. +10%

    var themeMode by mutableStateOf(AppThemeMode.DUDU_OEM)
    var dayNightAutoMode by mutableStateOf(DayNightAutoMode.SYSTEM)
    var screenOverrideMode by mutableStateOf(ScreenOverrideMode.AUTO)
    var uiScale by mutableFloatStateOf(1.0f)

    fun load(context: Context) {
        val prefs = context.getSharedPreferences("smart_volume_prefs", Context.MODE_PRIVATE)
        isEnabled = prefs.getBoolean("isEnabled", true)
        baseVolume = prefs.getInt("baseVolume", 12)
        smoothingSeconds = prefs.getInt("smoothingSeconds", 3)
        safeStartEnabled = prefs.getBoolean("safeStartEnabled", true)
        safeStartVolume = prefs.getInt("safeStartVolume", 10)
        trafficLightMuteEnabled = prefs.getBoolean("trafficLightMuteEnabled", true)
        speedCalibrationPercent = prefs.getInt("speedCalibrationPercent", 0)
        uiScale = prefs.getFloat("uiScale", 1.0f)

        val savedCurve = prefs.getString("curvePoints", "0,1,3,5,7") ?: "0,1,3,5,7"
        val parsed = savedCurve.split(",").mapNotNull { it.toIntOrNull() }
        if (parsed.size == 5) {
            for (i in 0..4) curvePoints[i] = parsed[i]
        }
        activePreset = prefs.getString("activePreset", "Город") ?: "Город"
        themeMode = AppThemeMode.entries.getOrElse(prefs.getInt("themeMode", 2)) { AppThemeMode.DUDU_OEM }
        dayNightAutoMode = DayNightAutoMode.entries.getOrElse(prefs.getInt("dayNightAutoMode", 1)) { DayNightAutoMode.SYSTEM }
        speedSourceMode = SpeedSourceMode.entries.getOrElse(prefs.getInt("speedSourceMode", 0)) { SpeedSourceMode.HYBRID }
        screenOverrideMode = ScreenOverrideMode.entries.getOrElse(prefs.getInt("screenOverrideMode", 0)) { ScreenOverrideMode.AUTO }
    }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences("smart_volume_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("isEnabled", isEnabled)
            .putInt("baseVolume", baseVolume)
            .putInt("smoothingSeconds", smoothingSeconds)
            .putBoolean("safeStartEnabled", safeStartEnabled)
            .putInt("safeStartVolume", safeStartVolume)
            .putBoolean("trafficLightMuteEnabled", trafficLightMuteEnabled)
            .putInt("speedCalibrationPercent", speedCalibrationPercent)
            .putFloat("uiScale", uiScale)
            .putString("curvePoints", curvePoints.joinToString(","))
            .putString("activePreset", activePreset)
            .putInt("themeMode", themeMode.ordinal)
            .putInt("dayNightAutoMode", dayNightAutoMode.ordinal)
            .putInt("speedSourceMode", speedSourceMode.ordinal)
            .putInt("screenOverrideMode", screenOverrideMode.ordinal)
            .apply()
    }

    fun applyPreset(name: String, context: Context) {
        activePreset = name
        val vals = when (name) {
            "Тихий" -> listOf(0, 1, 2, 3, 4)
            "Город" -> listOf(0, 1, 3, 5, 7)
            "Трасса" -> listOf(0, 2, 4, 7, 10)
            else -> curvePoints.toList()
        }
        for (i in 0..4) curvePoints[i] = vals[i]
        save(context)
    }

    fun calculateTargetBoost(speedKmh: Float): Int {
        val anchors = listOf(0f, 40f, 80f, 120f, 150f)
        if (speedKmh <= 0f) return curvePoints[0]
        if (speedKmh >= 150f) return curvePoints[4]
        for (i in 0 until 4) {
            if (speedKmh in anchors[i]..anchors[i + 1]) {
                val ratio = (speedKmh - anchors[i]) / (anchors[i + 1] - anchors[i])
                val interpolated = curvePoints[i] + ratio * (curvePoints[i + 1] - curvePoints[i])
                return kotlin.math.round(interpolated).toInt()
            }
        }
        return curvePoints[0]
    }
}
