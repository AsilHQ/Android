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

package com.duckduckgo.app.kahftube

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.duckduckgo.app.browser.R
import com.duckduckgo.app.browser.databinding.SafeGazePopupBinding
import com.duckduckgo.app.browser.safe_gaze.SafeGazeJsInterface
import com.duckduckgo.common.utils.SAFE_GAZE_BLUR_PROGRESS
import com.duckduckgo.common.utils.SAFE_GAZE_DEFAULT_BLUR_VALUE
import com.duckduckgo.common.utils.SAFE_GAZE_INTENSITY
import com.hoko.blur.HokoBlur

class SafeGazePopupHandler(
    private val binding: SafeGazePopupBinding,
    private val sharedPreferences: SharedPreferences,
    private val safeGazeInterface: SafeGazeJsInterface,
    val editor: Editor,
    onModeChanged: (mode: SafetyLevel) -> Unit,
    onShareClicked: () -> Unit,
    onSupportClicked: () -> Unit,
    onThemeChanged: () -> Unit
) {
    init {
        var btnHigh: PopupButton? = null
        var btnMed: PopupButton? = null
        var btnLow: PopupButton? = null

        val preSelected: SafetyLevel = SafetyLevel.get(sharedPreferences.getString(SAFE_GAZE_INTENSITY, "")!!)

        btnHigh = PopupButton(
            binding.btnHigh,
            SafetyLevel.High,
            preSelected == SafetyLevel.High,
        ) {
            btnHigh?.updateState(true)
            btnMed?.updateState(false)
            btnLow?.updateState(false)
            updateDescription(binding, SafetyLevel.High)
            onModeChanged(SafetyLevel.High)
        }
        btnMed = PopupButton(
            binding.btnMedium,
            SafetyLevel.Medium,
            preSelected == SafetyLevel.Medium,
        ) {
            btnHigh.updateState(false)
            btnMed?.updateState(true)
            btnLow?.updateState(false)
            updateDescription(binding, SafetyLevel.Medium)
            onModeChanged(SafetyLevel.Medium)
        }

        btnLow = PopupButton(
            binding.btnLow,
            SafetyLevel.Low,
            preSelected == SafetyLevel.Low,
        ) {
            btnHigh.updateState(false)
            btnMed.updateState(false)
            btnLow?.updateState(true)
            updateDescription(binding, SafetyLevel.Low)
            onModeChanged(SafetyLevel.Low)
        }

        // set initially selected item
        updateDescription(binding, preSelected)

        handleProgressBar()
        loadImageWithBlur(
            sharedPreferences.getInt(SAFE_GAZE_BLUR_PROGRESS, SAFE_GAZE_DEFAULT_BLUR_VALUE),
            binding.ivFullBlur,
        )

        binding.btnShare.setOnClickListener { onShareClicked() }
        binding.btnSupport.setOnClickListener { onSupportClicked() }
        binding.btnTheme.setOnClickListener { onThemeChanged() }
    }

    private fun updateDescription(binding: SafeGazePopupBinding, type: SafetyLevel) {
        when (type) {
            SafetyLevel.High -> {
                binding.tvDescription.text = binding.root.context.getString(R.string.kahf_mode_desc_high)
                binding.tvDescription.backgroundTintList =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(binding.root.context, com.duckduckgo.mobile.android.R.color.kahf_green),
                    )
            }
            SafetyLevel.Medium -> {
                binding.tvDescription.text = binding.root.context.getString(R.string.kahf_mode_desc_medium)
                binding.tvDescription.backgroundTintList =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(binding.root.context, com.duckduckgo.mobile.android.R.color.kahf_orange),
                    )
            }
            SafetyLevel.Low -> {
                binding.tvDescription.text = binding.root.context.getString(R.string.kahf_mode_desc_low)
                binding.tvDescription.backgroundTintList =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(binding.root.context, com.duckduckgo.mobile.android.R.color.kahf_red),
                    )
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun handleProgressBar() {
        val progress = sharedPreferences.getInt(SAFE_GAZE_BLUR_PROGRESS, SAFE_GAZE_DEFAULT_BLUR_VALUE)
        binding.progressBar.progress = progress
        binding.blurSeekbar.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    binding.progressBar.progress = progress
                    safeGazeInterface.updateBlur(progress.toFloat())
                    loadImageWithBlur(progress, binding.ivFullBlur)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    saveProgressToSharedPreferences(seekBar.progress)
                }
            },
        )

        // updateViewsPosition(binding.iconImageView, sharedPreferences.getInt(SAFE_GAZE_BLUR_PROGRESS, SAFE_GAZE_DEFAULT_BLUR_VALUE))
        /*binding.progressBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val width = binding.progressBar.width.toFloat()
                    val x = event.x
                    val calculatedProgress = (x / width * binding.progressBar.max).toInt()
                    binding.progressBar.progress = calculatedProgress
                    // updateViewsPosition(binding.iconImageView, calculatedProgress)
                    safeGazeInterface.updateBlur(calculatedProgress.toFloat())
                    saveProgressToSharedPreferences(calculatedProgress)
                    loadImageWithBlur(calculatedProgress, binding.ivFullBlur)
                    true
                }
                else -> false
            }
        }*/
    }

    private fun updateViewsPosition(iconImageView: ImageView, progress: Int) {
        val clampedProgress = progress.coerceIn(0, 100)

        val iconLayoutParams = iconImageView.layoutParams as ConstraintLayout.LayoutParams
        iconLayoutParams.horizontalBias = clampedProgress / 100f
        iconImageView.layoutParams = iconLayoutParams
    }

    private fun saveProgressToSharedPreferences(progress: Int) {
        editor.putInt(SAFE_GAZE_BLUR_PROGRESS, progress)
        editor.apply()
    }

    private fun loadImageWithBlur(blurRadius: Int, imageView: ImageView) {
        val bitmap = BitmapFactory.decodeResource(binding.root.context.resources, R.drawable.blur_image_background)

        // Convert blurRadius to a 0-1 scale to match the JS blurIntensity
        val blurIntensity = blurRadius / 100f

        // Calculate blur value between 2px and 8px
        val blurValue = 2 + (blurIntensity * 6)

        // Calculate brightness value between 200% and 800%
        val brightnessValue = 2 + (blurIntensity * 6)

        // Apply blur
        val blurredBitmap = HokoBlur.with(binding.root.context)
            .radius(blurValue.toInt())
            .sampleFactor(1.0f)
            .forceCopy(false)
            .processor()
            .blur(bitmap)

        // Apply grayscale, contrast, and brightness
        val colorMatrix = ColorMatrix()

        // Grayscale (100%)
        colorMatrix.setSaturation(0f)

        // Contrast (500%)
        val scale = 5f
        val translate = (-.5f * scale + .5f) * 255f
        colorMatrix.postConcat(
            ColorMatrix(
                floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )

        // Brightness
        colorMatrix.postConcat(
            ColorMatrix(
                floatArrayOf(
                    brightnessValue, 0f, 0f, 0f, 0f,
                    0f, brightnessValue, 0f, 0f, 0f,
                    0f, 0f, brightnessValue, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )

        val colorFilter = ColorMatrixColorFilter(colorMatrix)
        imageView.colorFilter = colorFilter
        imageView.setImageBitmap(blurredBitmap)
    }
}
