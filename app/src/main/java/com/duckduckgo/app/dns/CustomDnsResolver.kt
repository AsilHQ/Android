package com.duckduckgo.app.dns

import androidx.core.net.toUri
import com.duckduckgo.common.utils.DispatcherProvider
import okhttp3.Dns
import org.xbill.DNS.CNAMERecord
import org.xbill.DNS.ExtendedResolver
import org.xbill.DNS.Lookup
import org.xbill.DNS.Record
import org.xbill.DNS.Resolver
import org.xbill.DNS.SimpleResolver
import org.xbill.DNS.Type
import timber.log.Timber
import java.net.InetAddress

class CustomDnsResolver(private val dispatcher: DispatcherProvider): Dns {

    private val dnsResolverHigh: Resolver = SimpleResolver("low.kahfguard.com").apply {
        timeout = java.time.Duration.ofSeconds(3)
    }

    init {
        Lookup.setDefaultResolver(
            ExtendedResolver(arrayOf(dnsResolverHigh)),
        )
    }

    override fun lookup(hostname: String): List<InetAddress> {
        val resolvedIp = resolveDomain(hostname.toUri())

        return if (resolvedIp != null) {
            listOf(InetAddress.getByName(resolvedIp.first))
        } else {
            Dns.SYSTEM.lookup(hostname)  // FIXME return empty list
        }
    }

    /**
     * Resolves a domain name to its IP address, following CNAME records if needed.
     * @param domain The domain name to resolve.
     * @return Pair.first is the IP/CNAME, Pair.second is the domain name.
     */
    fun resolveDomain(domain: android.net.Uri): Pair<String, String>? {
        val host = (domain.host ?: domain.toString()).removeSuffix(".")
        return resolveDomainRecursive(host, 0)
    }

    private fun resolveDomainRecursive(domain: String, depth: Int): Pair<String, String>? {
        if (depth > MAX_DEPTH) {
            Timber.d("ipLog Exceeded maximum CNAME resolution depth for $domain")
            return null
        }

        try {
            val lookup = Lookup(domain, Type.A)
            val records: Array<Record>? = lookup.run()

            if (lookup.result == Lookup.SUCCESSFUL) {
                val ipAddress = records?.firstOrNull()?.rdataToString() ?: ""
                return Pair(ipAddress, domain)
            } else {
                Timber.d("ipLog Failed to resolve domain: $domain. Reason: ${lookup.errorString}")
                // Try resolving CNAME if no A records found
                return resolveCname(domain, depth)
            }
        } catch (e: Exception) {
            Timber.d("ipLog Exception occurred: ${e.message}")
            return null
        }
    }

    private fun resolveCname(domain: String, depth: Int): Pair<String, String>? {
        return try {
            val lookup = Lookup(domain, Type.CNAME)
            val records: Array<Record>? = lookup.run()

            if (lookup.result == Lookup.SUCCESSFUL) {
                val cnameRecord = records?.firstOrNull() as? CNAMERecord
                cnameRecord?.let {
                    val cnameTarget = it.target.toString()
                    resolveDomainRecursive(cnameTarget, depth + 1)
                } ?: run {
                    Timber.d("ipLog No CNAME record found for $domain")
                    null
                }
            } else {
                Timber.d("ipLog Failed to resolve CNAME for $domain. Reason: ${lookup.errorString}")
                null
            }
        } catch (e: Exception) {
            Timber.d("ipLog Exception occurred while resolving CNAME: ${e.message}")
            null
        }
    }

    companion object {
        const val MAX_DEPTH: Int = 5
    }
}
