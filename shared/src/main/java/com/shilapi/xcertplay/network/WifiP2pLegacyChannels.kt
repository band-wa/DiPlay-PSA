package com.shilapi.xcertplay.network

import android.net.wifi.p2p.WifiP2pManager
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Requests a group-owner operating channel on Android 9, where the public Wi-Fi Direct API cannot.
 *
 * Android 10 added `WifiP2pConfig.Builder.setGroupOperatingFrequency`, which [WifiP2pGroupManager]
 * uses to pin the group owner to a 5 GHz channel. Android 9 has no public equivalent, but
 * `WifiP2pManager.setWifiP2pChannels(channel, listenChannel, operatingChannel, listener)` is a
 * light-greylist hidden method: reflection reaches it without a platform signature and without a
 * hidden API exemption, and the framework forwards the operating channel to wpa_supplicant as
 * `P2P_SET disallow_freq 1000-(f-5),(f+5)-6000`, which is the restriction group-owner selection
 * honours. The listen channel itself stays in 2.4 GHz, because AOSP accepts only 1..11 there, and
 * that does not restrict the group owner.
 *
 * A non-SDK member can still be missing or blocked on a vendor build, so every failure path returns
 * `false` instead of throwing. A caller that asked for a specific channel must then fail loudly
 * rather than bring CarPlay up on an unknown band.
 */
internal object WifiP2pLegacyChannels {
    /** AOSP maps the listen channel straight onto `P2P_SET listen_channel`, which accepts 1..11. */
    const val LISTEN_CHANNEL = 6

    private const val CALLBACK_TIMEOUT_MILLIS = 2_000L

    /**
     * The channel a group-owner request would use for [frequencyMHz], or null when the legacy API
     * cannot pin it. DFS channels (52..144) are excluded on purpose: the radio has to leave one on
     * radar detection, which would drop a running CarPlay session.
     */
    fun operatingChannelFor(frequencyMHz: Int): Int? = when {
        frequencyMHz in 5180..5240 || frequencyMHz in 5745..5825 ->
            wifiFrequencyMhzToChannel(frequencyMHz)
        frequencyMHz in 2412..2472 -> wifiFrequencyMhzToChannel(frequencyMHz)?.takeIf { it in 1..13 }
        else -> null
    }

    fun bandLabelFor(channel: Int): String = if (channel in 1..14) "2.4 GHz" else "5 GHz"

    /**
     * Applies [operatingChannel] and waits for the framework callback. Returns true only when the
     * framework acknowledged the request, which is the closest Android 9 can get to confirming the
     * negotiated channel.
     */
    fun apply(
        manager: WifiP2pManager,
        channel: WifiP2pManager.Channel,
        operatingChannel: Int,
        diagnostic: (String) -> Unit,
    ): Boolean {
        val method = try {
            WifiP2pManager::class.java.getMethod(
                "setWifiP2pChannels",
                WifiP2pManager.Channel::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                WifiP2pManager.ActionListener::class.java,
            )
        } catch (missing: Throwable) {
            diagnostic("Wi-Fi P2P legacy channel selection unavailable failureClass=${failureClass(missing)}")
            return false
        }
        val settled = CountDownLatch(1)
        val accepted = AtomicBoolean(false)
        val listener = object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                accepted.set(true)
                settled.countDown()
            }

            override fun onFailure(reason: Int) {
                diagnostic("Wi-Fi P2P legacy channel rejected code=$reason requestedChannel=$operatingChannel")
                settled.countDown()
            }
        }
        try {
            method.invoke(manager, channel, LISTEN_CHANNEL, operatingChannel, listener)
        } catch (failure: Throwable) {
            diagnostic("Wi-Fi P2P legacy channel call failed failureClass=${failureClass(failure)}")
            return false
        }
        val completed = try {
            settled.await(CALLBACK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("Interrupted while selecting the Wi-Fi Direct channel", interrupted)
        }
        if (!completed) {
            diagnostic("Wi-Fi P2P legacy channel callback timeout requestedChannel=$operatingChannel")
        }
        return completed && accepted.get()
    }

    private fun failureClass(failure: Throwable): String = (failure.cause ?: failure).javaClass.simpleName
}
