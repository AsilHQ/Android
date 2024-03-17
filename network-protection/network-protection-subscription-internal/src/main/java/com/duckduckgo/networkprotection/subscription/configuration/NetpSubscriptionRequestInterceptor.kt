/*
 * Copyright (c) 2023 DuckDuckGo
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

package com.duckduckgo.networkprotection.subscription.configuration

import com.duckduckgo.appbuildconfig.api.AppBuildConfig
import com.duckduckgo.appbuildconfig.api.isInternalBuild
import com.duckduckgo.di.scopes.AppScope
import com.duckduckgo.networkprotection.impl.BuildConfig
import com.duckduckgo.networkprotection.impl.configuration.NetpRequestInterceptor
import com.duckduckgo.networkprotection.impl.store.NetworkProtectionRepository
import com.duckduckgo.networkprotection.subscription.NetpSubscriptionManager
import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.anvil.annotations.ContributesBinding.Priority.HIGHEST
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import logcat.logcat
import okhttp3.Interceptor.Chain
import okhttp3.Response

@ContributesBinding(
    AppScope::class,
    priority = HIGHEST, // binding for internal-testing build wins
)
class NetpSubscriptionRequestInterceptor @Inject constructor(
    private val appBuildConfig: AppBuildConfig,
    private val netpSubscriptionManager: NetpSubscriptionManager,
    private val networkProtectionRepository: NetworkProtectionRepository,
) : NetpRequestInterceptor {

    override fun intercept(chain: Chain): Response {
        val url = chain.request().url
        val newRequest = chain.request().newBuilder()
        return if (ENDPOINTS_PATTERN_MATCHER.any { url.toString().endsWith(it) }) {
            logcat { "Adding Authorization Bearer token to request $url" }
            newRequest.addHeader(
                name = "Authorization",
                // this runBlocking is fine as we're already in a background thread
                value = "bearer ddg:${runBlocking { netpSubscriptionManager.getToken() }}",
            )

            if (appBuildConfig.isInternalBuild()) {
                newRequest.addHeader(
                    name = "NetP-Debug-Code",
                    value = BuildConfig.NETP_DEBUG_SERVER_TOKEN,
                )
            }

            chain.proceed(
                newRequest.build().also { logcat { "headers: ${it.headers}" } },
            ).also {
                networkProtectionRepository.vpnAccessRevoked = it.code == 403
            }
        } else {
            chain.proceed(newRequest.build())
        }
    }

    companion object {
        // The NetP environments are for now https://<something>.netp.duckduckgo.com/<endpoint>
        private val ENDPOINTS_PATTERN_MATCHER = listOf(
            "netp.duckduckgo.com/servers",
            "netp.duckduckgo.com/register",
            "netp.duckduckgo.com/locations",
        )
    }
}
