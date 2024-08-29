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

package com.duckduckgo.app.prayers.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Madhab
import com.duckduckgo.app.browser.R
import com.duckduckgo.app.browser.databinding.MadhabAndCalculationSelectionBsFragmentBinding
import com.duckduckgo.app.prayers.adapters.CalculationMethodRecyclerAdapter
import com.duckduckgo.app.prayers.adapters.MadhabAsrTimeRecyclerAdapter
import com.duckduckgo.app.prayers.landing.PrayersLandingFragment.ItemOffsetDecoration
import com.duckduckgo.app.prayers.listeners.OnCalculationMethodClickedListener
import com.duckduckgo.app.prayers.listeners.OnMadhabMethodClickedListener
import com.duckduckgo.common.ui.view.toDp
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MadhabAndCalculationSelectionBsFragment: BottomSheetDialogFragment() {
    var madhabOptions: MutableMap<Madhab, String>? = null
    var calculationOptions: MutableMap<CalculationMethod, String>? = null
    var onMadhabClicked: OnMadhabMethodClickedListener? = null
    var onCalculationClicked: OnCalculationMethodClickedListener? = null

    private lateinit var binding: MadhabAndCalculationSelectionBsFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.madhab_and_calculation_selection_bs_fragment, container, false)
        binding = MadhabAndCalculationSelectionBsFragmentBinding.bind(view)
        return view
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        if (madhabOptions != null) {
            binding.title.text = getString(R.string.kahf_madhab_asr_time)

            binding.recyclerView.adapter = MadhabAsrTimeRecyclerAdapter(
                madhabOptions!!, requireContext(), onMadhabClicked!!,
            )
        } else if (calculationOptions != null) {
            binding.title.text = getString(R.string.kahf_calculation_method)

            binding.recyclerView.adapter = CalculationMethodRecyclerAdapter(
                calculationOptions!!, requireContext(), onCalculationClicked!!,
            )

            binding.recyclerView.setPadding(0, 0, 0, 400.toDp())
        } else {
            throw IllegalArgumentException("MadhabOptions and CalculationOptions cannot be null at the same time")
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.addItemDecoration(ItemOffsetDecoration(requireContext()))
    }

    companion object {
        fun builder() = Builder()
    }

    class Builder {
        private val fragment = MadhabAndCalculationSelectionBsFragment()
        private var madhabOptions: MutableMap<Madhab, String>? = null
        private var calculationOptions: MutableMap<CalculationMethod, String>? = null
        private var onMadhabClicked: OnMadhabMethodClickedListener? = null
        private var onCalculationClicked: OnCalculationMethodClickedListener? = null

        fun setMadhabOptions(
            options: MutableMap<Madhab, String>,
            onMadhabClicked: OnMadhabMethodClickedListener
        ): Builder {
            this.madhabOptions = options
            this.onMadhabClicked = onMadhabClicked
            return this
        }

        fun setCalculationOptions(
            options: MutableMap<CalculationMethod, String>,
            onCalculationClicked: OnCalculationMethodClickedListener
        ): Builder {
            this.calculationOptions = options
            this.onCalculationClicked = onCalculationClicked
            return this
        }

        fun build(): MadhabAndCalculationSelectionBsFragment {
            fragment.madhabOptions = madhabOptions
            fragment.calculationOptions = calculationOptions
            fragment.onMadhabClicked = onMadhabClicked
            fragment.onCalculationClicked = onCalculationClicked
            return fragment
        }
    }
}
