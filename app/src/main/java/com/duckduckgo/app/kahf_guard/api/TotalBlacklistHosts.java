package com.duckduckgo.app.kahf_guard.api;

public class TotalBlacklistHosts {
    public Number total;
    public String totalFormatted;
    public String lastUpdated;

    public TotalBlacklistHosts(Number total, String totalFormatted, String lastUpdated) {
        this.total = total;
        this.totalFormatted = totalFormatted;
        this.lastUpdated = lastUpdated;
    }
}
