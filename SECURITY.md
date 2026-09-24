# Security

## Secrets

- The dialer password is AES/GCM under Android Keystore alias `g1z4.vault`, stored as ciphertext in `g1z4_vault`. It is not in Room as plaintext, not in DataStore, and not in the JSON export.
- The app PIN is not stored. `pin_salt` and `pin_hash` are PBKDF2-HMAC-SHA256, 120,000 rounds, 16-byte salt. Comparison does not stop at the first differing byte.
- Logs do not print command bodies. Stored SMS is passed through `CommandBuilder.mask`, which replaces the password and any run of 8 or more digits.
- Backup and device-transfer rules exclude shared preferences, databases, and files. `allowBackup` is false.

## Session

The unlock flag lives in memory. Process death locks the app. Auto-lock measures time since the app went to the background. Five wrong PINs lock the pad for 30 seconds. Biometric is optional and is not a replacement for the dialer password.

## SMS permissions

Requested only as needed:

- `SEND_SMS` to deliver a command.
- `RECEIVE_SMS` so an alarm can arrive while the app is not open. The broadcast is finished only after ingest returns.
- `READ_SMS` to match a reply that the broadcast missed. Rows from other numbers are dropped.
- `READ_PHONE_STATE` so a chosen SIM subscription can be used.
- `POST_NOTIFICATIONS` for alerts on Android 13+.
- `USE_BIOMETRIC` and `VIBRATE`.
- `RECEIVE_BOOT_COMPLETED` only to expire stale waiting commands. It does not send SMS.

`INTERNET` is removed. Telephony and fingerprint are not required features, so a Wi-Fi tablet can still install; it simply cannot send.

Play policy treats SMS permissions as restricted. This app is a direct SMS controller, which is the legitimate use, but a Play listing would still need the default-SMS or exemption review. Sideload does not remove the need for the user to grant the permissions.

Without `SEND_SMS`, a release command is `PERMISSION_DENIED` and nothing is sent. Debug developer mode can skip the radio, and it must label the result «آزمایشی».

## Commands

ARM, DISARM, output changes, recharge, stop-dial, and storing a USSD formula need a second confirmation. The UI must not show success from the send callback. Only `CommandPolicy` can close a command as confirmed, and even then ARM/DISARM confirmation is about the output pulse, not the panel.

## Widget

The widget reads the stored arm state off the main thread. If nothing has been reported it says نامشخص. It does not invent a last alert.
