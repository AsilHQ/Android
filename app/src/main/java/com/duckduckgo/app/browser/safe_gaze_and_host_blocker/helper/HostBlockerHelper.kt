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
import android.webkit.WebResourceResponse
import android.webkit.WebView
import timber.log.Timber
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.net.URI

class HostBlockerHelper(
    private val context: Context? = null
) {
    private val errorHtml = """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Blocked</title>
            <style>
                body {
                    display: flex;
                    flex-direction: column;
                    justify-content: center;
                    align-items: center;
                    height: 100vh;
                    margin: 0;
                }
                img {
                    max-width: 100%;
                    max-height: 80%;
                }
                .quran-verses {
                    color: #000;
                    text-align: center;
                    font-family: Zilla Slab;
                    font-size: 31px;
                    font-weight: 500;
                    line-height: normal;
                    padding-top: 40px;
                    padding-left: 60px;
                    padding-right: 60px;
                }
                .place {
                    color: #000;
                    font-family: Zilla Slab;
                    font-size: 18px;
                    font-weight: 400;
                    line-height: normal;
                    padding-top: 40px;
                }
            </style>
        </head>
        <body>
            <img src="https://storage.asil.co/403Restricted.png" alt="Image" isSent="true" />
            <p style="text-align: center; font-size: 20px;"><h1>Blocked by KahfGuard</h1></p></body>
            <div class="quran-verses">
                "Tell the believing men that they should lower their gaze and guard their modesty; that will make for greater purity for them; And Allah is well acquainted with all that they do. And tell the believing women that they should lower their gaze and guard their modesty…".
            </div>
            <div class="place">
                (Quran 24:30-31)
            </div>
        </body>
        </html>
    """.trimIndent()
    private var blockedHosts: Set<String>? = null

    val blockedResourceResponse = WebResourceResponse(
        "text/html", // MIME type
        "UTF-8", // Encoding
        ByteArrayInputStream(errorHtml.toByteArray()),
    )

    init {
        loadBlockedHosts()
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun shouldBlock(uri: String, webView: WebView?, isQuery: Boolean = false): Boolean {
        return if (shouldBlockHost(uri, isQuery)) {
            val dataUri = "data:text/html;charset=utf-8;base64," + Base64.encodeToString(
                errorHtml.toByteArray(),
                Base64.NO_PADDING,
            )
            webView?.settings?.javaScriptEnabled = true
            webView?.loadUrl(dataUri)
            true
        } else {
            false
        }
    }

    private fun loadBlockedHosts() {
        try {
            val hostsTxtFilePath = "${context?.filesDir}/hosts.txt"
            val file = File(hostsTxtFilePath)
            if (!file.exists()) {
                Timber.tag("HostBlocker").d("Hosts file not found at path: $hostsTxtFilePath")
                blockedHosts = emptySet()
                return
            }

            val inputStream = FileInputStream(file)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val hostsSet = mutableSetOf<String>()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                if (line?.contains("#") == true || line?.isEmpty() == true) {
                    continue
                }
                val components = line?.split("\\s+".toRegex())
                if (components != null && components.size >= 2) {
                    hostsSet.add(components[1])
                }
            }

            blockedHosts = hostsSet
        } catch (e: Exception) {
            Timber.tag("HostBlocker").d("Error reading hosts.txt: ${e.message}")
            blockedHosts = emptySet()
        }
    }

    private fun shouldBlockHost(url: String?, isQuery: Boolean): Boolean {
        val host: String = if (isQuery) {
            url ?: ""
        } else {
            extractHost(url)
        }

        return blockedHosts?.contains(host) == true
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
