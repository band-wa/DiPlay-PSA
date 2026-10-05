# Compatibility

This public preview is an independent receiver, not an Apple-certified CarPlay accessory. The experimental bundled accessory identity is extractable and its future acceptance is not guaranteed.

| Area | Current scope |
| --- | --- |
| Head unit | Android 9+ APK; wired USB, car hotspot and Wi-Fi Direct, including a requested 5 GHz Wi-Fi Direct channel on Android 9 |
| Phone | Standard, non-jailbroken iPhone with CarPlay enabled; device/iOS compatibility varies |
| Physical evidence | Previous private builds: wired and wireless picture, touch and audio confirmed on the development car with iPhone XS / iOS 18.7.10 |
| Other cars | Mixed community reports across DiLink generations; not a certified model support list |
| Current release | Car hotspot confirmed; Wi-Fi Direct improved, occasional audio cutouts remain (DiLink5.1 tests) |
| Wi-Fi | Prefer 5 GHz without an established station connection; align to a supported existing station channel; explicit 2.4 GHz fallback for firmware that rejects 5 GHz or automatic channel selection |
| Video | Default H.264 / 30 fps; 60 fps and HEVC increase device-specific demands |

## Car hotspot

Car hotspot starts CarPlay on the development car using scoped IPv6. The phone must join the configured car hotspot. Neither result guarantees support on every firmware. The BYD instrument-cluster and HUD integration is removed in this fork; see [BYD_REMOVAL_AUDIT.md](BYD_REMOVAL_AUDIT.md).

## Wi-Fi Direct on Android 9

This fork asks the framework for the channel the user selected before it creates the group. Android 9 exposes no public way to request an operating frequency or to read the one the group ended up on, so the manager calls the hidden `WifiP2pManager.setWifiP2pChannels` and reports the requested channel as unverified instead of pretending it was confirmed. Verified on a Redmi K20 Pro running Android 9: channel 149 (5 GHz, 5745 MHz) with an iPhone joining as a legacy client and completing a full CarPlay session.

The accessory address sent to the iPhone over iAP2 is IPv4 for Wi-Fi Direct and LocalOnlyHotspot. Both are access-point style interfaces that the app itself owns, and on some Android 9 firmwares the link-local IPv6 route of such an interface is installed in a per-network policy table that no routing rule selects. The iPhone then reaches the accessory and never receives a reply, so the connection fails with no error on either side. Carrier hotspot mode keeps using IPv6: there the car owns the network and the phone is a client of it.

## Known limitations

- Some units stutter, particularly under higher video load. A 2.4 GHz link alone does not prove the cause: interference, firmware and decoder stalls can all contribute. Try Default icons, 30 fps and a lower resolution, then attach a report.
- Some iOS/head-unit combinations do not visibly apply icon and text size. Reconnection is implemented; that does not guarantee the iPhone chooses the requested layout.
- A radio that supports joining a 5 GHz network may still reject a 5 GHz Wi-Fi Direct group. The capability flag is diagnostic, not proof of group-owner support.
- Automatic startup depends on the car's firmware and startup permissions.
- USB requires a data port and correct host/device-role behavior.
- Calls, Siri, background reconnection, long journeys and future iOS releases need broader testing.

Reports record requested and actual frequencies, station association state, fallback failures and remembered-configuration events. Wi-Fi credentials and protocol payloads are excluded. A successful hotspot is not itself a successful CarPlay session.

Android references: [SupplicantState](https://developer.android.com/reference/android/net/wifi/SupplicantState), [explicit P2P operating frequency](https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pConfig.Builder#setGroupOperatingFrequency(int)).
