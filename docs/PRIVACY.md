# Privacy and diagnostics

DiPlay's product flow uses local authentication and a direct USB/Wi-Fi connection to the iPhone. No account, remote authentication service or automatic diagnostic upload is used. The iPhone's CarPlay apps have their own internet and privacy behavior.

The head unit stores app preferences, paired-device selections, pairing data and bounded diagnostic logs in app storage. Authentication and pairing material are kept out of Android backup. Uninstalling removes app-private data; exported reports in Downloads remain until you delete them.

Diagnostic export is initiated by you. Reports include app/device versions, display settings and negotiation, connection transitions, Wi-Fi band/channel and state, and decoder recovery events. The exporter filters protocol payloads, credential-bearing lines and common identifiers. Redaction cannot promise to recognize every vendor-specific string: review reports before posting them publicly. A GitHub issue is public.

Microphone access supports Siri and calls. Bluetooth/Nearby devices and Wi-Fi/Location permissions support discovery and transport. The optional local VPN permission supports the USB link; it does not provide a remote internet VPN.

This fork declares neither Usage Access nor the launcher map-embedding service. The optional centre map card draws only with the "display over other apps" permission, and app-activity history is never read or retained (see [BYD_REMOVAL_AUDIT.md](BYD_REMOVAL_AUDIT.md)).

The static website has no analytics script or account. GitHub Pages, GitHub and Telegram apply their own policies when you use those services.
