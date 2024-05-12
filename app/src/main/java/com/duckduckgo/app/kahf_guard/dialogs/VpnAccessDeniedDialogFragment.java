package com.duckduckgo.app.kahf_guard.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.duckduckgo.app.browser.R;


public class VpnAccessDeniedDialogFragment extends DialogFragment {
    private @Nullable DialogOnDismissAndExtraParam _onDismissListener;
    private String cancelType = "cancel";

    // Empty constructor required for Fragment instantiation
    public VpnAccessDeniedDialogFragment(){
    }

    public void newInstance(@Nullable DialogOnDismissAndExtraParam onDismissListener){
        _onDismissListener = onDismissListener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //get layout
        final Activity activity = requireActivity();
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_vpn_access_denied_fragment, null);

        //prepare dialog
        builder.setView(dialogView);

        //create dialog
        Dialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        //set button
        dialogView.findViewById(R.id.button_cancel).setOnClickListener(view -> {
            dialog.cancel();
        });
        dialogView.findViewById(R.id.button_retry).setOnClickListener(view -> {
            cancelType = "retry";
            dialog.cancel();
        });
        return dialog;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        if (_onDismissListener != null) {
            _onDismissListener.onDismiss(dialog, cancelType);
        }
    }
}
