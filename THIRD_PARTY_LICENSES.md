# Third-Party Notices & Open Source Licensing

## Telegram Web K
- **Project**: Telegram Web K (tweb)
- **Original Source**: https://github.com/TelegramOrg/Telegram-web-k
- **Original Author**: Igor Zhukov <igor.beatle@gmail.com> & Telegram Authors
- **License**: GNU General Public License v3.0 (GPL-3.0)

### Modification Notice:
This project contains a modified, buildable fork of the official Telegram Web K source at `third_party/telegram-web-k/`. Its upstream revision is recorded in that directory's `FOCUS_GUARD_STUDY_MODE.md`. The Android app packages the locally built frontend; it does not load Telegram's hosted Web K application.
Modifications include:
1. **Restricted Study Mode (Kiosk Logic)**: Enforces access exclusively to verified, administrator-approved educational channels and groups.
2. **Suppression of Non-Study Elements**: Restricts left column chat list, global search, contact discovery, calls, stories, bots, mini-apps, and direct unapproved messaging feeds.
3. **Link Interception & Verification**: Intercepts in-chat links and external deep links (`t.me`, `tg://`), preventing unapproved navigation and requiring explicit administrative approval before adding new destinations to the approved chat registry.
4. **Input Filtering & Safety**: The Android host retains the Focus Input Filter and applies its navigation checks to the bundled client.
5. **Source Distribution**: The modified Web K source, upstream license, and build instructions are included in `third_party/telegram-web-k/`; `scripts/build-focus-study-webk.sh` rebuilds the Android assets from that source.

### Disclaimer:
This application is **NOT** an official Telegram application and is not endorsed or certified by Telegram FZ-LLC. It is an independent, restricted study integration built solely for distraction-free medical education.

Full GPL-3.0 license text is available in the LICENSE file and at https://www.gnu.org/licenses/gpl-3.0.txt.
