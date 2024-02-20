package com.duckduckgo.app.browser.safe_gaze

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.webkit.JavascriptInterface
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.duckduckgo.app.browser.DuckDuckGoWebView
import com.duckduckgo.app.safegaze.ondeviceobjectdetection.ObjectDetectionHelper

class SafeGazeJsInterface(
    private val context: Context,
    private val webView: DuckDuckGoWebView? = null
) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences("safe_gaze_preferences", Context.MODE_PRIVATE)

    private val objectDetectionHelper = ObjectDetectionHelper(context)

    private fun isImageContainsHumanFromWebView(url: String, callback: (Boolean) -> Unit) {
        loadImageBitmapFromUrl(url, context) { bitmap ->
            if (bitmap != null) {
                val containsHuman = objectDetectionHelper.isImageContainsHuman(bitmap)
                callback(containsHuman)
            } else {
                callback(false)
            }
        }
    }

    private fun loadImageBitmapFromUrl(url: String, context: Context, listener: (Bitmap?) -> Unit) {
        try {
            Glide.with(context)
                .asBitmap()
                .load(url)
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        listener(resource)
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
            listener(null)
        }
    }

    @JavascriptInterface
    fun callSafegazeOnDeviceModelHandler(isExist: Boolean, index: Int) {
        val jsFunctionCall = "safegazeOnDeviceModelHandler($isExist, $index);"
        webView?.post {
            webView.evaluateJavascript(jsFunctionCall, null)
        }
    }

    @JavascriptInterface
    fun updateBlur(blur: Float){
        val trimmedBlur = blur / 100
        val jsFunction = "window.blurIntensity = $trimmedBlur; updateBluredImageOpacity();"
        webView?.post {
            webView.evaluateJavascript(jsFunction, null)
        }
    }

    @JavascriptInterface
    fun sendMessage(message: String) {
        updateBlur(preferences.getInt("safe_gaze_blur_progress", 0).toFloat())
        if (message.startsWith("coreML/-/")){
            val parts = message.split("/-/")
            val imageUrl = if (parts.size >= 2) parts[1] else ""
            val index = if (parts.size >= 2) parts[2] else ""
            isImageContainsHumanFromWebView(imageUrl){
                callSafegazeOnDeviceModelHandler(it, index.toInt())
            }
        }
        if (message.contains("page_refresh")) {
            preferences.edit().putInt("session_censored_count", 0).apply()
        } else if(message.contains("replaced")) {
            handleAllTimeCounter()
            handleCurrentSessionCounter()
        }
    }

    private fun handleAllTimeCounter() {
        val currentAllTimeCounter = getAllTimeCounter()
        val newAllTimeCounter = currentAllTimeCounter + 1
        saveAllTimeCounterValue(newAllTimeCounter)
    }

    private fun handleCurrentSessionCounter() {
        val currentSessionCounter = getCurrentSessionCounter()
        val newSessionCounter = currentSessionCounter + 1
        saveSessionCounterValue(newSessionCounter)
    }

    private fun saveAllTimeCounterValue(value: Int) {
        preferences.edit().putInt("all_time_censored_count", value).apply()
    }

    private fun getAllTimeCounter(): Int {
        return preferences.getInt("all_time_censored_count", 0)
    }

    private fun saveSessionCounterValue(value: Int) {
        preferences.edit().putInt("session_censored_count", value).apply()
    }

    private fun getCurrentSessionCounter(): Int {
        return preferences.getInt("session_censored_count", 0)
    }
}
