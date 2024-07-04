package com.duckduckgo.app.browser.safe_gaze

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.webkit.JavascriptInterface
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.duckduckgo.app.browser.DuckDuckGoWebView
import com.duckduckgo.app.safegaze.genderdetection.GenderDetector
import com.duckduckgo.app.safegaze.nsfwdetection.NsfwDetector
import com.duckduckgo.common.utils.DefaultDispatcherProvider
import com.duckduckgo.common.utils.SAFE_GAZE_PREFERENCES
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal data class UrlInfo(val url: String, val index: Int)

class SafeGazeJsInterface(
    private val context: Context,
    private val webView: DuckDuckGoWebView? = null,

) {
    private val dispatcher: DefaultDispatcherProvider = DefaultDispatcherProvider()
    private val preferences: SharedPreferences = context.getSharedPreferences(SAFE_GAZE_PREFERENCES, Context.MODE_PRIVATE)

    private val nsfwDetector = NsfwDetector(context)
    private val genderDetector = GenderDetector(context)
    private val alreadyProcessedUrls = mutableMapOf<String, Boolean>()

    private val urlQueue: ConcurrentLinkedQueue<UrlInfo> = ConcurrentLinkedQueue()
    private var processingJob: Job? = null
    private val scope = CoroutineScope(dispatcher.computation() + Job())

    private suspend fun shouldBlurImage(url: String, mScope: CoroutineScope): Boolean {
        return suspendCoroutine { continuation ->
            mScope.launch {
                val bitmap = loadImageBitmapFromUrl(url, context)

                if (bitmap != null) {
                    val nsfwPrediction = nsfwDetector.isNsfw(bitmap)

                    if (nsfwPrediction.isSafe()) {
                        val genderPrediction = genderDetector.predict(bitmap)

                        if (genderPrediction.hasFemale)
                            Timber.d("kLog Female (${genderPrediction.femaleConfidence}) $url")

                        continuation.resume(genderPrediction.hasFemale)
                    } else {
                        nsfwPrediction.getLabelWithConfidence().let {
                            Timber.d("kLog Nsfw: ${it.first} (${it.second}) $url")
                            continuation.resume(true)
                        }
                    }
                } else {
                    continuation.resume(false)
                }
            }
        } 
    }


    private suspend fun loadImageBitmapFromUrl(url: String, context: Context): Bitmap? {
        return suspendCoroutine { continuation ->
            try {
                Glide.with(context)
                    .asBitmap()
                    .load(url)
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            continuation.resume(resource)
                        }
                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            continuation.resume(null)
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            } catch (e: Exception) {
                e.printStackTrace()
                continuation.resume(null)
            }
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
        if (message.startsWith("coreML/-/")) {
            val parts = message.split("/-/")
            val imageUrl = if (parts.size >= 2) parts[1] else ""
            val index = (if (parts.size >= 2) parts[2] else "0").toInt()

            if (alreadyProcessedUrls.containsKey(imageUrl)) {
                callSafegazeOnDeviceModelHandler(alreadyProcessedUrls[imageUrl]!!, index)
            } else {
                addTaskToQueue(imageUrl, index)
            }
        }
        if (message.contains("page_refresh")) {
            preferences.edit().putInt("session_censored_count", 0).apply()
        } else if(message.contains("replaced")) {
            handleAllTimeCounter()
            handleCurrentSessionCounter()
        }
    }

    private fun addTaskToQueue(url: String, index: Int) {
        urlQueue.add(UrlInfo(url, index))
        processQueue()
    }

    private fun processQueue() {
        if (processingJob?.isActive != true) {

            processingJob = scope.launch {
                while (urlQueue.isNotEmpty()) {
                    val task = urlQueue.poll()

                    task?.let {
                        if (alreadyProcessedUrls.containsKey(it.url)) {
                            callSafegazeOnDeviceModelHandler(alreadyProcessedUrls[it.url]!!, it.index)
                        } else {
                            val shouldBlur = shouldBlurImage(it.url, this)
                            alreadyProcessedUrls[it.url] = shouldBlur
                            callSafegazeOnDeviceModelHandler(shouldBlur, it.index)
                        }
                    }
                }
            }
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
        scope.cancel()
    }
}
