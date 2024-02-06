package com.duckduckgo.app.browser.safe_gaze

import android.content.Context
import android.content.SharedPreferences
import android.webkit.JavascriptInterface

class SafeGazeJsInterface(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences("safe_gaze_preferences", Context.MODE_PRIVATE)

    @JavascriptInterface
    fun sendMessage(message: String) {
        if (message.contains("page_refresh")){
            preferences.edit().putInt("session_cencored_count", 0).apply()
        }else{
            handleAllTimeCounter()
            handleCurrentSessionCounter()
        }
    }

    private fun handleAllTimeCounter(){
        val currentAllTimeCounter = getAllTimeCounter()
        val newAllTimeCounter = currentAllTimeCounter + 1
        saveAllTimeCounterValue(newAllTimeCounter)
    }

    private fun handleCurrentSessionCounter(){
        val currentSessionCounter = getCurrentSessionCounter()
        val newSessionCounter = currentSessionCounter + 1
        saveSessionCounterValue(newSessionCounter)
    }

    private fun saveAllTimeCounterValue(value: Int) {
        preferences.edit().putInt("all_time_cencored_count", value).apply()
    }

    private fun getAllTimeCounter(): Int {
        return preferences.getInt("all_time_cencored_count", 0)
    }

    private fun saveSessionCounterValue(value: Int) {
        preferences.edit().putInt("session_cencored_count", value).apply()
    }

    private fun getCurrentSessionCounter(): Int {
        return preferences.getInt("session_cencored_count", 0)
    }
}