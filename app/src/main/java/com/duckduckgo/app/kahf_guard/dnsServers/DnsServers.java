package com.duckduckgo.app.kahf_guard.dnsServers;

import java.util.ArrayList;

public abstract class DnsServers {
    public abstract ArrayList<String> getStandardDnsServers();
    public abstract String getPrivateDnsHostname();
}
