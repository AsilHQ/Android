package com.duckduckgo.app.dns

import androidx.core.net.toUri
import com.duckduckgo.common.utils.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.xbill.DNS.DClass
import org.xbill.DNS.Message
import org.xbill.DNS.Record
import org.xbill.DNS.Section
import org.xbill.DNS.Type
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class CachedDnsResponse(
    val message: Message,
    val expirationTimeMillis: Long
) {
    fun isExpired() = System.currentTimeMillis() > expirationTimeMillis
}

class CustomDnsResolver(private val dispatcher: DispatcherProvider) {
    companion object {
        private val cache = ConcurrentHashMap<String, CachedDnsResponse>()
    }

    private val dohServerUrl = "https://sp-dns-doh.kahfguard.com/dns-query"
    private val client = OkHttpClient()

    /**
     * @return Pair.first is the IP address, Pair.second is the domain name
     */
    suspend fun sendDnsQueries(domain: android.net.Uri, visitedDomains: MutableSet<String> = mutableSetOf()): Pair<String, String>? {
        val host = (domain.host ?: domain.toString()).removeSuffix(".").plus(".")

        return checkCacheAndSendRequest(host, Type.A, createDnsQuery(host, Type.A), visitedDomains)
            ?: checkCacheAndSendRequest(host, Type.AAAA, createDnsQuery(host, Type.AAAA), visitedDomains)
            ?: checkCacheAndSendRequest(host, Type.CNAME, createDnsQuery(host, Type.CNAME), visitedDomains)
    }

    private suspend fun checkCacheAndSendRequest(
        domain: String,
        recordType: Int,
        queryData: ByteArray,
        visitedDomains: MutableSet<String>
    ): Pair<String, String>? {
        val cacheKey = "$domain-$recordType"
        cache[cacheKey]?.takeUnless { it.isExpired() }?.let { cachedResponse ->
            return getDnsResponse(cachedResponse.message, visitedDomains)
        }

        return sendDoHRequest(queryData, recordType, domain, visitedDomains)
    }

    private fun createDnsQuery(domain: String, type: Int): ByteArray {
        val name = org.xbill.DNS.Name.fromString(domain, org.xbill.DNS.Name.root)
        val record = Record.newRecord(name, type, DClass.IN)
        val query = Message.newQuery(record)

        return query.toWire()
    }

    private suspend fun sendDoHRequest(
        queryData: ByteArray,
        recordType: Int,
        domain: String,
        visitedDomains: MutableSet<String>
    ): Pair<String, String>? {
        val request = Request.Builder()
            .url(dohServerUrl)
            .addHeader("Content-Type", "application/dns-message")
            .addHeader("Accept", "application/dns-message")
            .post(queryData.toRequestBody("application/dns-message".toMediaTypeOrNull())) // Simplified request body
            .build()

        return suspendCoroutine { continuation ->
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Timber.e(e)
                    continuation.resume(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        response.body?.bytes()?.let { responseBytes ->
                            val responseMessage = Message(responseBytes)
                            cacheDnsResponse(domain, recordType, responseMessage)

                            CoroutineScope(dispatcher.io()).launch {
                                val dnsResponse = getDnsResponse(responseMessage, visitedDomains)
                                continuation.resume(dnsResponse)
                            }
                        } ?: continuation.resume(null)
                    } else {
                        continuation.resume(null)
                        Timber.e("DoH query failed with status code ${response.code}")
                    }
                }
            })
        }
    }

    private suspend fun getDnsResponse(responseMessage: Message, visitedDomains: MutableSet<String>): Pair<String, String>? {
        val answers = responseMessage.getSection(Section.ANSWER)
        val results = mutableListOf<Pair<String, String>?>()

        // Checking for A record when there are multiple answers
        val aRecord = answers.firstOrNull { it.type == Type.A }
        if (aRecord != null) {
            return Pair(aRecord.rdataToString(), aRecord.name.toString(true))
        }

        for (record in answers) {
            when (record.type) {
                Type.AAAA -> {
                    val ipAddress = record.rdataToString()
                    results.add(Pair(ipAddress, record.name.toString(true)))
                }
                Type.CNAME -> {
                    val cnameTarget = record.rdataToString()

                    if (!visitedDomains.contains(cnameTarget)) {
                        visitedDomains.add(cnameTarget)
                        results.add(sendDnsQueries(cnameTarget.toUri(), visitedDomains))
                    } else {
                        results.add(null)
                    }
                }
                else -> results.add(null)
            }
        }

        return results.firstOrNull()
    }

    private fun cacheDnsResponse(domain: String, recordType: Int, responseMessage: Message) {
        val cacheKey = "$domain-$recordType"
        val ttl = responseMessage.getMinTTL()
        val cachedResponse = CachedDnsResponse(responseMessage, System.currentTimeMillis() + ttl * 1000)
        cache[cacheKey] = cachedResponse
    }

    private fun Message.getMinTTL(): Long {
        return getSection(Section.ANSWER).minOfOrNull { it.ttl } ?: 0L
    }
}
