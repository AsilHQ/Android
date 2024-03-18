package org.mozilla.fenix.host_blocker

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL

object SafeGazeBlockListManager {
    fun fetchAndOverwriteHostFile(context: Context) {
        val localHostFile = File("${context.filesDir}/safe_gaze.txt")
        if (localHostFile.parentFile?.exists() == false) {
            localHostFile.parentFile?.mkdirs()
            println("SafeGazeBlockListManager: Parent directory created for custom file path.")
        }

        val remoteHostFileURL = URL("https://storage.asil.co/safegazeIgnore.txt")

        try {
            println("SafeGazeBlockListManager: Attempting to download host file from $remoteHostFileURL")

            val remoteHostFileData = remoteHostFileURL.readBytes()
            println("SafeGazeBlockListManager: Host file downloaded successfully.")

            FileOutputStream(localHostFile).use { fos ->
                fos.write(remoteHostFileData)
                println("SafeGazeBlockListManager: Host file overwritten to custom location.")
            }

            println("SafeGazeBlockListManager: Host file downloaded and overwritten successfully.")
        } catch (e: IOException) {
            println("SafeGazeBlockListManager: Error writing to the local host file: $e")
        }
    }

}
