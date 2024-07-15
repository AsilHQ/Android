/*
 * Copyright (c) 2024 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.app.kahftube

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import org.halalz.kahftube.extentions.injectJavascriptFileFromAsset
import timber.log.Timber

/**
 * Created by Asif Ahmed on 5/2/24.
 */

class KahfTubeWebViewClient : WebViewClient() {

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        Timber.v("shouldOverrideUrlLoading: request.url: ${request?.url}")
        return super.shouldOverrideUrlLoading(view, request)
    }

    override fun onPageFinished(
        webView: WebView?,
        url: String?
    ) {
        super.onPageFinished(webView, url)
        Timber.v("onPageFinished.url: $url")
        if (url == "https://m.youtube.com/?noapp") {
            webView?.injectJavascriptFileFromAsset("kahftube/email.js")
        }
    }
}
