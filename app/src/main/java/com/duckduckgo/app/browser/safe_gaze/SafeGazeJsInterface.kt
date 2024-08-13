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
import com.duckduckgo.app.trackerdetection.db.KahfImageBlocked
import com.duckduckgo.app.trackerdetection.db.KahfImageBlockedDao
import com.duckduckgo.common.utils.DefaultDispatcherProvider
// import com.duckduckgo.app.safegaze.personDetection.PersonDetector
import com.duckduckgo.common.utils.SAFE_GAZE_PREFERENCES
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
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
    private val genderDetector: GenderDetector,
    private val kahfImageBlockedDao: KahfImageBlockedDao
) {
    private val dispatcher: DefaultDispatcherProvider = DefaultDispatcherProvider()
    private val preferences: SharedPreferences = context.getSharedPreferences(SAFE_GAZE_PREFERENCES, Context.MODE_PRIVATE)

    private val onDeviceModelCachedResults = mutableMapOf<String, Boolean>()
    // private val personDetector = PersonDetector(context)

    private val urlQueue: ConcurrentLinkedQueue<UrlInfo> = ConcurrentLinkedQueue()
    private var processingJob: Job? = null
    private val scope = CoroutineScope(dispatcher.io() + Job())
    val counter = UnifiedCounter(preferences)

    private suspend fun shouldBlurImage(url: String, mScope: CoroutineScope, isPersonCheck: Boolean): Boolean {
        return suspendCoroutine { continuation ->
            mScope.launch {
                val bitmap = loadImageBitmapFromUrl(url, context)

                if (bitmap != null) {
                // NOTE: Person detection is disabled for now
                //     if (isPersonCheck) {
                //         val containsHuman = personDetector.hasPerson(bitmap)
                //         continuation.resume(containsHuman)
                //     } else {
                        val nsfwPrediction = nsfwDetector.isNsfw(bitmap)

                        if (nsfwPrediction.isSafe()) {
                            val genderPrediction = genderDetector.predict(bitmap)

                            if (genderPrediction.hasFemale) {
                                Timber.d("kLog Female (${genderPrediction.femaleConfidence}) $url")

                                // TODO Have to consider images blocked by remote model
                                // Insert to local DB
                                kahfImageBlockedDao.insert(KahfImageBlocked(
                                    imageUrl = url,
                                    tag = "female",
                                    score = genderPrediction.femaleConfidence.toDouble(),
                                ))
                            } else {
                                Timber.d("kLog SFW $url")
                            }

                            continuation.resume(genderPrediction.hasFemale)
                        } else {
                            nsfwPrediction.getLabelWithConfidence().let {
                                Timber.d("kLog Nsfw: ${it.first} (${it.second}) $url")

                                // Insert to local DB
                                kahfImageBlockedDao.insert(KahfImageBlocked(
                                    imageUrl = url,
                                    tag = it.first,
                                    score = it.second.toDouble(),
                                ))

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
