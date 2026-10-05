package com.shilapi.xcertplay.network

import java.net.Inet6Address
import java.net.InetAddress

/**
 * Address the accessory publishes to the iPhone for a manual car hotspot, chosen the same way as
 * for the app's own access points (see [HotspotAddressPolicy]).
 *
 * IPv4 wins when the interface has one, because the phone reaches the access point over it
 * directly. The IPv6 link-local fallback stays scoped to [interfaceIndex] so that a firmware whose
 * access point has no IPv4 still has a usable address.
 */
internal fun wirelessHostAddress(addresses: List<InetAddress>, interfaceIndex: Int): InetAddress? {
    val selected = HotspotAddressPolicy.select(addresses) ?: return null
    if (selected is Inet6Address && interfaceIndex > 0) {
        return Inet6Address.getByAddress(null, selected.address, interfaceIndex)
    }
    return selected
}
