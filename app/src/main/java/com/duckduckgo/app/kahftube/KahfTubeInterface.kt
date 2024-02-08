package com.duckduckgo.app.kahftube

import android.content.Context
import android.webkit.JavascriptInterface
import com.duckduckgo.app.kahftube.SharedPreferenceManager.KeyString
import org.halalz.kahftube.enums.GenderEnum
import org.halalz.kahftube.enums.PracticingLevelEnum
import timber.log.Timber

/**
 * Created by Asif Ahmed on 17/1/24.
 */

class KahfTubeInterface(
    private val context: Context,
    private val javaScriptCallBack: JavaScriptCallBack
) {

    @JavascriptInterface
    fun showToast(message: String) {
        javaScriptCallBack.showToast(message)
    }

    @JavascriptInterface
    fun callHandler(
        name: String?,
        email: String?,
        imgSrc: String?
    ) {

        name?.let { SharedPreferenceManager(context).setValue(KeyString.NAME, it) }
        email?.let { SharedPreferenceManager(context).setValue(KeyString.EMAIL, it) }
        imgSrc?.let { SharedPreferenceManager(context).setValue(KeyString.IMAGE_SRC, it) }
        javaScriptCallBack.callHandler(name, email, imgSrc)
    }

    @JavascriptInterface
    fun onHalalzTap() {
        javaScriptCallBack.onHalalzTap()
    }

    @JavascriptInterface
    fun shouldRestart() {
        javaScriptCallBack.shouldRestart()
    }

    @JavascriptInterface
    fun getUserToken(): String {
        Timber.v("SharedPreferenceManager:: ${SharedPreferenceManager(context).getValue(KeyString.TOKEN)}")
        return SharedPreferenceManager(context).getValue(KeyString.TOKEN)
    }

    @JavascriptInterface
    fun getUserGender(): Int {
        return if (SharedPreferenceManager(context).getIntValue(KeyString.GENDER) == -1) {
            GenderEnum.MALE.value //default set to male
        } else {
            SharedPreferenceManager(context).getIntValue(KeyString.GENDER)
        }
    }

    @JavascriptInterface
    fun getUserPracticingLevel(): Int {
        return if (SharedPreferenceManager(context).getIntValue(KeyString.PRACTICING_LEVEL) == -1) {
            PracticingLevelEnum.PRACTICING_MUSLIM.value //default set to male
        } else {
            SharedPreferenceManager(context).getIntValue(KeyString.PRACTICING_LEVEL)
        }
    }

    @JavascriptInterface
    fun fetchYtInitialData(id: String?) {
        return javaScriptCallBack.fetchYtInitialData(id)
    }

    @JavascriptInterface
    fun share(href: String?) {
        return javaScriptCallBack.share(href)
    }

    @JavascriptInterface
    fun getChannels(jsonArrayString: String) {
        return javaScriptCallBack.getChannels(jsonArrayString)
    }

    interface JavaScriptCallBack {
        fun showToast(message: String) {}
        fun callHandler(
            name: String?,
            email: String?,
            imgSrc: String?
        ) {
        }

        fun fetchYtInitialData(id: String?) {}
        fun share(href: String?) {}
        fun onHalalzTap() {}

        fun shouldRestart() {}

        fun getChannels(jsonArrayString: String) {}
    }
}
