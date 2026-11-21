package com.smartvolume.pro.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.smartvolume.pro.data.*
import kotlin.math.roundToInt

@Composable
fun MainScreen() {
    val palette = getActivePalette()
    val context = LocalContext.current
    val baseDensity = LocalDensity.current
    val scaledDensity = Density(
        density = baseDensity.density * SettingsStore.uiScale,
        fontScale = baseDensity.fontScale * SettingsStore.uiScale
    )

    var showSettings by remember { mutableStateOf(false) }
    var initialSettingsTab by remember { mutableIntStateOf(0) }
    var showLogDialog by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        val config = LocalConfiguration.current
        val isPortrait = when (SettingsStore.screenOverrideMode) {
            ScreenOverrideMode.AUTO -> config.screenHeightDp > config.screenWidthDp
            ScreenOverrideMode.FORCE_LANDSCAPE -> false
            ScreenOverrideMode.FORCE_PORTRAIT -> true
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bgPrimary)
                .padding(16.dp)
        ) {
            // Верхняя панель
            TopHeaderBar(
                palette = palette,
                onOpenPro = {
                    initialSettingsTab = 3
                    showSettings = true
                },
                onOpenLog = { showLogDialog = true },
                onOpenSettings = {
                    initialSettingsTab = 0
                    showSettings = true
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Чистый водительский дашборд
            if (isPortrait) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SpeedometerCard(palette, Modifier.weight(1.2f).fillMaxWidth())
                    VolumeBoostCard(palette, Modifier.weight(1f).fillMaxWidth())
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SpeedometerCard(palette, Modifier.weight(1.15f).fillMaxHeight())
                    VolumeBoostCard(palette, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }

        if (showSettings) {
            SettingsDialog(
                palette = palette,
                initialTab = initialSettingsTab,
                onDismiss = { showSettings = false }
            )
        }

        if (showLogDialog) {
            DiagnosticLogDialog(
                palette = palette,
                onDismiss = { showLogDialog = false }
            )
        }
    }
}

@Composable
private fun TopHeaderBar(
    palette: SmartPalette,
    onOpenPro: () -> Unit,
    onOpenLog: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .shadow(6.dp, RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(listOf(palette.accent, palette.accentSecondary)),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Smart Volume",
                color = palette.textPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Кнопка статуса лицензии
            Surface(
                modifier = Modifier.clickable { onOpenPro() },
                shape = RoundedCornerShape(20.dp),
                color = palette.accent.copy(alpha = 0.16f),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.accent.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "PRO BETA",
                    color = palette.accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Кнопка лога
            val hasErr = AppLog.hasErrors()
            Surface(
                modifier = Modifier.clickable { onOpenLog() },
                shape = RoundedCornerShape(12.dp),
                color = if (hasErr) Color(0xFFEF4444).copy(alpha = 0.2f) else palette.cardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (hasErr) Color(0xFFEF4444) else palette.cardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = "Лог",
                        tint = if (hasErr) Color(0xFFEF4444) else palette.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Лог", color = palette.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Кнопка настроек
            Surface(
                modifier = Modifier.clickable { onOpenSettings() },
                shape = RoundedCornerShape(12.dp),
                color = palette.cardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorder)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Настройки",
                    tint = palette.textPrimary,
                    modifier = Modifier.padding(8.dp).size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SpeedometerCard(palette: SmartPalette, modifier: Modifier) {
    val speed = SettingsStore.currentSpeedKmh.roundToInt()
    val source = SettingsStore.activeSpeedSource
    val sourceColor = when (source) {
        "GPS" -> Color(0xFF10B981)
        "CAN" -> Color(0xFF38BDF8)
        else -> Color(0xFFF59E0B)
    }

    Box(
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(24.dp))
            .background(palette.cardBg, RoundedCornerShape(24.dp))
            .border(1.dp, palette.cardBorder, RoundedCornerShape(24.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Индикатор источника GPS / CAN
        Row(
            modifier = Modifier.align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(sourceColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = source,
                color = sourceColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$speed",
                color = palette.textPrimary,
                fontSize = 84.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 84.sp
            )
            Text(
                text = "км/ч",
                color = palette.textSecondary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun VolumeBoostCard(palette: SmartPalette, modifier: Modifier) {
    val context = LocalContext.current
    val base = SettingsStore.baseVolume
    val boost = SettingsStore.currentVolumeBoost

    Column(
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(24.dp))
            .background(palette.cardBg, RoundedCornerShape(24.dp))
            .border(1.dp, palette.cardBorder, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Автогромкость", color = palette.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Пресет: ${SettingsStore.activePreset}", color = palette.textSecondary, fontSize = 13.sp)
            }
            Switch(
                checked = SettingsStore.isEnabled,
                onCheckedChange = {
                    SettingsStore.isEnabled = it
                    SettingsStore.save(context)
                }
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (SettingsStore.isEnabled) "+$boost" else "ВЫКЛ",
                color = if (boost > 0 && SettingsStore.isEnabled) palette.accentSecondary else palette.textPrimary,
                fontSize = 54.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "База: $base  →  Итого: ${base + if (SettingsStore.isEnabled) boost else 0}",
                color = palette.textSecondary,
                fontSize = 14.sp
            )
        }

        // Шкала прогресса прибавки
        LinearProgressIndicator(
            progress = { (boost / 12f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = palette.accent,
            trackColor = palette.bgPrimary
        )
    }
}

@Composable
private fun SettingsDialog(
    palette: SmartPalette,
    initialTab: Int,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabs = listOf("Кривая", "Источники", "Экран и темы", "Pro и ID")
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .background(palette.bgPrimary, RoundedCornerShape(24.dp))
                .border(1.dp, palette.cardBorder, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Настройки", color = palette.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = palette.textPrimary)
                }
            }

            // Вкладки (сетка 2x2 на узких экранах, 1 ряд на широких)
            val isNarrow = LocalConfiguration.current.screenWidthDp < 600
            if (isNarrow) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TabChip(tabs[0], selectedTab == 0, palette, Modifier.weight(1f)) { selectedTab = 0 }
                        TabChip(tabs[1], selectedTab == 1, palette, Modifier.weight(1f)) { selectedTab = 1 }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TabChip(tabs[2], selectedTab == 2, palette, Modifier.weight(1f)) { selectedTab = 2 }
                        TabChip(tabs[3], selectedTab == 3, palette, Modifier.weight(1f)) { selectedTab = 3 }
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tabs.forEachIndexed { index, title ->
                        TabChip(title, selectedTab == index, palette, Modifier.weight(1f)) { selectedTab = index }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> CurveSettingsTab(palette, context)
                    1 -> SourcesSettingsTab(palette, context)
                    2 -> DisplayThemeSettingsTab(palette, context)
                    3 -> ProLicenseTab(palette, context)
                }
            }
        }
    }
}

@Composable
private fun TabChip(title: String, selected: Boolean, palette: SmartPalette, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) palette.accent else palette.cardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) palette.accent else palette.cardBorder)
    ) {
        Text(
            text = title,
            color = if (selected) Color.White else palette.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
        )
    }
}

@Composable
private fun CurveSettingsTab(palette: SmartPalette, context: Context) {
    // Пресеты
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Тихий", "Город", "Трасса").forEach { preset ->
            val active = SettingsStore.activePreset == preset
            Surface(
                modifier = Modifier.weight(1f).clickable { SettingsStore.applyPreset(preset, context) },
                shape = RoundedCornerShape(12.dp),
                color = if (active) palette.accent else palette.cardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorder)
            ) {
                Text(
                    preset,
                    color = if (active) Color.White else palette.textPrimary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }

    // Интерактивный график кривой
    val speeds = listOf("0", "40", "80", "120", "150")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .background(palette.cardBg, RoundedCornerShape(16.dp))
            .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val stepX = size.width / 4f
                    val idx = (change.position.x / stepX).roundToInt().coerceIn(0, 4)
                    val normY = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    val newBoost = (normY * 12f).roundToInt().coerceIn(0, 12)
                    SettingsStore.curvePoints[idx] = newBoost
                    SettingsStore.activePreset = "Свой"
                    SettingsStore.save(context)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val stepX = w / 4f
            val points = (0..4).map { i ->
                val y = h - (SettingsStore.curvePoints[i] / 12f) * h
                Offset(i * stepX, y)
            }

            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1..4) lineTo(points[i].x, points[i].y)
            }
            drawPath(path, color = palette.accent, style = Stroke(width = 6f, cap = StrokeCap.Round))

            points.forEach { pt ->
                drawCircle(color = palette.cardBg, radius = 16f, center = pt)
                drawCircle(color = palette.accentSecondary, radius = 11f, center = pt)
            }
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        speeds.forEachIndexed { idx, s ->
            Text("$s км/ч (+${SettingsStore.curvePoints[idx]})", color = palette.textSecondary, fontSize = 11.sp)
        }
    }

    // Плавность и Безопасный старт
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.cardBg, RoundedCornerShape(16.dp))
            .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text("Плавность реакции: ${SettingsStore.smoothingSeconds} сек.", color = palette.textPrimary, fontSize = 14.sp)
        Slider(
            value = SettingsStore.smoothingSeconds.toFloat(),
            onValueChange = {
                SettingsStore.smoothingSeconds = it.roundToInt()
                SettingsStore.save(context)
            },
            valueRange = 1f..8f,
            steps = 6
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Безопасный старт ГУ (до ${SettingsStore.safeStartVolume})", color = palette.textPrimary, fontSize = 14.sp)
            Switch(
                checked = SettingsStore.safeStartEnabled,
                onCheckedChange = {
                    SettingsStore.safeStartEnabled = it
                    SettingsStore.save(context)
                }
            )
        }
    }
}

