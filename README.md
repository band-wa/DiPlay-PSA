# DiPlay-PSA

**CarPlay for Peugeot and Citroën (PSA) Android 9 head units.** Wired USB, car hotspot and Wi-Fi Direct — including 5 GHz Wi-Fi Direct on Android 9. Independent app: `com.shihab.diplay`.

DiPlay-PSA is a fork of [DiPlay](https://github.com/shihabal3amri/DiPlay): the CarPlay protocol, audio, video and touch paths are kept upstream, while the vendor-specific head-unit integrations and the Android 9 wireless limitations are what this fork reworks.

![DiPlay home](site/assets/home.png)

## Base

- Built on [DiPlay 0.2.10](https://github.com/shihabal3amri/DiPlay/releases/tag/v0.2.10) (2026-10-03), the release this fork diverged from, including the generic wireless, media, settings and diagnostic fixes it carried.
- Fork version `0.2.11-psa.x`; the current build is **0.2.11-psa.2**.
- Upstream is followed read-only: only small, confirmed patches are cherry-picked ([FORK_POLICY.md](docs/FORK_POLICY.md)). The IPv4 accessory address and the Android 9 Wi-Fi Direct channel support below started in this fork and were merged upstream (PRs #282, #283); their later hardening is imported back.

## What this fork changes

### PSA adaptation

- Removes the BYD/DiLink head-unit integrations — instrument cluster, windscreen HUD, launcher map card, vehicle data and network ADB — together with their settings, resources and tests. The default OEM label is PSA; CarPlay audio, video, touch and the wireless connection paths are unaffected ([BYD_REMOVAL_AUDIT.md](docs/BYD_REMOVAL_AUDIT.md)).
- Offers local offline authentication only; the CH341 bridge, I2C and remote authentication sources are not part of this fork.
- Documents the development head unit's Wi-Fi Direct firmware defect: its vendor `WifiP2pServiceImpl`, a HiCar adaptation, crashes the framework when a phone joins the group without P2P discovery, so on that unit the car hotspot or USB are the options ([COMPATIBILITY.md](docs/COMPATIBILITY.md)).

### Android 9 wireless

- **Wi-Fi Direct on Android 9**: when the user selects a channel, the group's operating channel is pinned through the hidden Wi-Fi P2P channel request before the group is created, and reported as unverified because Android 9 cannot read the frequency back. Verified end to end on Android 9: an iPhone joining a 5 GHz group on channel 149 completes a full CarPlay session ([ANDROID9_WIFI_DIRECT.md](docs/ANDROID9_WIFI_DIRECT.md)).
- **IPv4 accessory address**: the address sent to the iPhone over iAP2 and Bonjour is the interface's IPv4 address whenever it has one — for Wi-Fi Direct and LocalOnlyHotspot groups the app owns, and for a car hotspot the head unit owns. Some Android 9 firmwares route the link-local IPv6 address of such interfaces into a policy table no rule selects, where an IPv6-only endpoint never opens the AirPlay connection.

### CarPlay experience

- In-session settings menu opened with the configured swipe gesture, with system bar controls and safe-area edits preserved when the menu is cancelled.
- Live picture adjustments (brightness, contrast, saturation, warmth) applied as a colour matrix on the video texture, with an original/comparison switch and a reset.
- Multi-window and split-screen layouts with compact cards, instant GPU matrix scaling, preserved video geometry and an optional resolution adaptation toggle.
- Album art kept between tracks, with a neutral placeholder instead of a stale cover.
- CarPlay preparation screen fitted to short landscape displays, respecting system bars and cutouts.
- Wireless startup recovery: a half-raised car hotspot interface is awaited until it is usable, and a session where the iPhone never opens the AirPlay control connection is retried with backoff and a manual retry button.
- USB attach filter matches Apple devices, and an Accessibility service confirms DiPlay's own Android USB permission dialog.
- Diagnostics export without a head-unit file picker: reports are saved in the app or under Android/data and can be shared from there.
- R8 code and resource shrinking for a small release APK, keeping original class and enum names so exported reports stay readable.

## Requirements

- Head unit: Android 9 or newer, with permission to install APKs.
- Phone: standard, non-jailbroken iPhone with CarPlay enabled.
- Connection: wired USB, the car's own hotspot, or Wi-Fi Direct. No jailbreak, dongle, Mac, account or authentication server.

This is **not an Apple-certified product**. The APK bundles an experimental accessory identity recovered from public Carlinkit firmware, which is extractable; acceptance after future iOS updates is not guaranteed. See [COMPATIBILITY.md](docs/COMPATIBILITY.md) for scope and troubleshooting.

## Install and build

- [Install and connect](docs/INSTALL.md) — install on the car, not the iPhone.
- [Build from source](docs/BUILD.md) — JDK 17+, Android SDK Platform 37, NDK 28.2.13676358; release builds need `DIPLAY_AUTH_ASSETS_DIR` and a signing key.

## Documentation

- [Compatibility and troubleshooting](docs/COMPATIBILITY.md)
- [Android 9 Wi-Fi Direct](docs/ANDROID9_WIFI_DIRECT.md)
- [Wireless diagnostics](docs/WIRELESS_DIAGNOSTICS.md)
- [Connection setup](docs/CONNECTION_SETUP.md)
- [Privacy and diagnostic reports](docs/PRIVACY.md)
- [Validation](docs/VALIDATION.md)
- [Fork policy](docs/FORK_POLICY.md)
- [Upstream README](docs/UPSTREAM-README.md) · [Changelog](CHANGELOG.md) · [Credits and licenses](docs/THIRD_PARTY_NOTICES.md)

## Source and credits

Based on [xcertplay](https://github.com/shilapi/xcertplay) and [DiPlay](https://github.com/shihabal3amri/DiPlay), GPL-3.0. The home/settings UI and website adapt [DiAuto](https://github.com/shihabal3amri/DiAuto), AGPL-3.0; that license is included in `docs/licenses`. Preserve those notices when distributing modifications. CarPlay and its icon belong to Apple Inc.; no Apple or Stellantis/PSA affiliation or endorsement is implied.
