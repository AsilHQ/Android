package com.duckduckgo.app.kahf_guard.dialogs.NativeDnsHelperFragments.Fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.duckduckgo.app.browser.BrowserTabFragment;
import com.duckduckgo.app.browser.R;


public class NativeDnsHelperFragmentIntro extends Fragment {
    // Empty constructor required for Fragment instantiation
    public NativeDnsHelperFragmentIntro(){
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = (ViewGroup) inflater.inflate(R.layout.dialog_native_dns_helper_fragment_intro, container, false);
        final Activity activity = new Activity();

        //set title and description
        final ImageView logo =  view.findViewById(R.id.logo);
        final TextView title = view.findViewById(R.id.title);
        final TextView description = view.findViewById(R.id.description);
        if (BrowserTabFragment.Companion.getLAST_CONNECT_OR_DISCONNECT_PRESSED()){
            title.setText(R.string.native_dns_helper_title_intro);
            description.setText(R.string.native_dns_helper_text_intro);
        } else {
            logo.setImageResource(R.drawable.logo_disconnected);
            title.setText(R.string.native_dns_helper_title_intro_disable);
            description.setText(R.string.native_dns_helper_text_intro_disable);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) getView().requestLayout();
    }
}