@Composable
private fun SourcesSettingsTab(palette: SmartPalette, context: Context) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.cardBg, RoundedCornerShape(16.dp))
            .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Источник скорости", color = palette.textPrimary, fontWeight = FontWeight.Bold)
        SpeedSourceMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        SettingsStore.speedSourceMode = mode
                        SettingsStore.save(context)
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = SettingsStore.speedSourceMode == mode, onClick = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(mode.title, color = palette.textPrimary, fontSize = 14.sp)
            }
        }

        HorizontalDivider(color = palette.cardBorder)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Умный светофор (тише на 0 км/ч)", color = palette.textPrimary, fontSize = 14.sp)
            Switch(
                checked = SettingsStore.trafficLightMuteEnabled,
                onCheckedChange = {
                    SettingsStore.trafficLightMuteEnabled = it
                    SettingsStore.save(context)
                }
            )
        }

        Text("Поправка спидометра: ${SettingsStore.speedCalibrationPercent}%", color = palette.textPrimary, fontSize = 14.sp)
        Slider(
            value = SettingsStore.speedCalibrationPercent.toFloat(),
            onValueChange = {
                SettingsStore.speedCalibrationPercent = it.roundToInt()
                SettingsStore.save(context)
            },
            valueRange = -10f..10f
        )
    }
}

