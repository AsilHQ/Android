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

package com.duckduckgo.app.browser.safe_gaze_and_host_blocker.helper

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.webkit.WebView
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.net.URI

class HostBlockerHelper(
    webView: WebView? = null,
    private val context: Context? = null
) {
    private val _webView = webView
    private val errorHtml = "<!DOCTYPE html>\n" +
        "<html>\n" +
        "<head>\n" +
        "    <style>\n" +
        "        body {\n" +
        "            display: flex;\n" +
        "            flex-direction: column;\n" +
        "            justify-content: center;\n" +
        "            align-items: center;\n" +
        "            height: 100vh;\n" +
        "            margin: 0;\n" +
        "        }\n" +
        "        img {\n" +
        "            max-width: 100%;\n" +
        "            max-height: 80%;\n" +
        "        }\n" +
        "        .quran-verses {\n" +
        "            color: #000;\n" +
        "            text-align: center;\n" +
        "            font-family: Zilla Slab;\n" +
        "            font-size: 31px;\n" +
        "            font-style: normal;\n" +
        "            font-weight: 500;\n" +
        "            line-height: normal;\n" +
        "            padding-top: 40px;\n" +
        "            padding-left: 60px;\n" +
        "            padding-right: 60px;\n" +
        "        }\n" +
        "        .place {\n" +
        "            color: #000;\n" +
        "            font-family: Zilla Slab;\n" +
        "            font-size: 18px;\n" +
        "            font-style: normal;\n" +
        "            font-weight: 400;\n" +
        "            line-height: normal;\n" +
        "            padding-top: 40px;\n" +
        "        }\n" +
        "    </style>\n" +
        "</head>\n" +
        "<body>\n" +
        "    <img src=\"https://storage.asil.co/403Restricted.png\" alt=\"Image\" isSent=\"true\" />\n" +
        "    <div class=\"quran-verses\">\n" +
        "        \"Tell the believing men that they should lower their gaze and guard their modesty; that will make for greater purity for them; And Allah is well acquainted with all that they do. And tell the believing women that they should lower their gaze and guard their modesty…\".\n" +
        "    </div>\n" +
        "    <div class=\"place\">\n" +
        "        (Quran 24:30-31)\n" +
        "    </div>\n" +
        "</body>\n" +
        "</html>\n"
    
    @SuppressLint("SetJavaScriptEnabled") fun blockUrl(uri: String, isQuery: Boolean = false): Boolean{
        return if (shouldBlockHost(uri, isQuery)){
            val dataUri = "data:text/html;charset=utf-8;base64," + Base64.encodeToString(
                errorHtml.toByteArray(),
                Base64.NO_PADDING,
            )
            _webView?.settings?.javaScriptEnabled = true
            _webView?.loadUrl(dataUri)
            true
        }else{
            false
        }
    }

    private fun shouldBlockHost(url: String?, isQuery: Boolean): Boolean {
        val host: String = if (isQuery){
            url ?: ""
        }else{
            extractHost(url)
        }

        try {
            val hostsTxtFilePath = "${context?.filesDir}/hosts.txt"
            val file = File(hostsTxtFilePath)
            if (!file.exists()) {
                Timber.tag("HostBlocker").d("Hosts file not found at path: $hostsTxtFilePath")
                return false
            }

            val inputStream = FileInputStream(file)
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                if (line?.contains("#") == true || line?.isEmpty() == true) {
                    continue
                }
                val components = line?.split("\\s+".toRegex())
                if (components != null) {
                    if (components.size >= 2) {
                        val domain = components[1]
                        if (host == domain) {
                            Timber.tag("HostBlocker").d("Domain -> $domain")
                            Timber.tag("HostBlocker").d("Host -> $host")
                            return true
                        }
                    }
                }
            }

            return false
        } catch (e: Exception) {
            Timber.tag("HostBlocker").d("Error reading hosts.txt: ${e.message}")
            return false
        }
    }

    private fun extractHost(url: String?): String {
        return try {
            val uri = URI(url)
            uri.host ?: ""
        } catch (e: Exception) {
            Timber.tag("HostBlocker").d("Error extracting host: ${e.message}")
            ""
        }
    }

}
