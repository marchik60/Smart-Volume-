package com.smartvolume.pro.data

import android.content.Context

sealed class LicenseState {
    object BetaProFull : LicenseState()
    data class Trial(val daysLeft: Int) : LicenseState()
    object Free : LicenseState()
    object ProPermanent : LicenseState()
}

interface LicenseRepository {
    fun getLicenseState(context: Context): LicenseState
}

/**
 * Тестовая заглушка для дорожных испытаний без сервера.
 * Позже заменяется на RemoteLicenseRepository с проверкой подписи Ed25519.
 */
class LocalMockLicenseRepository : LicenseRepository {
    override fun getLicenseState(context: Context): LicenseState {
        return LicenseState.BetaProFull
    }
}
