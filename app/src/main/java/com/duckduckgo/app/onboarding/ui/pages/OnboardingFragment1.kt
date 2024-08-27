package com.duckduckgo.app.onboarding.ui.pages

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.duckduckgo.app.browser.databinding.FragmentOnboarding1Binding
import com.duckduckgo.app.onboarding.ui.KahfOnboardingActivity

/*@InjectWith(FragmentScope::class)
class OnboardingFragment1 : DuckDuckGoFragment(R.layout.fragment_onboarding1) {

    lateinit var binding: FragmentOnboarding1Binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOnboarding1Binding.inflate(inflater, container, false)
        return binding.root
    }
}*/

@SuppressLint("NoFragment")
class OnboardingFragment1 : Fragment() {

    lateinit var binding: FragmentOnboarding1Binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOnboarding1Binding.inflate(inflater, container, false)

        binding.btnContinue.setOnClickListener {
            (requireActivity() as KahfOnboardingActivity).onContinueClicked()
        }

        return binding.root
    }
}
