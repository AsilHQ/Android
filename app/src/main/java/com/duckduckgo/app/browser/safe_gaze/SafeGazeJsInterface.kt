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
import com.duckduckgo.app.safegaze.genderdetection.GenderDetector
import com.duckduckgo.app.safegaze.nsfwdetection.NsfwDetector
import com.duckduckgo.common.utils.SAFE_GAZE_PREFERENCES
import timber.log.Timber

class SafeGazeJsInterface(
    private val context: Context,
    private val webView: DuckDuckGoWebView? = null
) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(SAFE_GAZE_PREFERENCES, Context.MODE_PRIVATE)

    private val nsfwDetector = NsfwDetector(context)
    private val genderDetector = GenderDetector(context)
    var imageCount = 0

    private fun shouldBlurImage(url: String, shouldBlur: (Boolean) -> Unit) {
        loadImageBitmapFromUrl(url, context) { bitmap ->
            if (bitmap != null) {
                val a = System.currentTimeMillis()
                val nsfwPrediction = nsfwDetector.isNsfw(bitmap)
                val b = System.currentTimeMillis()

                Timber.d("kLog Contains nsfw: ${nsfwPrediction.isSafe().not()}. Processing time: ${b-a}")

                if (nsfwPrediction.isSafe()) {
                    genderDetector.predict(bitmap) { prediction ->
                        val c = System.currentTimeMillis()
                        Timber.d("kLog Contains female: ${prediction.hasFemale}. Processing time: ${c-b}")

                        shouldBlur(prediction.hasFemale)
                    }
                } else {
                    shouldBlur(true)
                }
            } else {
                shouldBlur(false)
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
        if (message.startsWith("coreML/-/")) {
            val parts = message.split("/-/")
            val imageUrl = if (parts.size >= 2) parts[1] else ""
            val index = if (parts.size >= 2) parts[2] else ""

            synchronized(this) {
                shouldBlurImage(imageUrl) {
                    callSafegazeOnDeviceModelHandler(it, index.toInt())
                }
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

    fun closeMlModels() {
        nsfwDetector.dispose()
        genderDetector.dispose()
    }
}
