/*
 * Copyright (c) 2024 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.app.kahf_guard.layouts;

import static com.duckduckgo.common.utils.ConstantsKt.URL_BOTTOM_BUTTON_COMMUNITY;
import static com.duckduckgo.common.utils.ConstantsKt.URL_BOTTOM_BUTTON_CONTACT;
import static com.duckduckgo.common.utils.ConstantsKt.URL_BOTTOM_BUTTON_PATREON;
import static com.duckduckgo.common.utils.ConstantsKt.URL_BOTTOM_BUTTON_SHARE;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import com.duckduckgo.app.kahf_guard.api.Api;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.duckduckgo.app.browser.R;
import com.duckduckgo.app.browser.databinding.BottomContainerBinding;
import com.duckduckgo.app.kahf_guard.api.ApiCallback;
import com.duckduckgo.app.kahf_guard.api.BottomBanner;

import timber.log.Timber;

public class BottomContainer extends ConstraintLayout {

    private final Api _api = new Api();
    private final BottomContainerBinding _binding;
    public BottomContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        _binding = BottomContainerBinding.inflate(LayoutInflater.from(context), this, true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        _setup();
    }

    private void _setup(){
        //setup multiple screen sizes without creating dimens, strings or whatever for simplicity (few items).
        //when things gets complex and big, create those files.
        _setupMultipleScreenSizes();

        //setup button clicks
        _setBottomButtonClicks();

        //get bottom banner;
        new Handler().postDelayed(this::_getBottomBanner, 1000);
    }

    private void _setupMultipleScreenSizes(){
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        if (dpWidth <= 400){
            Timber.tag("setupMultipleScreenSizes").v("Small screen");
            _binding.bottomButtonPatreonText.setVisibility(View.GONE);
        } else {
            Timber.tag("setupMultipleScreenSizes").v("Not Small screen");
        }
    }

    private void _getBottomBanner(){
        final Context context = this.getContext();
        final Resources resources = context.getResources();
        final float firstStep = resources.getDimension(R.dimen.bottom_banner_max_height_and_button_height) - resources.getDimension(R.dimen.bottom_banner_max_height) - resources.getDimension(R.dimen.bottom_container_padding);
        final float secondStep = 0;

        //prepare callback
        ApiCallback apiCallback = new ApiCallback() {
            @Override
            public void onResponse(Object response) {
                BottomBanner bottomBanner = (BottomBanner) response;

                try {
                    Glide.with(context).load(bottomBanner.imageUrl).listener(new RequestListener<>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                            Timber.tag("Bottom Banner").v("Image load failed: %s", ((e != null) ? e.toString() : ""));
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                            try {
                                Timber.tag("Bottom Banner").v("Ready");

                                final SpringAnimation springAnim = new SpringAnimation(_binding.getRoot(), DynamicAnimation.TRANSLATION_Y, firstStep);
                                springAnim.getSpring().setStiffness(SpringForce.STIFFNESS_LOW);
                                springAnim.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
                                springAnim.start();

                                //set banner click
                                _binding.bottomBanner.setOnClickListener(view -> {
                                    try {
                                        //cue to notify user click happened
                                        ObjectAnimator scaleXDown = ObjectAnimator.ofFloat(view, "scaleX", 0.9f);
                                        ObjectAnimator scaleYDown = ObjectAnimator.ofFloat(view, "scaleY", 0.9f);

                                        // Scale up back to 1
                                        ObjectAnimator scaleXUp = ObjectAnimator.ofFloat(view, "scaleX", 1f);
                                        ObjectAnimator scaleYUp = ObjectAnimator.ofFloat(view, "scaleY", 1f);

                                        scaleXDown.setDuration(100);
                                        scaleYDown.setDuration(100);
                                        scaleXUp.setDuration(100);
                                        scaleYUp.setDuration(100);

                                        // Create an AnimatorSet to play animations in sequence
                                        AnimatorSet scaleDown = new AnimatorSet();
                                        scaleDown.playTogether(scaleXDown, scaleYDown);

                                        AnimatorSet scaleUp = new AnimatorSet();
                                        scaleUp.playTogether(scaleXUp, scaleYUp);

                                        AnimatorSet scaleAnimation = new AnimatorSet();
                                        scaleAnimation.playSequentially(scaleDown, scaleUp);
                                        scaleAnimation.start();

                                        _openUrl(bottomBanner.link);
                                    } catch (Exception e) {
                                        Timber.tag("_getBottomBanner banner onClick").v("Exception: %s", e.getMessage());
                                    }
                                });

                                //show bottom buttons
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    try {
                                        _binding.bottomButtons.setVisibility(VISIBLE);
                                        final SpringAnimation springAnim1 = new SpringAnimation(_binding.getRoot(), DynamicAnimation.TRANSLATION_Y, secondStep);
                                        springAnim1.getSpring().setStiffness(SpringForce.STIFFNESS_LOW);
                                        springAnim1.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
                                        springAnim1.start();
                                    } catch (Exception e) {
                                        Timber.tag("_getBottomBanner onResponse show buttons").v("Exception: %s", e.getMessage());
                                    }
                                }, 1000);
                            } catch (Exception e){
                                Timber.tag("_getBottomBanner onResponse onResourceReady").v("Exception: %s", e.getMessage());
                            }

                            return false;
                        }
                    }).into(_binding.bottomBanner);
                } catch (Exception e){
                    Timber.tag("_getBottomBanner onResponse").v("Exception: %s", e.getMessage());
                }
            }

            @Override
            public void onError(String errorMessage) {
                Timber.tag("_getBottomBanner apiCallback onError").v(errorMessage);
            }
        };

        //execute
        _api.getBottomBanner(apiCallback);
    }

    private void _setBottomButtonClicks(){
        _binding.bottomButtonShare.setOnClickListener(view -> {
            final Activity activity = (Activity) getContext();
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.share_title));
            String shareMessage = activity.getString(R.string.share_description, URL_BOTTOM_BUTTON_SHARE);
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            activity.startActivity(Intent.createChooser(shareIntent, "Where to share Kahf Guard?"));
        });
        _binding.bottomButtonCommunity.setOnClickListener(view -> _openUrl(URL_BOTTOM_BUTTON_COMMUNITY));
        _binding.bottomButtonContact.setOnClickListener(view -> _openUrl(URL_BOTTOM_BUTTON_CONTACT));
        _binding.bottomButtonPatreon.setOnClickListener(view -> _openUrl(URL_BOTTOM_BUTTON_PATREON));
    }

    private void _openUrl(String url){
        if (getContext() == null) return;

        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            getContext().startActivity(browserIntent);
        } catch (Exception e) {
            //sometimes there is no browser installed or no browser is there to handle this event.
            Timber.tag("BottomContainer _openUrl").v("Exception: %s", e.getMessage());

            //try showing the toast with url instead
            Toast.makeText(getContext(), url, Toast.LENGTH_LONG).show();
        }
    }
}
