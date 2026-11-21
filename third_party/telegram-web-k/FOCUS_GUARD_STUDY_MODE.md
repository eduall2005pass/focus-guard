# Focus Guard Study Mode fork

This directory contains a modified copy of Telegram Web K from
https://github.com/TelegramOrg/Telegram-web-k.

- Upstream revision: `2e4bbb52b6aec5e95ee32e5c2882c1f05de32200`
- Upstream license: GNU GPL version 3; see `LICENSE`.
- Build integration: `package.json` adds `build:focus-study`; the root Vite
  config keeps this packaged build free of source maps.
- Android packaging: `../../scripts/build-focus-study-webk.sh` generates the
  compiled files under `app/src/main/assets/telegram-web-k` and copies the
  upstream `public/assets` runtime files to `app/src/main/assets/focus-web-public`.

The Focus Guard modifications add `src/lib/focusStudyMode.ts`, which obtains
Telegram peer IDs and peer types from resolved Web K entities, checks each
peer against the Android bridge's exact approved `type:id` set, and reports
unknown peers to the native approval UI. Checks are added at Web K's entity
open and conversation-setting boundaries in `src/lib/appImManager.ts` and
`src/components/chat/chat.ts`. Bots and direct user chats are denied, and
Web Apps are disabled. `src/scss/focusStudyMode.scss` hides distracting client
surfaces. Login completion is reported from the actual Web K auth transition
in `src/index.ts`.

These changes are maintained as a small patch over the pinned upstream source.
The client still uses Telegram's network services for account login and chat
data; its approved-chat restrictions are specific to the Focus Guard client.
