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

package com.duckduckgo.mobile.android.vpn.integration

import com.duckduckgo.app.global.plugins.PluginPoint
import com.duckduckgo.di.scopes.VpnScope
import com.duckduckgo.mobile.android.vpn.network.VpnNetworkStack
import com.duckduckgo.mobile.android.vpn.state.VpnStateCollectorPlugin
import com.squareup.anvil.annotations.ContributesMultibinding
import org.json.JSONObject
import javax.inject.Inject

@ContributesMultibinding(VpnScope::class)
class VpnNetworkLayerStateCollector @Inject constructor(
    private val vpnNetworkStackPluginPoint: PluginPoint<VpnNetworkStack>
) : VpnStateCollectorPlugin {

    override suspend fun collectVpnRelatedState(appPackageId: String?): JSONObject {
        val name = vpnNetworkStackPluginPoint.getPlugins().firstOrNull { it.isEnabled() }?.name ?: "unknown"
        return JSONObject().apply {
            put("name", name)
        }
    }

    override val collectorName: String = "networkLayer"
}