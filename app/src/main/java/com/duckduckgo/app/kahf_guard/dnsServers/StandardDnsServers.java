package com.duckduckgo.app.kahf_guard.dnsServers;

import java.util.ArrayList;

public class StandardDnsServers extends DnsServers {
    @Override
    public ArrayList<String> getStandardDnsServers() {
        ArrayList<String> dnsServers = new ArrayList<>();
        dnsServers.add("137.184.251.32");
        dnsServers.add("139.59.194.85");

        return dnsServers;
    }

    @Override
    public String getPrivateDnsHostname() {
        return "sp-dns-dot.kahfguard.com";
    }
}
