package com.duckduckgo.app.kahf_guard.connect;

public class ConnectByNativeDnsCheckResult {
    public boolean isEnabled;
    public boolean isYoutubeNoSs;

    public ConnectByNativeDnsCheckResult(boolean isEnabled, boolean isYoutubeNoSs) {
        this.isEnabled = isEnabled;
        this.isYoutubeNoSs = isYoutubeNoSs;
    }
}
