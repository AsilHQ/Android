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

package com.duckduckgo.app.browser.host_blocker

import android.content.Context
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL

object HostBlockerManager {
    fun fetchAndOverwriteHostFile(context: Context) {
        val localHostFile = File("${context.filesDir}/hosts.txt")
        if (localHostFile.parentFile?.exists() == false) {
            localHostFile.parentFile?.mkdirs()
            Timber.d("HostBlockerManager: Parent directory created for custom file path.")
        }

        val remoteHostFileURL = URL("https://storage.asil.co/hosts.txt")

        try {
            Timber.d("HostBlockerManager: Attempting to download host file from $remoteHostFileURL")

            val remoteHostFileData = remoteHostFileURL.readBytes()
            Timber.d("HostBlockerManager: Host file downloaded successfully.")

            FileOutputStream(localHostFile).use { fos ->
                fos.write(remoteHostFileData)
            }
        } catch (e: IOException) {
            Timber.d("HostBlockerManager: Error writing to the local host file: $e")
        }
    }

}

