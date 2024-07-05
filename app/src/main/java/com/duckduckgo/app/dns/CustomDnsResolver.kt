package com.duckduckgo.app.dns

import android.net.Uri
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

class CustomDnsResolver {
    private val dohServerUrl = "https://sp-dns-doh.kahfguard.com/dns-query"
    private val failed = "failed"
    
    private val client = OkHttpClient()
    private val cache = ConcurrentHashMap<String, CachedDnsResponse>()

    suspend fun resolve(uri: Uri): String {
        return resolve(uri.host ?: "")
    }

    suspend fun resolve(domain: String): String {
        val absDomain = domain.plus(".") // trailing dot to make absolute URL

        // Create queries outside the conditional blocks for potential reuse
        val queryA = createDnsQuery(absDomain, Type.A)
        val queryAAAA = createDnsQuery(absDomain, Type.AAAA)

        return when {
            queryA != null -> checkCacheAndSendRequest(absDomain, Type.A, queryA)
            queryAAAA != null -> checkCacheAndSendRequest(absDomain, Type.AAAA, queryAAAA)
            else -> failed
        }
    }

    private suspend fun checkCacheAndSendRequest(
        domain: String,
        recordType: Int,
        queryData: ByteArray,
    ): String {
        val cacheKey = "$domain-$recordType"
        cache[cacheKey]?.takeUnless { it.isExpired() }?.let { cachedResponse ->
            Timber.d("ipLog Cache hit!")
            return getDnsResponse(cachedResponse.message, recordType)
        }

        return sendDoHRequest(queryData, recordType, domain)
    }

    private fun createDnsQuery(domain: String, recordType: Int): ByteArray? {
        return try {
            val record = Record.newRecord(org.xbill.DNS.Name.fromString(domain), recordType, DClass.IN)
            Message.newQuery(record).toWire()
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    private suspend fun sendDoHRequest(
        queryData: ByteArray,
        recordType: Int,
        domain: String,
    ): String {
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
                    continuation.resume(failed)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        response.body?.bytes()?.let { responseBytes ->
                            val responseMessage = Message(responseBytes)
                            val ip = getDnsResponse(responseMessage, recordType)
                            cacheDnsResponse(domain, recordType, responseMessage)
                            continuation.resume(ip)
                        } ?: continuation.resume(failed) // Handle null body
                    } else {
                        continuation.resume(failed)
                        Timber.e("DoH query failed with status code ${response.code}")
                    }
                }
            })
        }
    }

    private fun getDnsResponse(responseMessage: Message, recordType: Int): String {
        return responseMessage.getSection(Section.ANSWER)
            .firstOrNull { it.type == recordType }
            ?.rdataToString()
            ?: failed
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
