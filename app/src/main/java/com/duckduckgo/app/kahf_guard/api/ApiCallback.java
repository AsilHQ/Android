package com.duckduckgo.app.kahf_guard.api;

public interface ApiCallback {
    void onResponse(Object response);
    void onError(String errorMessage);
}
