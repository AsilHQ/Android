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

import android.content.res.ColorStateList
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.duckduckgo.app.browser.R
import com.duckduckgo.app.browser.databinding.SafegazeButtonBinding
import com.duckduckgo.app.kahftube.PopupButtonType.High
import com.duckduckgo.app.kahftube.PopupButtonType.Low
import com.duckduckgo.app.kahftube.PopupButtonType.Medium
import com.duckduckgo.common.ui.view.hide
import com.duckduckgo.common.ui.view.show

sealed class PopupButtonType(val name: String) {
    data object High : PopupButtonType("High")
    data object Medium : PopupButtonType("Medium")
    data object Low : PopupButtonType("Low")

    companion object {
        fun get(name: String) = when (name) {
            "High" -> High
            "Medium" -> Medium
            "Low" -> Low
            else -> Low
        }
    }
}

class PopupButton(
    private val binding: SafegazeButtonBinding,
    type: PopupButtonType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var color: ColorStateList
    var label: String
    var icon = 0

    fun updateState(isSelected: Boolean) {
        binding.btnContainer.backgroundTintList = color
        binding.btnLabel.text = label
        binding.ivCheck.setImageResource(icon)

        if (isSelected) {
            binding.btnContainer.background = AppCompatResources.getDrawable(binding.root.context, R.drawable.kahf_button_bg)
            binding.ivCheck.show()
            binding.btnLabel.setTextColor(
                ColorStateList.valueOf(
                ContextCompat.getColor(binding.root.context, com.duckduckgo.mobile.android.R.color.white)
            ))
        } else {
            binding.btnContainer.background = AppCompatResources.getDrawable(binding.root.context, R.drawable.kahf_button_bg_outlined)
            binding.ivCheck.hide()
            binding.btnLabel.setTextColor(color)
        }
    }

    init {
        when (type) {
            High -> {
                label = binding.root.context.getString(R.string.kahf_high)
                color = ColorStateList.valueOf(
                    ContextCompat.getColor(binding.root.context, com.duckduckgo.mobile.android.R.color.kahf_green)
                )
                icon = R.drawable.ic_check_green
            }
            Medium -> {
                label = binding.root.context.getString(R.string.kahf_medium)
                color = ColorStateList.valueOf(
                    ContextCompat.getColor(binding.root.context, com.duckduckgo.mobile.android.R.color.kahf_orange)
                )
                icon = R.drawable.ic_check_orange
            }
            Low -> {
                label = binding.root.context.getString(R.string.kahf_low)
                color = ColorStateList.valueOf(
                    ContextCompat.getColor(binding.root.context, com.duckduckgo.mobile.android.R.color.kahf_red)
                )
                icon = R.drawable.ic_check_red
            }
        }

        updateState(isSelected)

        binding.btnContainer.setOnClickListener {
            onClick()
        }
    }
}
