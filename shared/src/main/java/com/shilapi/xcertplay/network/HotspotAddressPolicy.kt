package com.shilapi.xcertplay.network

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/**
 * Picks the address the iPhone should reach the accessory on for every access-point style
 * backend: the app's own Wi-Fi Direct and LocalOnlyHotspot groups, and a car hotspot that the
 * head unit itself owns.
 *
 * IPv4 wins over the IPv6 link-local address. Android configures the link-local route of an AP
 * interface in a per-network policy table that carries no matching ip rule, so an app socket
 * without a network mark cannot send replies to a link-local peer: the framework silently drops
 * the SYN-ACK and the iPhone retries until it gives up. The IPv4 subnet route is installed in the
 * shared `local_network` table, so replies work without any network selection. A phone that took
 * an IPv4 lease also uses it directly, while a link-local IPv6 endpoint additionally requires the
 * phone to have an IPv6 route on that link.
 */
internal object HotspotAddressPolicy {
    fun select(addresses: List<InetAddress>): InetAddress? {
        var linkLocal: InetAddress? = null
        for (address in addresses) {
            if (address is Inet4Address && usable(address)) {
                return address
            }
            if (address is Inet6Address && address.isLinkLocalAddress && linkLocal == null) {
                linkLocal = address
            }
        }
        return linkLocal
    }

    private fun usable(address: InetAddress): Boolean =
        !address.isLoopbackAddress && !address.isAnyLocalAddress && !address.isLinkLocalAddress &&
            !address.isMulticastAddress
}
