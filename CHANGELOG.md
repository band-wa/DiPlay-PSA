# DiPlay PSA fork — 0.2.11-psa.2

Fork of [DiPlay](https://github.com/shihabal3amri/DiPlay) aimed at Peugeot/Citroën PSA head units running Android 9. The CarPlay protocol, audio, video and touch paths stay upstream; the vendor-specific head-unit integrations do not.

- Remove the BYD instrument-cluster, windscreen-HUD, launcher-map-card, vehicle-data and network-ADB integrations together with their settings, resources, tests and documentation. The default OEM label is now PSA; CarPlay audio, video, touch and wireless connection paths are unaffected ([BYD_REMOVAL_AUDIT.md](docs/BYD_REMOVAL_AUDIT.md)).
- Fix connection settings saved while the projection screen was in the background: the screen is resumed with FLAG_ACTIVITY_REORDER_TO_FRONT, so a Wi-Fi Direct choice made on the settings screen was ignored until the app was recreated.
- Offer local offline authentication only. The CH341-bridge, I2C and remote authentication sources are not offered here; the identity requirements for builds are unchanged (see [docs/BUILD.md](docs/BUILD.md)).
- Enable R8 code and resource shrinking for the mobile release build, keeping original class and enum names (`-dontobfuscate`) so in-app diagnostics and exported reports stay readable on a head unit.
- Open the settings menu from inside a running session with the configured swipe gesture, including system bar visibility, and restore safe-area edits made in the menu when it is cancelled.
- Adjust the CarPlay picture while it is on screen: brightness, contrast, saturation and warmth are applied as a colour matrix on the video texture, with an original/comparison switch and a reset. Nothing is renegotiated, restarted or applied to the head-unit user interface.
- Fit the CarPlay preparation screen into short landscape displays and respect system bar and cutout insets, so the title, instructions and buttons stay fully visible on head-unit screens.
- Support multi-window and split-screen layouts with compact cards, instant GPU matrix scaling, preserved video geometry, and an optional multi-window resolution adaptation toggle that stays off by default.
- Export diagnostics without a head-unit file picker: reports are saved inside the app and can be viewed or copied there.
- Add an Accessibility service that confirms DiPlay's own Android USB permission dialog, restricted to system USB dialogs for this app.
- Merge upstream generic fixes: settings swipe finger count, location permission flow, media metadata de-duplication, Android 9 AudioTrack property lookup, settled display size, wireless diagnostics with one guarded Android 10 Wi-Fi Direct recovery, codec release and media failure diagnostics, theme observation with own-app process-exit metadata, own-package VPN scope (#168) and preferred Wi-Fi Direct channel selection (#175).
- Wait for a car hotspot interface that is genuinely usable before wireless startup, so a half-raised access point no longer fails the connection with a wrong address.
- Recover wireless startup when the iPhone accepts Bluetooth and the hotspot but never opens the AirPlay control connection: the StartSession send now arms a bounded deadline, and failed attempts are retried with backoff, reset after a stable session, and reported with a manual retry button.
- Save diagnostics reports under Android/data when no head-unit file picker is available, and run the export tests on Windows hosts as well.
- Match Apple devices in the USB attach filter so a connected iPhone is offered to DiPlay.
- Keep the current album art between tracks and show a neutral placeholder instead of a stale cover.
- Fix Simplified Chinese wording and spacing, including the hotspot description that asked users to use the head unit's own hotspot.

# DiPlay 0.2.10 — 2026-10-03

- Publish CarPlay song metadata, position and artwork to Android media sessions; bound artwork queues and reject stale work across sessions (#82).
- Preserve normal USBMUX frames while handling narrowly validated handshake padding (#114); let USB connect without saved wireless-hotspot credentials (#130).
- Handle unknown reported Wi-Fi Direct security types, retry busy channels and allow bounded 5 GHz fallback (#121).
- Select an available AirPlay port and advertise it over Bonjour and wired/wireless iAP2; close sockets on failed setup/notification (#143).
- Enable available platform echo cancellation and noise suppression for calls, restoring the previous mode afterward (#116).
- Detect BYD CAN/CANFD battery protocols and clear unsupported/stale readings (#123).
- Add a saved show/hide setting for the home-screen dashboard-map mirror (#133).
- Improve optional parked video with seeking and ten-second skip controls; validate media URLs and redirects (#129).
- Extend Ukrainian translations, including the new map-mirror setting (#128 and release localization).
- Add bounded anonymous Bluetooth/USB/boot and microphone capture/encode/send diagnostics to exported reports; omit audio and packet contents.

See [0.2.10 release notes](docs/RELEASE-NOTES-0.2.10.md) for contributor credits, requirements and validation limits. Android 9 remains the minimum supported version.

# DiPlay 0.2.9 — 2026-10-02

- Follow BYD head-unit day/night changes while CarPlay is visible, including firmware that does not reliably deliver Android configuration callbacks.
- Restore media and navigation audio stream selection to 0–20 and inherit older saved navigation settings when no new selection exists. Vendor-specific outputs depend on head-unit support.
- Keep CarPlay connected through normal surround-view window changes, preserving video proportions and touch alignment. A connection started in a narrow camera window reconnects once when the window grows to restore the full-screen canvas.
- Add Ukrainian to the app language picker, Android app-language settings, and website. Correct its audio help to describe streams 1–20.
- Add an optional CarPlay song title, artist, and play/pause display on the BYD instrument cluster, using the existing network ADB connection.
- Add a CarPlay navigation widget for launchers that host standard Android widgets, with the next turn, road, distance, arrival information, and song. Clear expired guidance and explicitly cleared song titles.
- Add an optional floating copy of the dashboard map on the centre screen, with drag, pinch-to-resize, and tap-to-open controls. Requires permission to draw over other apps; Usage Access restricts it to home screens.
- Fix floating-map resizing on head units that ignore small pinch gestures.
- Let compatible launchers embed the live dashboard map on Android 11 and newer. Sharing is off by default; turning it off closes existing shared map views.
- Add map-host and DiPlay Home sample apps for developers. DiPlay Home combines the live map, standard Android widgets, a clock, and an app list; sample builds, lint, and Home back-navigation tests are checked in CI.
- Leave the GPS course empty when its direction is unknown, instead of reporting north. Valid GPS directions are preserved.
- Thanks to @lpcheng1208 for PRs [#71](https://github.com/shihabal3amri/DiPlay/pull/71), [#88](https://github.com/shihabal3amri/DiPlay/pull/88), and [#89](https://github.com/shihabal3amri/DiPlay/pull/89).
- Thanks to @romanchukg-cloud for PRs [#93](https://github.com/shihabal3amri/DiPlay/pull/93), [#101](https://github.com/shihabal3amri/DiPlay/pull/101), [#105](https://github.com/shihabal3amri/DiPlay/pull/105), [#106](https://github.com/shihabal3amri/DiPlay/pull/106), [#107](https://github.com/shihabal3amri/DiPlay/pull/107), [#108](https://github.com/shihabal3amri/DiPlay/pull/108), and [#109](https://github.com/shihabal3amri/DiPlay/pull/109).

See [0.2.9 release notes](docs/RELEASE-NOTES-0.2.9.md) for the merged changes and validation limits.

# DiPlay 0.2.8 — 2026-09-30

- Keep iPhone location reporting active across the wireless Bluetooth-to-Wi-Fi CarPlay handoff; limit location updates to one per second on wireless and USB.
- Add optional ADB wheel-speed and gear reporting for iPhone dead reckoning when GPS is unavailable. Tunnel use has not yet been verified.
- Add optional iOS 27 video playback on the car screen while parked, with iPhone, touchscreen and steering-wheel controls; close playback when leaving P.
- Explain unsupported DRM-protected video such as Apple TV+, which requires a licensed FairPlay receiver.
- Improve playback error reporting and preserve CarPlay when the head unit cannot play a video.

# DiPlay 0.2.7 — 2026-09-29

- App interface in English, Simplified Chinese, Arabic, Russian and Spanish; synchronized Android app-language settings.
- Steering-wheel media controls and long-press Siri on supported BYD firmware while CarPlay is on screen.
- Dashboard display choices: map, turn card, or both; corrected dashboard keyframe recovery.
- Optional ADB feature on supported DiLink 5.0: pause the dashboard map stream when its display mode hides the map.
- Optional ADB battery reporting for Apple Maps, with warning threshold, charging-connector selection and a checked reconnect action.
- Audio playback reliability fixes and clearer dashboard settings.
- Clarify the BYD-only support scope on the README and all five website editions.

# 0.2.0 — BYD navigation and connection improvements

- Standalone windshield HUD arrows, distance and street names on the verified DiLink5.1 firmware; no ADB, root or computer helper.
- Retain contributor cluster/SOME-IP navigation, route parsing, BYD CarPlay icon and display-size presets.
- Fix Car hotspot startup by using scoped IPv6 when available and binding discovery/probing to the AP interface. Physically confirmed on the development car.
- Drain asynchronously decoded audio during packet gaps and rebuild the music buffer after starvation. Wi-Fi Direct is much better in the user retest; occasional audio cutouts remain for a later version.
- Preserve bounded music-buffer choices, USB read improvements and decoder recovery; fix USB request/close races and keep vendor output outside phone callbacks.
- Save audio/video/receive timing and discovery diagnostics without road names or protocol payloads.
- HUD cleanup on normal end/disconnect/off/stale input; interrupted sessions recover on the next app launch. Force-stop may leave guidance visible until reopening.
- Thanks to @romanchukg-cloud and @georgiyrr for PR #3 and vehicle testing.

# 0.1.0 release restored — 2026-09-25

- Rebuilt and signed the APK locally with explicitly supplied runtime authentication assets.
- Restored release downloads; no app behavior or version-code change from 0.1.0.
- Accessory identity remains in the APK only. No credential files enter Git or the source archive.
- Retained generated test identities and public-source credential checks.
- Source/CI builds omit runtime identity assets by default; local packaging requires an explicit external directory.

# Source reset — 2026-09-25

- Withdrew the 0.1.0 APK and removed its release tag.
- Reset the public branch after preserving restricted local incident records.
- Removed static synthetic test private keys; generate test identities at runtime.
- Removed automatic private-asset packaging and disabled the old release build script.
- Added a build guard rejecting credential asset files.
- Replaced the download site with a five-language suspension notice.

The APK was subsequently rebuilt and restored as described above. Existing copies cannot be recalled by a Git history reset.
