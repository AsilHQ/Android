/*
 * Copyright (c) 2022 DuckDuckGo
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

package com.duckduckgo.autoconsent.impl.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duckduckgo.anvil.annotations.ContributesViewModel
import com.duckduckgo.autoconsent.api.Autoconsent
import com.duckduckgo.autoconsent.impl.R
import com.duckduckgo.di.scopes.ActivityScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ContributesViewModel(ActivityScope::class)
class AutoconsentSettingsViewModel @Inject constructor(private val autoconsent: Autoconsent) : ViewModel() {
    data class ViewState(
        val autoconsentEnabled: Boolean,
    )
    sealed class Command {
        data class LaunchLearnMoreWebPage(val url: String = LEARN_MORE_URL, @StringRes val titleId: Int = R.string.autoconsentTitle) : Command()
    }

    private val command = Channel<Command>(1, BufferOverflow.DROP_OLDEST)

    private val viewStateFlow: MutableStateFlow<ViewState> =
        MutableStateFlow(ViewState(autoconsent.isSettingEnabled()))
    val viewState: StateFlow<ViewState> = viewStateFlow

    fun commands(): Flow<Command> {
        return command.receiveAsFlow()
    }

    fun onUserToggleAutoconsent(enabled: Boolean) {
        viewModelScope.launch {
            autoconsent.changeSetting(enabled)
            viewStateFlow.emit(ViewState(autoconsent.isSettingEnabled()))
        }
    }

    fun onLearnMoreSelected() {
        viewModelScope.launch { command.send(Command.LaunchLearnMoreWebPage()) }
    }

    companion object {
        const val LEARN_MORE_URL = "https://asil.co/asil-help-pages/privacy/web-tracking-protections/#cookie-pop-up-management"
    }
}
