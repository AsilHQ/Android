package com.duckduckgo.app.kahf_guard.dialogs;

import android.content.DialogInterface;

import androidx.annotation.NonNull;

public interface DialogOnDismissAndExtraParam {
    public void onDismiss(@NonNull DialogInterface dialogInterface, String extraParam);
}
