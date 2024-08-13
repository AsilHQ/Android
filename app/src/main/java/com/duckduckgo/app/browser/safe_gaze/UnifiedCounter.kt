package com.duckduckgo.app.browser.safe_gaze

import android.content.SharedPreferences

class UnifiedCounter(
    private val preferences: SharedPreferences
) {
    private var dailyCount = 0
    private var sessionCount = 0
    private var allTimeCount = 0
    private var lastResetDate = 0L
    private val QUOTA_LIMIT = 60
    private var dirtyCount = 0

    init {
        // Initialize counters from SharedPreferences
        lastResetDate = preferences.getLong("SafeGazeLastResetDate", 0)
        dailyCount = preferences.getInt("SafeGazeAPICallsCount", 0)
        allTimeCount = preferences.getInt("all_time_censored_count", 0)
        sessionCount = preferences.getInt("session_censored_count", 0)
        checkAndResetQuota()
    }

    fun incrementDailyQuota(isPositiveDetection: Boolean) {
        if (isPositiveDetection) {
            dailyCount++
        }
        dirtyCount++

        // Save to preferences every 20 images or when quota is exceeded
        if (dirtyCount >= 20 || dailyCount == QUOTA_LIMIT) {
            saveToPreferences()
        }
    }

    fun incrementSessionAndAllTimeCount(isPositiveDetection: Boolean) {
        if (isPositiveDetection) {
            sessionCount++
            allTimeCount++
        }
        dirtyCount++

        // Save to preferences every 20 images
        if (dirtyCount >= 20) {
            saveToPreferences()
        }
    }

    fun checkAndResetQuota() {
        val currentDate = System.currentTimeMillis() / 86400000 // Current day since epoch
        if (currentDate > lastResetDate) {
            lastResetDate = currentDate
            dailyCount = 0
            sessionCount = 0
            saveToPreferences()
        }
    }

    fun isQuotaExceeded(): Boolean = dailyCount >= QUOTA_LIMIT

    fun getDailyCount(): Int = dailyCount
    fun getSessionCount(): Int = sessionCount
    fun getAllTimeCount(): Int = allTimeCount

    fun saveToPreferences() {
        preferences.edit()
            .putLong("SafeGazeLastResetDate", lastResetDate)
            .putInt("SafeGazeAPICallsCount", dailyCount)
            .putInt("all_time_censored_count", allTimeCount)
            .putInt("session_censored_count", sessionCount)
            .apply()
        dirtyCount = 0
    }

    fun resetSession() {
        sessionCount = 0
        saveToPreferences()
    }
}
