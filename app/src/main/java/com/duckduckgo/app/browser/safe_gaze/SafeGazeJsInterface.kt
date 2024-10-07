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
// import com.duckduckgo.app.safegaze.personDetection.PersonDetector
import com.duckduckgo.common.utils.SAFE_GAZE_PREFERENCES
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal data class UrlInfo(val url: String, val uid: String)

class SafeGazeJsInterface(
    private val context: Context,
    private val webView: DuckDuckGoWebView,
    private val nsfwDetector: NsfwDetector,
    private val genderDetector: GenderDetector
) {
    private val dispatcher: DefaultDispatcherProvider = DefaultDispatcherProvider()
    private val preferences: SharedPreferences = context.getSharedPreferences(SAFE_GAZE_PREFERENCES, Context.MODE_PRIVATE)

    private val onDeviceModelCachedResults = mutableMapOf<String, Boolean>()
    // private val personDetector = PersonDetector(context)

    private val urlQueue: ConcurrentLinkedQueue<UrlInfo> = ConcurrentLinkedQueue()
    private var processingJob: Job? = null
    private val scope = CoroutineScope(dispatcher.computation() + Job())
    
    inner class UnifiedCounter {
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

    val counter = UnifiedCounter()

    private suspend fun shouldBlurImage(url: String, mScope: CoroutineScope, isPersonCheck: Boolean): Boolean {
        return suspendCoroutine { continuation ->
            mScope.launch {
                val bitmap = loadImageBitmapFromUrl(url, context)

                if (bitmap != null) {
                //NOTE: Person detection is disabled for now
                //     if (isPersonCheck) {
                //         val containsHuman = personDetector.hasPerson(bitmap)
                //         continuation.resume(containsHuman)
                //    } else {
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
                    // }
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
    fun callSafegazeOnDeviceModelHandler(isExist: Boolean, uid: String, quotaExceeded: Boolean) {
        val jsFunctionCall = "safegazeOnDeviceModelHandler($isExist, '$uid', $quotaExceeded);"
        webView.post {
            webView.evaluateJavascript(jsFunctionCall, null)
        }
    }

    @JavascriptInterface
    fun updateBlur(blur: Float){
        val trimmedBlur = blur / 100
        val jsFunction = "window.blurIntensity = $trimmedBlur; updateBluredImageOpacity();"
        webView.post {
            webView.evaluateJavascript(jsFunction, null)
        }
    }

    @JavascriptInterface
    fun sendMessage(message: String) {
        if (message.startsWith("coreML/-/")) {
            val parts = message.split("/-/")
            val imageUrl = if (parts.size >= 2) parts[1] else ""
            val uid = (if (parts.size >= 2) parts[2] else "0")
            if (onDeviceModelCachedResults.containsKey(imageUrl)) {
                callSafegazeOnDeviceModelHandler(onDeviceModelCachedResults[imageUrl]!!, uid, counter.isQuotaExceeded())
            } else {
                addTaskToQueue(imageUrl, uid)
            }
        }
        if (message.contains("page_refresh")) {
            counter.resetSession()
        }
    }

    private fun addTaskToQueue(url: String, uid: String) {
        urlQueue.add(UrlInfo(url, uid))
        processQueue()
    }

    private fun processQueue() {
        if (processingJob?.isActive != true) {
            processingJob = scope.launch {
                while (urlQueue.isNotEmpty()) {
                    val task = urlQueue.poll()

                    task?.let {
                        counter.checkAndResetQuota()
                        val shouldBlur = shouldBlurImage(it.url, this, true)
                        counter.incrementDailyQuota(shouldBlur)
                        counter.incrementSessionAndAllTimeCount(shouldBlur)

                        onDeviceModelCachedResults[it.url] = shouldBlur
                        callSafegazeOnDeviceModelHandler(shouldBlur, it.uid, counter.isQuotaExceeded())
                    }
                }
            }
        }
    }

    
    fun cancelOngoingImageProcessing() {
        counter.saveToPreferences()
        scope.cancel()
    }
}
