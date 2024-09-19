package com.duckduckgo.app.dns

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.duckduckgo.app.kahftube.SafetyLevel
import timber.log.Timber
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel

class DnsOverVpnService : VpnService() {
    private var thread: Thread? = null
    private var parcelInterface: ParcelFileDescriptor? = null
    private var dnsServers = mutableListOf<String>()
    private var tunnel: DatagramChannel? = null
    private val builder by lazy { this.Builder() }

    private fun setTunnel(tunnel: DatagramChannel) {
        this.tunnel = tunnel
    }

    override fun onStartCommand(
        intent: Intent,
        flags: Int,
        startId: Int
    ): Int {
        val mode = SafetyLevel.get(intent.getStringExtra("mode") ?: "")
        Timber.d("vpnLog onStartCommand. VPN mode: $mode")

        when (intent.action) {
            INTENT_ACTION_START_VPN -> {
                changeMode(mode)
                startVpn(isRestart = false)
            }
            INTENT_ACTION_STOP_VPN -> {
                stopVpn()
            }
            INTENT_CHANGE_MODE -> {
                restartVpnWithMode(mode)
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Timber.d("vpnLog onDestroy")
        if (thread != null) {
            thread!!.interrupt()
        }
        isServiceRunning = false
        broadcastVpnStatusChanged(isServiceRunning)
        super.onDestroy()
    }

    private fun startVpn(isRestart: Boolean) {
        Timber.d("vpnLog Starting VPN")
        thread = Thread(
            {
                isServiceRunning = true
                broadcastVpnStatusChanged(isServiceRunning)

                try {
                    builder.setSession("VPNService")
                        .addAddress("192.168.0.1", 24)
                        .addDnsServer(dnsServers[0])
                        .addDnsServer(dnsServers[1])
                        .setMtu(1280) // Maximum size of a data packet that can be Transmitted over the network interface without being fragmented
                        .establish()?.also {
                            parcelInterface = it

                            setTunnel(DatagramChannel.open())
                            tunnel?.connect(InetSocketAddress("127.0.0.1", 8087))
                            protect(tunnel?.socket())

                            if (isRestart) {
                                LocalBroadcastManager
                                    .getInstance(this@DnsOverVpnService)
                                    .sendBroadcast(Intent(INTENT_VPN_MODE_CHANGED))
                            }
                        }

                } catch (e: Exception) {
                    Timber.d("vpnLog startVpn failed: $e")
                    e.printStackTrace()
                }
            },
            "vpnLog Runnable",
        )
        thread!!.start()
    }

    private fun stopVpn() {
        Timber.d( "vpnLog Stopping VPN")
        stopSelf()
        try {
            parcelInterface?.close()
            parcelInterface = null
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun restartVpnWithMode(mode: SafetyLevel) {
        changeMode(mode)

        try {
            parcelInterface?.close()
            parcelInterface = null
        } catch (e: IOException) {
            e.printStackTrace()
        }
        thread?.interrupt()

        startVpn(isRestart = true)
    }

    private fun broadcastVpnStatusChanged(status: Boolean) {
        val intent = Intent(INTENT_VPN_STATUS_CHANGED)
        intent.putExtra("status", status)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun changeMode(mode: SafetyLevel) {
        // Old DNS server "188.166.216.127"
        dnsServers.clear()

        when (mode) {
            SafetyLevel.High -> {
                dnsServers.add("51.142.0.101")
                dnsServers.add("51.142.0.102")
            }
            SafetyLevel.Medium -> {
                dnsServers.add("51.142.0.99")
                dnsServers.add("51.142.0.100")
            }
            SafetyLevel.Low -> {
                dnsServers.add("51.142.0.97")
                dnsServers.add("51.142.0.98")
            }
        }
    }

    companion object {
        private const val SERVICE_NAME = "DnsOverVpnService"

        const val INTENT_ACTION_START_VPN = "LocalVpnServiceStartVpn"
        const val INTENT_ACTION_STOP_VPN = "LocalVpnServiceStopVpn"
        const val INTENT_CHANGE_MODE = "LocalVpnServiceChangeMode"
        const val INTENT_VPN_STATUS_CHANGED = "LocalVpnServiceVpnStatusChanged"
        const val INTENT_VPN_MODE_CHANGED = "LocalVpnServiceVpnModeChanged"

        private var isServiceRunning = false

        fun isVpnRunning(connectivityManager: ConnectivityManager): Boolean {
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
