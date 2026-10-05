package com.shilapi.xcertplay.network

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/**
 * Picks the address the iPhone should reach the accessory on for hotspot style backends
 * (Wi-Fi Direct and LocalOnlyHotspot), where the app itself owns the network.
 *
 * IPv4 wins over the IPv6 link-local address. Android configures the link-local route of an AP
 * interface in a per-network policy table that carries no matching ip rule, so an app socket
 * without a network mark cannot send replies to a link-local peer: the framework silently drops
 * the SYN-ACK and the iPhone retries until it gives up. The IPv4 subnet route is also installed in
 * the shared `local_network` table, so replies work without any network selection.
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
        !address.isLoopbackAddress && !address.isAnyLocalAddress && !address.isLinkLocalAddress
}
