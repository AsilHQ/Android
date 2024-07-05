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

class CustomDnsResolver {

    fun resolve(
        uri: Uri,
        onQueryResolved: (String) -> Unit
    ) {
        this.resolve(uri.host ?: "", onQueryResolved)
    }

    fun resolve(
        domain: String,
        onQueryResolved: (String) -> Unit
    ) {
        val query = createDnsQuery(domain.plus(".")) // trailing dot to make absolute URL

        if (query != null) {
            sendDoHRequest(query, onQueryResolved)
        } else {
            onQueryResolved("")
        }
    }

    private fun createDnsQuery(domain: String): ByteArray? {
        return try {
            val record = Record.newRecord(org.xbill.DNS.Name.fromString(domain), Type.A, DClass.IN)
            val query = Message.newQuery(record)
            query.toWire()
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    private fun sendDoHRequest(
        queryData: ByteArray,
        onQueryResolved: (String) -> Unit
    ) {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://sp-dns-doh.kahfguard.com/dns-query")
            .addHeader("Content-Type", "application/dns-message")
            .addHeader("Accept", "application/dns-message")
            .post(
                queryData.toRequestBody(
                    "application/dns-message".toMediaTypeOrNull(),
                    0,
                    queryData.size,
                ),
            )
            .build()

        client.newCall(request)
            .enqueue(
                object : Callback {
                    override fun onFailure(
                        call: Call,
                        e: IOException
                    ) {
                        Timber.e(e)
                        onQueryResolved("")
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response
                    ) {
                        if (response.isSuccessful) {
                            response.body?.let { responseBody ->
                                val responseBytes = responseBody.bytes()
                                val responseMessage = Message(responseBytes)

                                val answers = responseMessage.getSection(Section.ANSWER)
                                val ip = answers.first { it.type == Type.A }.rdataToString() // Address Record

                                onQueryResolved(ip)
                            }
                        } else {
                            onQueryResolved("")
                            Timber.e("DoH query failed with status code ${response.code}")
                        }
                    }
                },
            )
    }
}
