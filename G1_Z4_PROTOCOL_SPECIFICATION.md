# G1 / Z4 protocol specification

This file is the contract for G1 Z4 Control Pro. A behaviour that is not written here is not implemented, and a behaviour marked unverified is not shown as success.

The app is not a Classic product and does not speak for Classic. It sends SMS from the phone to the SIM inside the G1 dialer. The G1 pulses the Z4 / Z4 Ultra. There is no cloud in this path.

## 1. Sources and how conflicts were treated

Official Classic pages that used to live on `classic.co.ir` now return 404. The command table below follows the archived knowledge-base article **KB 20/44 (2021-11-30)**, which is the last unshifted official SMS table found. KB 20/39 (2022-01-17) prints the same table with columns shifted and is not used.

Other archived Classic notes used here:

- KB 20/59: trigger inputs are not the event log. T3 is the Z4 siren path. Default texts include «تحریک 1 فعال شد» and «تحریک 3 فعال شد». Suggested siren text: «هشدار! آژیر دزدگیر به صدا در آمده است.»
- KB 20/41: restart is holding the keypad `*` for about five seconds. Data is kept. Active outputs drop. This is not an SMS command. Mobile-network faults can need a 15-minute wait.
- KB 20/40, 16/54, 16/55: memory names and access are changed in Classic SETUP. Those names can appear inside G1 SMS. The Z4 Farsi app can arm, disarm, query status, and control a door, but its command strings were not published.
- Classic events page (2021-12-06): the G1 stores 200 events. No retrieval SMS was published.

A reseller Z4 Ultra page (`4zone-setting`, archived 2023-03-29) describes panel-button procedures. Those procedures are labeled panel-only. They are not turned into SMS commands.

Two charge endings disagree. KB 20/44 ends top-up with `#`. KB 20/43 (2023-02-08) ends the same top-up with `*`. Both are selectable templates. Neither is marked verified.

The I/O query is implemented as `*{password}*01`. The only place that string was seen with a normal password is a secondary installer note (`*1234*01`). The official table’s status row is printed `*1324*01`. That is treated as a password typo in the sample, not as a special command and not as a required password. The reply body of `*01` was not published.

Reseller samples that used a trailing star (`*21*`, `*22*`, `*31`) are not followed. The official table has no trailing star. `*22` is not in that table, so it is optional, off by default, and never the status button.

Observed SMS shapes, not a published grammar, come from installer screenshots: continuation with `…`, zone ranges, «خروجی N روشن/خاموش», «تحریک N», «هشدار تلفن قطع شد». The parser accepts those shapes. It does not invent the rest of the sentence.

## 2. Path

Phone SMS → SIM in the G1 → output pulse or report → Z4 / Z4 Ultra zones, siren, and outputs.

The phone is not on the panel bus. A successful G1 reply means the dialer accepted or reported something. It does not, by itself, prove the panel changed state.

## 3. Hardware limits that the app will not paper over

| Fact | App behaviour |
| --- | --- |
| Password is four ASCII digits. Factory sample is `1234` only if the installer left it. | Reject anything else. Persian digits are folded before send. |
| Wait between SMS. | One command in flight. |
| Outputs are active-low in the wiring notes. Momentary outputs last about 1.5 seconds. | The app pulses; it does not claim the relay is still on. |
| Standard wiring: panel A to OUT2, panel D to OUT3. | ARM is output 2 ON. DISARM is output 3 ON. Those outputs are reserved, not free relays. |
| Remote buttons: A arm, B disarm, C part-set, D a free output. | Part-set has no SMS command. The button is labeled «پشتیبانی نمی‌شود». |
| Remote learn is the physical LRN button, panel disarmed, capacity 15. | No learn or delete command is sent. Slots are local placeholders until a real SMS names a remote. |
| G1 memories 1–4 are SMS numbers unless SETUP changes access. Memories 21–70 can toggle OUT1 by a phone call. | The app does not edit memories. |
| Siren time, chirp, zone-4 mode, chime, and mains-sabotage warning are keypad or remote procedures. | Shown as panel-only. |
| `*00` stops dialing. It is not a proven siren-off. | Labeled توقف شماره‌گیری. |
| No published SMS starts the siren, reads battery voltage, downloads the 200-event log, or learns a remote. | Those screens say پشتیبانی نمی‌شود or نامشخص. |
| Restart is hold `*` for five seconds. | Not a button that sends SMS. |

G1 and G1+ do not add the LOG naming path. G1 Pro and G1 Ultra can. Zone names stored in this app are local and are not written back to the panel.

## 4. SMS commands that are sent

Bodies are ASCII. There is no trailing star unless the optional `*22*` switch is explicitly on.

