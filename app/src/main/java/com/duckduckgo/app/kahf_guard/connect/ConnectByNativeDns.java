package com.duckduckgo.app.kahf_guard.connect;

import android.content.Context;
import android.provider.Settings;

import com.duckduckgo.app.kahf_guard.dnsServers.DnsServers;
import com.duckduckgo.app.kahf_guard.dnsServers.NoYoutubeSafeSearchDnsServers;
import com.duckduckgo.app.kahf_guard.dnsServers.StandardDnsServers;
import com.duckduckgo.app.kahf_guard.utils.KahfGuardSharedPrefManager;

public class ConnectByNativeDns {
    DnsServers selectedDnsServers;
    StandardDnsServers standardDnsServers;
    NoYoutubeSafeSearchDnsServers noYoutubeSafeSearchDnsServers;

    public ConnectByNativeDns(Context context){
        standardDnsServers = new StandardDnsServers();
        noYoutubeSafeSearchDnsServers = new NoYoutubeSafeSearchDnsServers();

        if (!KahfGuardSharedPrefManager.INSTANCE.isYoutubeNoSsEnabled(context)){
            selectedDnsServers = standardDnsServers;
        } else {
            selectedDnsServers = noYoutubeSafeSearchDnsServers;
        }
    }

    public DnsServers getSelectedDnsServers() {
        return selectedDnsServers;
    }

    public String getPrivateDnsHostname(){
        return selectedDnsServers.getPrivateDnsHostname();
    }

    public ConnectByNativeDnsCheckResult checkActivated(Context context){
        try {
            String dnsMode = Settings.Global.getString(context.getContentResolver(), "private_dns_mode");
            if (dnsMode != null && dnsMode.trim().equals("hostname")) {
                String hostname = Settings.Global.getString(context.getContentResolver(), "private_dns_specifier");
                if (hostname != null) {
                    //check in both standard and no youtube ss
                    if (hostname.equals(standardDnsServers.getPrivateDnsHostname())){
                        return new ConnectByNativeDnsCheckResult(true, false);
                    } else if (hostname.equals(noYoutubeSafeSearchDnsServers.getPrivateDnsHostname())){
                        return new ConnectByNativeDnsCheckResult(true, true);
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        return new ConnectByNativeDnsCheckResult(false, false);
    }
}
