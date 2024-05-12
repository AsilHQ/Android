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

public class NativeDnsDisconnectedDialogFragment extends DialogFragment {
    private @Nullable DialogOnDismiss _onDismissListener;

    // Empty constructor required for Fragment instantiation
    public NativeDnsDisconnectedDialogFragment(){
    }

    public void newInstance(@Nullable DialogOnDismiss onDismissListener){
        _onDismissListener = onDismissListener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //get layout
        final Activity activity = requireActivity();
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_native_dns_disconnected_fragment, null);

        //prepare dialog
        builder.setView(dialogView);

        //create dialog
        Dialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        //set button
        dialogView.findViewById(R.id.button_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });
        return dialog;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        if (_onDismissListener != null) {
            _onDismissListener.onDismiss(dialog);
        }
    }
}
