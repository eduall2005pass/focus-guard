# Study Portal architecture and feasibility

## Study Portal

- `DevicePolicyController` and the approved-app registry provide Device Owner
  and blocked-app enforcement.
- `StudyPortalScreen` stores approved websites and Telegram destinations in
  app-private files. Its initial website list includes GTC, BoiMate, Medical
  Contents, NotebookLM, MuslimBangla, iHadis, and LiveMCQ.
- `ControlledStudyWebView` uses Android System WebView, persistent WebView
  cookies/storage, the Focus-Control input filter, a navigation guard, and
  DownloadManager for URL downloads.
- Telegram links are parsed natively; unresolved links open the bundled client
  to resolve Telegram's actual peer metadata before administrator approval.

## Modified Telegram Web K

The app includes a source fork at `third_party/telegram-web-k/`, based on the
official Telegram Web K repository and pinned to the commit in
`FOCUS_GUARD_STUDY_MODE.md`. `scripts/build-focus-study-webk.sh` typechecks and
builds the modified client, then packages it into the Android app. At runtime,
Android serves the local files through `WebViewAssetLoader`; it does not load
the hosted `web.telegram.org/k` frontend.

The fork checks the resolved Telegram peer ID and peer type in the entity
resolution and conversation-opening paths. The native bridge supplies the
approved `type:id` list, blocks users and bots, and sends unknown group/channel
peers to the native credential-protected approval screen. CSS hides controls
such as global search, contacts, calls, stories, and chat lists; the peer checks
control which conversations can open.

Telegram login, messages, and media still require a network connection.
Restrictions apply only to the bundled Focus Guard client, not other Telegram
apps, browsers, or devices. Samsung device verification remains necessary for
login, session restore, forwarded links, media playback, downloads, and device
owner behavior. The app-private session and approval registry survive ordinary
app upgrades but are removed when the app is uninstalled.

## Other constraints

- Website main-frame navigation uses the approved-site registry; subresources
  use the existing support-host policy.
- Device credential checks protect native approval changes and require a
  secure screen lock and Device Owner setup.
- Public usernames and invite links do not themselves establish verified peer
  IDs or peer types; only client-resolved peers may be approved.

## Upstream references

- https://github.com/TelegramOrg/Telegram-web-k
- https://github.com/TelegramOrg/Telegram-web-k/blob/master/README.md
- https://github.com/TelegramOrg/Telegram-web-k/blob/master/AGENTS.md
