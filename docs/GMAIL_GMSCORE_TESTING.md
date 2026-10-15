# Gmail GmsCore research (experimental)

## Scope

- Gmail package: `com.google.android.gm`.
- `Gmail registration (experimental)`: only registers the app in Morphe; does not change the APK.
- `Gmail GmsCore metadata (experimental)`: adds a GmsCore package visibility query, the fake-signature permission and signing metadata to `AndroidManifest.xml`. It also requires the original signing certificate; it does not alter DEX bytecode.
- Both patches are **disabled by default** and have **no validated Gmail APK**.
- **Selected research target** for the metadata patch only: `2026.09.28.994593356.Release`, versionCode `66072052`, package `com.google.android.gm`. The registration-only patch remains usable for version discovery.
- APKMirror publishes both standalone APK and split APKM variants. Metadata patch recommends the standalone APK to simplify inspection, but this does **not** mean the standalone or split variants have been tested.
- APKMirror-listed original Google certificate SHA-1: `38918a453d07199354f8b19af05ec6562ced5788`; SHA-256: `f0fd6c5b410f25cb25c3b53346c8972fae30f8ee7411df910480ad6b2d60db83`. Verify original APK fingerprints rather than assuming a file was signed by Google; alternative legitimate signing certificates exist.

Source: https://www.apkmirror.com/apk/google-inc/gmail/gmail-2026-09-28-994593356-release-release/

**Not supported yet:** Google account sign-in, Gmail sync, push notifications/FCM, Android Play Integrity, server-side authentication, or any guarantee of compatibility. This is an investigation step, not an operational Gmail mod.

## Before patching

1. Use a legitimately obtained **unmodified** Gmail APK/APKS, ideally `2026.09.28.994593356.Release` (`66072052`), and record its versionName, versionCode, file format and signing certificate. Re-signed patched APKs are not suitable as the original signing-certificate source.
2. Do **not** uninstall, replace, or clear the data of the official Gmail app on a personal phone. Check unsynced drafts first.
3. For inspection only, choose **Gmail GmsCore metadata (experimental)** and **Provide Original app certificate**; do not combine it with the upstream universal **GmsCore support (MicroG)**. Choose the package name actually used by your installed MicroG RE/GmsCore.
4. Generate the APK without installing it. Inspect the resulting manifest and confirm that the three metadata keys, the permission and the package query were added correctly.
5. Use a disposable test device or emulator and a test account only after verifying the build. Signing-key conflicts prevent safely installing it as an update to the official Gmail.

## Validation checklist

- Does the patch build without exception?
- Is the APK manifest parseable and the package name still `com.google.android.gm`?
- Are the added metadata values correct and unique?
- Can the test app open without crashing? (Not established.)
- Can the test account sign in, sync, and receive FCM? (Not established; a future version-specific investigation.)

## Gather diagnostic logs (Windows PowerShell)

```powershell
adb logcat -c
# On the TEST device: reproduce the issue.
adb logcat -d -v time > gmail-test-log.txt
Select-String -Path .\gmail-test-log.txt -Pattern "FATAL EXCEPTION|AndroidRuntime|SecurityException|GmsCore|FirebaseMessaging" -Context 3,15
```

Redact email addresses, access tokens and account identifiers before sharing logs.

## Next engineering milestone

Request the original Gmail APK or base.apk for the specified version, a clean `AndroidManifest.xml`, relevant DEX methods, and redacted logcat. Only then design Gmail-specific fingerprints rather than applying the upstream universal string rewriting to an untested APK.