@Composable
private fun DisplayThemeSettingsTab(palette: SmartPalette, context: Context) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.cardBg, RoundedCornerShape(16.dp))
            .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Автосмена День / Ночь", color = palette.textPrimary, fontWeight = FontWeight.Bold)
        DayNightAutoMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        SettingsStore.dayNightAutoMode = mode
                        SettingsStore.save(context)
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = SettingsStore.dayNightAutoMode == mode, onClick = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(mode.title, color = palette.textPrimary, fontSize = 14.sp)
            }
        }

        HorizontalDivider(color = palette.cardBorder)

        Text("Тема оформления", color = palette.textPrimary, fontWeight = FontWeight.Bold)
        AppThemeMode.entries.forEach { theme ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        SettingsStore.themeMode = theme
                        SettingsStore.save(context)
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = SettingsStore.themeMode == theme, onClick = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(theme.title, color = palette.textPrimary, fontSize = 14.sp)
            }
        }

        HorizontalDivider(color = palette.cardBorder)

        Text("Адаптация нестандартного экрана", color = palette.textPrimary, fontWeight = FontWeight.Bold)
        ScreenOverrideMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        SettingsStore.screenOverrideMode = mode
                        SettingsStore.save(context)
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = SettingsStore.screenOverrideMode == mode, onClick = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(mode.title, color = palette.textPrimary, fontSize = 14.sp)
            }
        }

        Text("Масштаб интерфейса: ${(SettingsStore.uiScale * 100).roundToInt()}%", color = palette.textPrimary, fontSize = 14.sp)
        Slider(
            value = SettingsStore.uiScale,
            onValueChange = {
                SettingsStore.uiScale = it
                SettingsStore.save(context)
            },
            valueRange = 0.8f..1.3f
        )
    }
}

@Composable
private fun ProLicenseTab(palette: SmartPalette, context: Context) {
    val deviceId = remember { HardwareInfo.getDeviceId(context) }
    val platformSummary = remember { HardwareInfo.getPlatformSummary(context) }

    // Вертикальная стековая карточка (исключает выезд текста за края на любых экранах)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.cardBg, RoundedCornerShape(16.dp))
            .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.accentSecondary.copy(alpha = 0.18f)
        ) {
            Text(
                text = "PRO ТЕСТОВЫЙ РЕЖИМ АКТИВЕН",
                color = palette.accentSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Text(
            text = "ID вашей магнитолы:",
            color = palette.textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.bgPrimary, RoundedCornerShape(12.dp))
                .padding(vertical = 12.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = deviceId,
                color = palette.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                softWrap = false
            )
        }

        Button(
            onClick = {
                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText("Device ID", deviceId))
                Toast.makeText(context, "ID скопирован: $deviceId", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Скопировать ID магнитолы")
        }

        Text(
            text = platformSummary,
            color = palette.textSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DiagnosticLogDialog(palette: SmartPalette, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val hwInfo = remember { HardwareInfo.getPlatformSummary(context) + " | ID: " + HardwareInfo.getDeviceId(context) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .background(palette.bgPrimary, RoundedCornerShape(24.dp))
                .border(1.dp, palette.cardBorder, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Диагностический лог", color = palette.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = palette.textPrimary)
                }
            }

            Text(hwInfo, color = palette.textSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(palette.cardBg, RoundedCornerShape(12.dp))
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppLog.entries.forEach { entry ->
                    val color = when (entry.level) {
                        "ERR" -> Color(0xFFEF4444)
                        "WARN" -> Color(0xFFF59E0B)
                        else -> palette.textPrimary
                    }
                    Text(
                        text = "[${entry.timestamp}] ${entry.tag}: ${entry.message}",
                        color = color,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cb.setPrimaryClip(ClipData.newPlainText("SmartVolumeLog", AppLog.exportText(hwInfo)))
                    Toast.makeText(context, "Отчет скопирован в буфер", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Скопировать весь лог для отладки")
            }
        }
    }
}
