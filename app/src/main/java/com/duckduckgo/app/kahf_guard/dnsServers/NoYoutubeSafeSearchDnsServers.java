package com.duckduckgo.app.kahf_guard.dnsServers;

import java.util.ArrayList;

public class NoYoutubeSafeSearchDnsServers extends DnsServers {
    @Override
    public ArrayList<String> getStandardDnsServers() {
        ArrayList<String> dnsServers = new ArrayList<>();
        dnsServers.add("167.172.5.116");
        dnsServers.add("146.190.194.246");

        return dnsServers;
    }

    @Override
    public String getPrivateDnsHostname() {
        return "sp-dns-dot-yt.kahfguard.com";
    }
}
