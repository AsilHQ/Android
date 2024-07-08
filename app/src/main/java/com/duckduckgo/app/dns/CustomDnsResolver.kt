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
    private val dohServerUrl = "https://sp-dns-doh.kahfguard.com/dns-query"

    private val client = OkHttpClient()
    private val cache = ConcurrentHashMap<String, CachedDnsResponse>()

    suspend fun sendDnsQueries(domain: android.net.Uri): String? {
        val host = domain.host?.plus(".") ?: return null // trailing dot to make absolute URL

        return checkCacheAndSendRequest(host, Type.A, createDnsQuery(host, Type.A), mutableSetOf())
            ?: checkCacheAndSendRequest(host, Type.AAAA, createDnsQuery(host, Type.AAAA), mutableSetOf())
            ?: checkCacheAndSendRequest(host, Type.CNAME, createDnsQuery(host, Type.CNAME), mutableSetOf())
    }

    private suspend fun checkCacheAndSendRequest(
        domain: String,
        recordType: Int,
        queryData: ByteArray,
        visitedDomains: MutableSet<String>
    ): String? {
        val cacheKey = "$domain-$recordType"
        cache[cacheKey]?.takeUnless { it.isExpired() }?.let { cachedResponse ->
            Timber.d("ipLog Cache hit!")
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
    ): String? {
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

    private suspend fun getDnsResponse(responseMessage: Message, visitedDomains: MutableSet<String>): String? {
        val answers = responseMessage.getSection(Section.ANSWER)
        val results = mutableListOf<String?>()

        for (record in answers) {
            when (record.type) {
                Type.A -> {
                    val ipAddress = record.rdataToString()
                    results.add(0, ipAddress)
                }
                Type.AAAA -> {
                    val ipAddress = record.rdataToString()
                    results.add(ipAddress)
                }
                Type.CNAME -> {
                    val cnameTarget = record.rdataToString()

                    if (!visitedDomains.contains(cnameTarget)) {
                        visitedDomains.add(cnameTarget)
                        sendDnsQueries(cnameTarget.toUri())
                    }
                }
                else -> results.add(null)
            }
        }

        return results.first()
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
