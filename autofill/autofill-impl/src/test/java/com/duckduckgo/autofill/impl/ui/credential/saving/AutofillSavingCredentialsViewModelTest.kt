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

package com.duckduckgo.autofill.impl.ui.credential.saving

import com.duckduckgo.app.statistics.pixels.Pixel
import com.duckduckgo.autofill.impl.store.InternalAutofillStore
import com.duckduckgo.autofill.impl.store.NeverSavedSiteRepository
import com.duckduckgo.common.test.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class AutofillSavingCredentialsViewModelTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val mockStore: InternalAutofillStore = mock()
    private val pixel: Pixel = mock()
    private val neverSavedSiteRepository: NeverSavedSiteRepository = mock()
    private val testee = AutofillSavingCredentialsViewModel(
        neverSavedSiteRepository = neverSavedSiteRepository,
        pixel = pixel,
        dispatchers = coroutineTestRule.testDispatcherProvider,
    ).also { it.autofillStore = mockStore }

    @Test
    fun whenUserPromptedToSaveThenFlagSet() = runTest {
        testee.userPromptedToSaveCredentials()
        verify(mockStore).hasEverBeenPromptedToSaveLogin = true
    }

    @Test
    fun whenUserSpecifiesNeverToSaveCurrentSiteThenSitePersisted() = runTest {
        val url = "https://example.com"
        testee.addSiteToNeverSaveList(url)
        verify(neverSavedSiteRepository).addToNeverSaveList(eq(url))
    }
}
