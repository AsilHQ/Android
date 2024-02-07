package com.duckduckgo.app.kahftube

import android.webkit.JavascriptInterface
import com.google.gson.Gson

/**
 * Created by Asif Ahmed on 17/1/24.
 */

class KahfTubeUnsubscribeInterface(
    private val haramChannelIds: List<String>,
    private val javaScriptCallBack: JavaScriptCallBack
) {

    @JavascriptInterface
    fun responseCallback(
        isSuccess: Boolean
    ) {
        javaScriptCallBack.responseCallback(isSuccess)
    }

    @JavascriptInterface
    fun getHaramChannelIds(): String {

        //return haramChannelIds
        return Gson().toJson(haramChannelIds)
    }

    interface JavaScriptCallBack {
        fun responseCallback(isSuccess: Boolean) {}
    }
}
