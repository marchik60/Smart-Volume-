package com.smartvolume.pro.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.smartvolume.pro.data.AppLog
import com.smartvolume.pro.data.HardwareInfo
import com.smartvolume.pro.data.SettingsStore
import kotlin.math.abs

class SmartVolumeService : Service(), LocationListener {

    private lateinit var audioManager: AudioManager
    private lateinit var locationManager: LocationManager
    private val handler = Handler(Looper.getMainLooper())

    private var lastGpsTime = 0L
    private var lastGpsSpeed = 0f
    private var lastCanTime = 0L
    private var lastCanSpeed = 0f

    private var lastAppliedBoost = 0
    private var speedStableSince = 0L
    private var pendingTargetBoost = 0
    private var stoppedSince = 0L
    private var isTrafficMuted = false

    // Слушатель системных CAN-сообщений для платформ FYT / Teyes / DUDU / Topway
    private val canReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val speedRaw = when {
                intent.hasExtra("speed") -> intent.getFloatExtra("speed", -1f).takeIf { it >= 0 }
                    ?: intent.getIntExtra("speed", -1).toFloat()
                intent.hasExtra("car_speed") -> intent.getIntExtra("car_speed", -1).toFloat()
                intent.hasExtra("value") -> intent.getIntExtra("value", -1).toFloat()
                else -> -1f
            }
            if (speedRaw in 0f..320f) {
                lastCanSpeed = speedRaw
                lastCanTime = System.currentTimeMillis()
            } else {
                AppLog.w("CAN", "Получен пакет $action без распознанного поля скорости")
            }
        }
    }

    private val controlLoop = object : Runnable {
        override fun run() {
            updateSpeedAndVolume()
            val delay = if (HardwareInfo.isLowEndCpu()) 1000L else 500L
            handler.postDelayed(this, delay)
        }
    }

    override fun onCreate() {
        super.onCreate()
        SettingsStore.load(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        startForegroundNotification()
        registerCanReceivers()
        startGpsUpdates()

        if (SettingsStore.safeStartEnabled) {
            applySafeStart()
        } else {
            SettingsStore.baseVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        }

        AppLog.i("SERVICE", "Фоновый сервис запущен. ${HardwareInfo.getPlatformSummary(this)}")
        handler.post(controlLoop)
    }

    private fun applySafeStart() {
        try {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val safeTarget = SettingsStore.safeStartVolume.coerceIn(1, maxVol)
            if (current > safeTarget) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, safeTarget, 0)
                SettingsStore.baseVolume = safeTarget
                AppLog.i("AUDIO", "Безопасный старт: громкость снижена с $current до $safeTarget")
            } else {
                SettingsStore.baseVolume = current
            }
        } catch (e: Exception) {
            AppLog.e("AUDIO", "Ошибка безопасного старта", e)
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerCanReceivers() {
        try {
            val filter = IntentFilter().apply {
                addAction("com.syu.ms.canbus.speed")
                addAction("com.dudu.canbus.SPEED_CHANGED")
                addAction("com.ts.can.SPEED_INFO")
                addAction("android.intent.action.CAR_SPEED")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(canReceiver, filter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(canReceiver, filter)
            }
            AppLog.i("CAN", "Подписка на системные CAN-интенты активна")
        } catch (e: Exception) {
            AppLog.e("CAN", "Не удалось зарегистрировать CAN-ресивер", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startGpsUpdates() {
        try {
            val minTime = if (HardwareInfo.isLowEndCpu()) 1000L else 400L
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, 0f, this)
            AppLog.i("GPS", "Запрос обновлений GPS запущен (интервал ${minTime}мс)")
        } catch (e: Exception) {
            AppLog.e("GPS", "Нет доступа к GPS-провайдеру", e)
        }
    }

    override fun onLocationChanged(location: Location) {
        if (location.hasSpeed() && location.accuracy <= 25f) {
            lastGpsSpeed = location.speed * 3.6f
            lastGpsTime = System.currentTimeMillis()
        }
    }

    private fun updateSpeedAndVolume() {
        val now = System.currentTimeMillis()
        val gpsValid = (now - lastGpsTime) < 2500L
        val canValid = (now - lastCanTime) < 2500L

        val rawSpeed = when (SettingsStore.speedSourceMode) {
            com.smartvolume.pro.data.SpeedSourceMode.GPS_ONLY -> {
                SettingsStore.activeSpeedSource = if (gpsValid) "GPS" else "НЕТ GPS"
                if (gpsValid) lastGpsSpeed else 0f
            }
            com.smartvolume.pro.data.SpeedSourceMode.CAN_ONLY -> {
                SettingsStore.activeSpeedSource = if (canValid) "CAN" else "НЕТ CAN"
                if (canValid) lastCanSpeed else 0f
            }
            com.smartvolume.pro.data.SpeedSourceMode.HYBRID -> {
                when {
                    gpsValid -> {
                        SettingsStore.activeSpeedSource = "GPS"
                        lastGpsSpeed
                    }
                    canValid -> {
                        SettingsStore.activeSpeedSource = "CAN"
                        lastCanSpeed
                    }
                    else -> {
                        SettingsStore.activeSpeedSource = "ПОИСК"
                        0f
                    }
                }
            }
        }

        val calibratedSpeed = (rawSpeed * (1f + SettingsStore.speedCalibrationPercent / 100f)).coerceAtLeast(0f)
        SettingsStore.currentSpeedKmh = calibratedSpeed

        if (!SettingsStore.isEnabled) {
            SettingsStore.currentVolumeBoost = 0
            return
        }

        // Отслеживание ручного изменения громкости водителем
        val systemCurrentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val expectedSystemVol = (SettingsStore.baseVolume + lastAppliedBoost - if (isTrafficMuted) 2 else 0).coerceAtLeast(0)
        if (abs(systemCurrentVol - expectedSystemVol) >= 1) {
            SettingsStore.baseVolume = (systemCurrentVol - lastAppliedBoost).coerceAtLeast(1)
        }

        // Логика «Умного светофора»
        if (SettingsStore.trafficLightMuteEnabled && calibratedSpeed < 2f && SettingsStore.activeSpeedSource != "ПОИСК") {
            if (stoppedSince == 0L) stoppedSince = now
            if (!isTrafficMuted && (now - stoppedSince) > 5000L) {
                isTrafficMuted = true
                applySystemVolume(SettingsStore.baseVolume - 2)
                AppLog.i("AUDIO", "Умный светофор: приглушение на остановке")
            }
        } else {
            stoppedSince = 0L
            if (isTrafficMuted) {
                isTrafficMuted = false
                applySystemVolume(SettingsStore.baseVolume + lastAppliedBoost)
            }
        }

        // Гистерезис и плавный шаг кривой
        val targetBoost = SettingsStore.calculateTargetBoost(calibratedSpeed)
        if (targetBoost != pendingTargetBoost) {
            pendingTargetBoost = targetBoost
            speedStableSince = now
        }

        val holdMillis = SettingsStore.smoothingSeconds * 1000L
        if (pendingTargetBoost != lastAppliedBoost && (now - speedStableSince) >= holdMillis) {
            val step = if (pendingTargetBoost > lastAppliedBoost) 1 else -1
            lastAppliedBoost += step
            SettingsStore.currentVolumeBoost = lastAppliedBoost
            if (!isTrafficMuted) {
                applySystemVolume(SettingsStore.baseVolume + lastAppliedBoost)
            }
            speedStableSince = now
        }
    }

    private fun applySystemVolume(targetVol: Int) {
        try {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val clamped = targetVol.coerceIn(0, maxVol)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0)
        } catch (e: Exception) {
            AppLog.e("AUDIO", "Сбой установки системной громкости", e)
        }
    }

    private fun startForegroundNotification() {
        val channelId = "smart_volume_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Smart-Volume Service", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Smart-Volume Pro активен")
            .setContentText("Автокомпенсация громкости по скорости")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setOngoing(true)
            .build()
        startForeground(1001, notification)
    }

    override fun onDestroy() {
        handler.removeCallbacks(controlLoop)
        try {
            unregisterReceiver(canReceiver)
            locationManager.removeUpdates(this)
        } catch (_: Exception) { }
        AppLog.w("SERVICE", "Фоновый сервис остановлен")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
