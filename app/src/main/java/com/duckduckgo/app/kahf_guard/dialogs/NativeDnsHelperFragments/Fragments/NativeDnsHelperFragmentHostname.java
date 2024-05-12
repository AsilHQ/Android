package com.duckduckgo.app.kahf_guard.dialogs.NativeDnsHelperFragments.Fragments;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

import com.duckduckgo.app.browser.BrowserTabFragment;
import com.duckduckgo.app.kahf_guard.connect.ConnectByNativeDns;
import com.google.android.material.snackbar.Snackbar;
import com.duckduckgo.app.browser.R;

public class NativeDnsHelperFragmentHostname extends Fragment {
    private Snackbar _snackbar;

    // Empty constructor required for Fragment instantiation
    public NativeDnsHelperFragmentHostname(){
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = (ViewGroup) inflater.inflate(R.layout.dialog_native_dns_helper_fragment_our_hostname, container, false);
        final Activity activity = getActivity();

        //animate entry
        Animation fadeInAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in);
        view.startAnimation(fadeInAnimation);

        //set title and description
        final ImageView logo = (ImageView) view.findViewById(R.id.logo);
        final TextView title = (TextView) view.findViewById(R.id.title);
        final TextView description = (TextView) view.findViewById(R.id.description);
        if (BrowserTabFragment.Companion.getLAST_CONNECT_OR_DISCONNECT_PRESSED()){
            title.setText(R.string.native_dns_helper_title_our_hostname);
            description.setText(R.string.native_dns_helper_text_our_hostname);
        } else {
            logo.setImageResource(R.drawable.logo_disconnected);
            title.setText(R.string.native_dns_helper_title_our_hostname_disable);
            description.setText(R.string.native_dns_helper_text_our_hostname_disable);
        }

        //set hostname
        TextView hostname = view.findViewById(R.id.hostname);
        hostname.setText((new ConnectByNativeDns(activity)).getPrivateDnsHostname());

        if (BrowserTabFragment.Companion.getLAST_CONNECT_OR_DISCONNECT_PRESSED()) {
            //set hostname copy
            hostname.setOnClickListener(view1 -> {
                if (activity != null) {
                    Button button = (Button) view1;
                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Kahf Guard Private DNS", button.getText());
                    if (clipboard == null || clip == null) return;
                    clipboard.setPrimaryClip(clip);

                    //show toast
                    _snackbar = Snackbar.make(view, R.string.native_dns_hostname_copied, Snackbar.LENGTH_LONG);
                    _snackbar.show();
                }
            });
        }

        //return
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (_snackbar != null){
            _snackbar.dismiss();
        }
    }

    public void onResume() {
        super.onResume();

        if (getView() == null){
            return;
        }

        if (getView() != null) getView().requestLayout();
    }
}
