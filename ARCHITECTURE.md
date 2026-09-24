# Architecture

G1 Z4 Control Pro is one Android app. The path is fixed:

**Hardware → Protocol → Communication → Parser → Repository → ViewModel → UI**

Nothing in the UI invents a state the layers below did not record.

## Modules

| Layer | Where | Responsibility |
| --- | --- | --- |
| Hardware facts | `G1_Z4_PROTOCOL_SPECIFICATION.md` | What the dialer and panel actually do. |
| Protocol | `protocol/` | Build ASCII commands, parse replies, decide confirm / reject / unsolicited / unverified, fold Persian digits, Jalali dates, PIN hash, phone keys. |
| Communication | `communication/SmsEngine.kt` | Queue one SMS, enforce permission, match the reply, expire to unconfirmed, ignore other numbers. |
| Persistence | `data/db`, `data/security`, `data/settings` | Room v1, Keystore vault, DataStore. |
| Repository | `data/repo/PanelRepository.kt` | Devices, zones, outputs, remotes, events. Seed children from the saved wiring. Never upgrade an unknown snapshot. |
| ViewModel | `ui/vm/AppModel.kt` | Session, settings, send requests, export. |
| UI | `ui/screens`, `MainActivity` | Persian RTL Compose. Shows unknown and unsupported instead of a fake dashboard. |

## Process

1. The user confirms a sensitive command. Biometric is a second step when the phone has it and the user enabled it.
2. `CommandBuilder` refuses a bad password or a non-ASCII body.
3. `SmsEngine` writes a `QUEUED` row, sends, then `WAITING`.
4. The receiver or the inbox scan calls `ingest`. `CommandPolicy.match` either closes the command or leaves it waiting.
5. A trigger SMS is stored even if a command is in flight, and it does not close that command.
6. `expireStale` runs at launch and after a send. A waiting command past the timeout becomes `UNCONFIRMED`.
7. The snapshot is updated only from a non-synthetic parse. Missing fields stay null or `UNKNOWN`.

## Multi-device

Each device has its own phone key, password, wiring map, zones, and snapshot. The active device is a DataStore id. An incoming SMS selects the device by phone key, not by whichever screen is open.

## What is intentionally absent

No account server, no Firebase, no mock catalog in release, no part-set command, no remote-learn command, no event-log download, no battery-voltage query. Those would be fiction.

## Build

Kotlin, Compose, Material 3, Hilt, Room, Coroutines. `minSdk` 26, `compileSdk` / `targetSdk` 35. Release minify is on. Signing uses `keystore/release.jks` when that file exists; CI creates it before assemble. The internet permission is removed.
