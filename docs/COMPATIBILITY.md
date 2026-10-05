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

The address sent to the iPhone over iAP2, and the address in the accessory's Bonjour advertisement, is the interface's IPv4 address whenever it has one: for the Wi-Fi Direct and LocalOnlyHotspot groups the app owns, and for a car hotspot the head unit owns. Some Android 9 firmwares keep the link-local IPv6 route of an access-point interface in a per-network policy table that no routing rule selects, so an accessory that publishes an IPv6 link-local address alone is unreachable: on the PSA head unit `ip -6 route get` for the phone's link-local resolved through the upstream Wi-Fi client interface rather than the access point, whose `fe80::/64` route sat in an unselected table. The scoped IPv6 link-local address is now used only when the interface has no IPv4 address.

### Head units whose Wi-Fi Direct service crashes

Some Android 9 head units ship a modified `WifiP2pServiceImpl` that fails as soon as a station joins the group. The framework process dies and the head unit restarts, which the user sees as DiPlay closing; nothing in DiPlay raises this.

```text
E AndroidRuntime: *** FATAL EXCEPTION IN SYSTEM PROCESS: WifiP2pService
E AndroidRuntime: java.lang.NullPointerException: Attempt to write to field
E AndroidRuntime:   'java.lang.String android.net.wifi.p2p.WifiP2pDevice.interfaceAddress' on a null object reference
E AndroidRuntime: 	at com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine$GroupCreatedState.processMessage(WifiP2pServiceImpl.java:2674)
```

Check a unit with `adb logcat -b crash | grep -A3 "FATAL EXCEPTION IN SYSTEM PROCESS: WifiP2pService"`. The development car that reproduces this is a Spreadtrum/Unisoc `alps/full_spm8666p1_64` head unit (model `3VS121`), the target for this fork.

AOSP handles the same event defensively, because a station that never performed Wi-Fi Direct discovery is not in the framework's peer list:

```java
if (mPeers.get(deviceAddress) != null) mGroup.addClient(mPeers.get(deviceAddress));
else                                   mGroup.addClient(deviceAddress);   // a legacy client
```

The vendor build keeps those lines and then writes the station's interface address into a peer object it never checked, as disassembling `wifi-service.jar` from the unit shows:

```smali
13601e: if-eqz v2, 0053              ; AOSP's null check
13605c: invoke-virtual {v2, v1}, WifiP2pDeviceList;.get:(Ljava/lang/String;)Landroid/net/wifi/p2p/WifiP2pDevice;
13606e: move-result-object v2         ; no null check before the write
136070: iget-object v5, v0, Landroid/net/wifi/p2p/WifiP2pDevice;.interfaceAddress:Ljava/lang/String;
136074: iput-object v5, v2, Landroid/net/wifi/p2p/WifiP2pDevice;.interfaceAddress:Ljava/lang/String;
```

An iPhone joins as a legacy client: it receives the hotspot name and passphrase over iAP2 and associates as an ordinary WPA2 station, so it is never in that peer list and the write fails on the first connection. Any other station would do the same. DiPlay cannot work around it, because the peer list has no public write path, so on such a unit the car hotspot or USB are the only options.

## Known limitations

- On the head units whose Wi-Fi Direct service crashes (above), selecting Wi-Fi Direct restarts the framework rather than failing the connection: use the car hotspot or USB there.
- Some units stutter, particularly under higher video load. A 2.4 GHz link alone does not prove the cause: interference, firmware and decoder stalls can all contribute. Try Default icons, 30 fps and a lower resolution, then attach a report.
- Some iOS/head-unit combinations do not visibly apply icon and text size. Reconnection is implemented; that does not guarantee the iPhone chooses the requested layout.
- A radio that supports joining a 5 GHz network may still reject a 5 GHz Wi-Fi Direct group. The capability flag is diagnostic, not proof of group-owner support.
- Automatic startup depends on the car's firmware and startup permissions.
- USB requires a data port and correct host/device-role behavior.
- Calls, Siri, background reconnection, long journeys and future iOS releases need broader testing.

Reports record requested and actual frequencies, station association state, fallback failures and remembered-configuration events. Wi-Fi credentials and protocol payloads are excluded. A successful hotspot is not itself a successful CarPlay session.

Android references: [SupplicantState](https://developer.android.com/reference/android/net/wifi/SupplicantState), [explicit P2P operating frequency](https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pConfig.Builder#setGroupOperatingFrequency(int)).
