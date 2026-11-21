package com.smartvolume.pro.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import com.smartvolume.pro.data.AppThemeMode
import com.smartvolume.pro.data.DayNightAutoMode
import com.smartvolume.pro.data.SettingsStore
import java.util.Calendar

data class SmartPalette(
    val bgPrimary: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentSecondary: Color,
    val isDark: Boolean
)

@Composable
fun getActivePalette(): SmartPalette {
    val config = LocalConfiguration.current
    val systemDark = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    val resolvedMode = when (SettingsStore.dayNightAutoMode) {
        DayNightAutoMode.MANUAL -> SettingsStore.themeMode
        DayNightAutoMode.SYSTEM -> if (systemDark) {
            if (SettingsStore.themeMode == AppThemeMode.AIR_3D) AppThemeMode.DUDU_OEM else SettingsStore.themeMode
        } else {
            AppThemeMode.AIR_3D
        }
        DayNightAutoMode.SCHEDULE -> {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val isNight = hour >= 20 || hour < 7
            if (isNight) {
                if (SettingsStore.themeMode == AppThemeMode.AIR_3D) AppThemeMode.DUDU_OEM else SettingsStore.themeMode
            } else {
                AppThemeMode.AIR_3D
            }
        }
    }

    return when (resolvedMode) {
        AppThemeMode.AIR_3D -> SmartPalette(
            bgPrimary = Color(0xFFEEF2F7),
            cardBg = Color(0xFFFFFFFF),
            cardBorder = Color(0xFFD9E2EC),
            textPrimary = Color(0xFF102A43),
            textSecondary = Color(0xFF627D98),
            accent = Color(0xFF2563EB),
            accentSecondary = Color(0xFF10B981),
            isDark = false
        )
        AppThemeMode.GRAPHITE -> SmartPalette(
            bgPrimary = Color(0xFF111318),
            cardBg = Color(0xFF1B1F27),
            cardBorder = Color(0xFF2C3340),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFF94A3B8),
            accent = Color(0xFF3B82F6),
            accentSecondary = Color(0xFF22C55E),
            isDark = true
        )
        AppThemeMode.DUDU_OEM -> SmartPalette(
            bgPrimary = Color(0xFF0B1120),
            cardBg = Color(0xFF151E32),
            cardBorder = Color(0xFF243352),
            textPrimary = Color(0xFFF1F5F9),
            textSecondary = Color(0xFF94A3B8),
            accent = Color(0xFF38BDF8),
            accentSecondary = Color(0xFF34D399),
            isDark = true
        )
        AppThemeMode.WARM_AMBER -> SmartPalette(
            bgPrimary = Color(0xFF140D08),
            cardBg = Color(0xFF21160E),
            cardBorder = Color(0xFF3D2818),
            textPrimary = Color(0xFFFEF3C7),
            textSecondary = Color(0xFFD97706),
            accent = Color(0xFFF59E0B),
            accentSecondary = Color(0xFFFB923C),
            isDark = true
        )
        AppThemeMode.EMERALD_SPORT -> SmartPalette(
            bgPrimary = Color(0xFF071510),
            cardBg = Color(0xFF0E241C),
            cardBorder = Color(0xFF1B4335),
            textPrimary = Color(0xFFECFDF5),
            textSecondary = Color(0xFF6EE7B7),
            accent = Color(0xFF10B981),
            accentSecondary = Color(0xFF34D399),
            isDark = true
        )
    }
}
