package com.duckduckgo.app.kahf_guard.dialogs.NativeDnsHelperFragments.Fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.duckduckgo.app.browser.BrowserTabFragment;
import com.duckduckgo.app.browser.R;

public class NativeDnsHelperFragmentChangePrivateDns extends Fragment {
    public NativeDnsHelperFragmentChangePrivateDns(){
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.dialog_native_dns_helper_fragment_change_private_dns, container, false);

        //set title and description
        final ImageView logo = view.findViewById(R.id.logo);
        final TextView title = (TextView) view.findViewById(R.id.title);
        final TextView description = (TextView) view.findViewById(R.id.description);
        final TextView description2 = (TextView) view.findViewById(R.id.description2);
        if (BrowserTabFragment.Companion.getLAST_CONNECT_OR_DISCONNECT_PRESSED()){
            title.setText(R.string.native_dns_helper_title_private_dns_setting);
            description.setText(R.string.native_dns_helper_text_private_dns_setting);
            description2.setText(R.string.native_dns_helper_text2_private_dns_setting);
        } else {
            logo.setImageResource(R.drawable.logo_disconnected);
            title.setText(R.string.native_dns_helper_title_private_dns_setting_disable);
            description.setText(R.string.native_dns_helper_text_private_dns_setting_disable);
            description2.setText(R.string.native_dns_helper_text2_private_dns_setting_disable);
        }

        final Button viewGuideButton = (Button) view.findViewById(R.id.button_view_private_dns_guide);
        final ImageView privateDnsGuide = (ImageView) view.findViewById(R.id.private_dns_guide);

        if (BrowserTabFragment.Companion.getLAST_CONNECT_OR_DISCONNECT_PRESSED()) {
            viewGuideButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Slide up the button
                    ObjectAnimator buttonAnimator = ObjectAnimator.ofFloat(viewGuideButton, "alpha", 0);
                    buttonAnimator.setDuration(300);
                    buttonAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            // After button slides up, handle the image slide down
                            privateDnsGuide.setVisibility(View.VISIBLE);
                            privateDnsGuide.animate().alpha(1).setDuration(300).start();
                        }
                    });
                    buttonAnimator.start();
                }
            });

            //set gif
            Glide.with(this).load(R.raw.private_dns_guide).into((ImageView) view.findViewById(R.id.private_dns_guide));
        } else {
            view.findViewById(R.id.divider).setVisibility(View.GONE);
            viewGuideButton.setVisibility(View.GONE);
            privateDnsGuide.setVisibility(View.GONE);
        }

        //return
        return view;
    }

    public void onResume() {
        super.onResume();
        if (getView() != null) getView().requestLayout();
    }
}
