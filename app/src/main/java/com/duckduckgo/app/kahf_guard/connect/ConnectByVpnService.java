/*
 * Created by Mehdi with ♥ / hi@mehssi.com
 * Last modified 2/23/24, 6:32 PM
 */

package com.duckduckgo.app.kahf_guard.connect;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.duckduckgo.app.kahf_guard.dnsServers.DnsServers;
import com.duckduckgo.app.kahf_guard.dnsServers.NoYoutubeSafeSearchDnsServers;
import com.duckduckgo.app.kahf_guard.dnsServers.StandardDnsServers;
import com.duckduckgo.app.kahf_guard.utils.KahfGuardSharedPrefManager;

import java.io.IOException;
import java.util.Objects;

import timber.log.Timber;

public class
ConnectByVpnService extends VpnService {
    //constants
    public static final String SERVICE_NAME = "ConnectByVpnService";

    //intents
    public static final String INTENT_ACTION_START_VPN = "vpnStarted";
    public static final String INTENT_ACTION_STOP_VPN = "vpnStopped";
    public static final String INTENT_VPN_STATUS_CHANGED = "vpnStatusChanged";

    //fields
    private Thread _thread;

    private ParcelFileDescriptor _interface = null;

    private Builder _builder = new Builder();

    // Services interface
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(SERVICE_NAME, "run() executed.");

        final String intentAction = intent.getAction();
        if (Objects.equals(intentAction, INTENT_ACTION_START_VPN)) {
            //start
            mStartVpn();
        } else if (Objects.equals(intentAction, INTENT_ACTION_STOP_VPN)) {
            //stop
            mStopVpn();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.v(SERVICE_NAME, "onDestroy");
    }

    private void mStartVpn() {
        DnsServers dnsServers;
        if (KahfGuardSharedPrefManager.INSTANCE.isYoutubeNoSsEnabled(this)){
            dnsServers = new NoYoutubeSafeSearchDnsServers();
        } else {
            dnsServers = new StandardDnsServers();
        }

        Timber.tag(SERVICE_NAME).v("Youtube ss: %s", KahfGuardSharedPrefManager.INSTANCE.isYoutubeNoSsEnabled(this));

        Log.v(SERVICE_NAME, "Starting...");
        // Start a new session by creating a new thread.
        _thread = new Thread(() -> {
            Log.v(SERVICE_NAME, "Creating interface...");

            try {
                //build vpn service
                _builder = _builder.setSession("VPNService")
                        .addAddress("10.0.5.0", 24);

                //add dns servers
                for (String currentDnsServer : dnsServers.getStandardDnsServers()) {
                    Log.v(SERVICE_NAME, "Set dns server: " + currentDnsServer);
                    _builder.addDnsServer(currentDnsServer);
                }

                //connect vpn
                _interface = _builder.establish();
                broadcastVpnStatusChanged(true);

                Log.v(SERVICE_NAME, "Interface created.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, SERVICE_NAME + " Runnable");

        //start the vpn service
        _thread.start();
    }

    private void mStopVpn() {
        Log.v(SERVICE_NAME, "Stopping...");

        stopSelf();
        if (_interface != null) {
            Log.v(SERVICE_NAME, "Closing Interface...");

            try {
                _interface.close();
                broadcastVpnStatusChanged(false);
                Log.v(SERVICE_NAME, "Interface closed.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcastVpnStatusChanged(boolean status){
        Intent intent = new Intent(INTENT_VPN_STATUS_CHANGED);
        intent.putExtra("status", status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public static boolean isVpnRunning(ConnectivityManager connectivityManager) {
        try {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
            if (caps != null) {
                Log.v(SERVICE_NAME, "isVpnRunning - caps available");
                return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}