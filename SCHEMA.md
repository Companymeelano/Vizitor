# Schema

Room database `g1z4.db`, version 1, `exportSchema` on. There is no destructive migration fallback. A future version must ship a `Migration`.

## Tables

| Table | Key | Contents |
| --- | --- | --- |
| `devices` | `id` | Name, phone raw and key, password ciphertext, panel, dialer, wiring, arm/disarm outputs, zone counts, subscription, operator, balance formula, recharge template, optional `*22` flags, timeout, query retry, output-4 flag, created time. |
| `zones` | `deviceId` + `key` | `WIRED-n`, `WIRELESS-n`, `EXPANDER-n`. Local name, note, `UNKNOWN` or `TRIGGERED`, last trigger. |
| `outputs` | `deviceId` + `number` | Local name, reserved-for label, whether a report has been seen, last on/off, time. |
| `remotes` | `deviceId` + `slot` | Slots 1–15. Local name and last time a real SMS mentioned that slot. |
| `events` | `id` | Masked body, severity, title, synthetic flag, kind. Duplicate window is 30 seconds on device + masked body. |
| `alerts` | `id` | Notification rows. Deleted with the device. |
| `commands` | `id` | Kind, phase, sent and finished times, masked response, Persian error, output number, synthetic flag. |
| `snapshots` | `deviceId` | Arm, power, siren, credit mention, phone-cut, jammer, battery mention, link phase. Missing values stay null / `UNKNOWN`. |

New devices start from `Snapshot.unknown`. Seeding zones does not mark them closed.

## Export JSON, schema 1

`AppModel.exportJson()` writes:

```json
{
  "schema": 1,
  "note": "پشتیبان محلی. رمز دستگاه‌ها عمداً صادر نمی‌شود.",
  "devices": [
    { "id": "...", "name": "...", "phone": "...", "panel": "Z4_ULTRA", "dialer": "G1_ULTRA" }
  ]
}
```

Passwords, PIN material, raw SMS, and charge codes are not in this file.

## Settings

DataStore `g1z4_settings`: theme, biometric, auto-lock seconds, sounds, notification toggles, developer flag, active device id, `pin_ready`.

The developer flag is ignored unless `BuildConfig.DEBUG` is true.
