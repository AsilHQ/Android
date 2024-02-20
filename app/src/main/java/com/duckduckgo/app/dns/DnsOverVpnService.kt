package com.duckduckgo.app.dns

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import timber.log.Timber
import java.io.IOException

class DnsOverVpnService : VpnService() {
    private var thread: Thread? = null
    private var parcelInterface: ParcelFileDescriptor? = null
    var builder: Builder = Builder()

    override fun onStartCommand(
        intent: Intent,
        flags: Int,
        startId: Int
    ): Int {
        Timber.v(SERVICE_NAME, "run() executed.")
        val intentAction = intent.action
        if (intentAction == INTENT_ACTION_START_VPN) {
            //start
            startVpn()
        } else if (intentAction == INTENT_ACTION_STOP_VPN) {
            //stop
            mStopVpn()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Timber.v(SERVICE_NAME, "onDestroy")
        if (thread != null) {
            thread!!.interrupt()
        }
        isServiceRunning = false
        broadcastVpnStatusChanged(isServiceRunning)
        super.onDestroy()
    }

    private fun startVpn() {
        Timber.v(SERVICE_NAME, "Starting...")

        // Start a new session by creating a new thread.
        thread = Thread(
            {
                Timber.v(SERVICE_NAME, "Creating interface...")
                isServiceRunning = true
                broadcastVpnStatusChanged(isServiceRunning)
                try {
                    parcelInterface = builder.setSession("VPNService")
                        .addAddress("10.0.2.0", 24)
                        .addDnsServer("188.166.216.127")
                        .addDnsServer("206.189.155.86")
                        .establish()
                    Timber.v(SERVICE_NAME, "Interface created.")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            SERVICE_NAME + " Runnable",
        )

        //start the service
        thread!!.start()
    }

    private fun mStopVpn() {
        Timber.v(SERVICE_NAME, "Stopping...")
        stopSelf()
        if (parcelInterface != null) {
            Timber.v(SERVICE_NAME, "Closing Interface...")
            try {
                parcelInterface!!.close()
                Timber.v(SERVICE_NAME, "Interface closed.")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun broadcastVpnStatusChanged(status: Boolean) {
        val intent = Intent(INTENT_VPN_STATUS_CHANGED)
        intent.putExtra("status", status)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    companion object {
        const val SERVICE_NAME = "DnsOverVpnService"

        const val INTENT_ACTION_START_VPN = "LocalVpnServiceStartVpn"
        const val INTENT_ACTION_STOP_VPN = "LocalVpnServiceStopVpn"
        const val INTENT_VPN_STATUS_CHANGED = "LocalVpnServiceVpnStatusChanged"

        private var isServiceRunning = false
        fun isVpnRunning(connectivityManager: ConnectivityManager): Boolean {
            Timber.v(SERVICE_NAME, "isVpnRunning")
            if (isServiceRunning) {
                Timber.v(SERVICE_NAME, "isVpnRunning - Service running")
                try {
                    val activeNetwork = connectivityManager.activeNetwork
                    val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
                    if (caps != null) {
                        Timber.v(SERVICE_NAME, "isVpnRunning - caps available")
                        return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return false
            }
            return false
        }
    }
}