| Action | Body | Notes |
| --- | --- | --- |
| Stop dialing | `*PPPP*00` | Not siren off. |
| I/O query | `*PPPP*01` | Reply body unpublished. |
| Output N on | `*PPPP*N1` | N is 1–4. |
| Output N off | `*PPPP*N0` | `*20` is output 2 off, not disarm. |
| ARM | `*PPPP*21` on the standard map | Output ON of `armOutput`. Not a panel-status command. |
| DISARM | `*PPPP*31` on the standard map | Output ON of `disarmOutput`. Also the only path offered to stop an alarm, and it disarms. |
| Credit query | `*PPPP*80` | |
| Store balance formula | `*PPPP*81*{formula}` | Formula is `*`, `#`, and digits. Irancell sample `*141*1#`. MCI sample `*140*11#`. |
| Top-up | one of the four templates | See below. |
| Optional panel query | `*PPPP*22` or `*PPPP*22*` | Off by default. Not the status command. |

`PPPP` is the dialer password, not the app PIN.

Top-up templates, none verified over the other:

| Id | Body |
| --- | --- |
| `irancell_hash` | `*PPPP*99*141*{code}#` |
| `irancell_star` | `*PPPP*99*141*{code}*` |
| `mci_hash` | `*PPPP*99*140*#{code}#` |
| `mci_star` | `*PPPP*99*140*#{code}*` |

The charge code is 8–20 digits. It is masked in stored text.

## 5. What a reply is allowed to mean

The dialer replies after it executes a command. The success sentence was not published. Therefore:

- Timeout, an unrelated SMS, or a sentence that does not match the command becomes `UNCONFIRMED`. The user sees «تأیید نشد» / «پاسخی که فرمان را تأیید کند دریافت نشد.»
- A rejection phrase (`رمز اشتباه`, `دستور نامعتبر`, `invalid`, `wrong password`, and the other phrases in `SmsParser`) becomes `REJECTED`: «فرمان توسط دستگاه تأیید نشد.»
- A trigger, phone-cut, or jammer SMS does not confirm a command. It is `UNSOLICITED` and is stored as an event.
- An output command, including ARM and DISARM, is confirmed only when the reply names that output number. `CommandPolicy.mayInferPanelArmed` is always false. The panel stays `UNKNOWN` until a report actually says the system armed or disarmed.
- An I/O query is confirmed only by a status or output report (`وضعیت`, or an output line). A credit sentence does not confirm it.
- Credit is confirmed only when the reply is classified as credit. Any other body stays unconfirmed.
- Stop-dial, store-formula, and top-up have no published success text, so a non-trigger reply stays `UNVERIFIED` and the command stays unconfirmed. The app does not pretend those succeeded.

Parsed shapes that are accepted when they actually appear:

- «خروجی N روشن/خاموش/فعال/غیرفعال» sets that output’s last report only.
- «تحریک N», «زون N», and a short range such as «زون ۱-۲» mark those zones triggered. Other zones stay unknown. The app never draws them as closed or healthy.
- «آژیر» plus «به صدا» or the suggested warning marks siren sounding. «آژیر» plus «قطع» marks quiet. Otherwise siren is unknown.
- «قطع برق» / «وصل برق» set mains only.
- A battery word is stored as a mention. No voltage is invented.
- «تلفن قطع» and «جمر» are connection warnings, not signal bars.
- A remote number in the text updates that slot’s last-seen time. It does not mean the other slots are empty.
- `…` or `...` marks a continued SMS. The missing tail is not guessed.

## 6. Timing and identity

- One send holds the engine lock through the send, the short inbox delay, and the optional query rescan. Incoming SMS is ingested without taking that lock, so an alarm is not dropped behind a send.
- After a real send the app waits four seconds and scans the inbox if `READ_SMS` was granted. Query commands can scan once more when query retry is on. The command still expires to `UNCONFIRMED` at the device timeout (30–180 seconds, default 90).
- A duplicate is the same device, the same masked body, within 30 seconds.
- Inbox rows are kept only when the sender matches the device number. `0912…`, `+98912…`, and `0098912…` are the same key. Another device’s number is ignored.
- Personal SMS that does not match a saved device is ignored.

## 7. Developer mode

Developer mode exists only in debug builds, behind seven taps on the about row, and after an explicit confirm. It does not send SMS. It writes a labeled «آزمایشی» event and does not copy that text into the hardware snapshot. A release build cannot turn it on. It is not a simulator of the panel and must not be described as one.

## 8. App PIN versus dialer password

The app PIN never leaves the phone. It is PBKDF2-HMAC-SHA256, 120,000 rounds, random salt, in the Android Keystore vault. Five failures lock the screen for 30 seconds. The dialer password is sealed with AES/GCM in the same vault and is absent from the JSON export.
