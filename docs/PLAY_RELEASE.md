# Play release and existing-device data

The checked-in project can build a release Android App Bundle (AAB). With no release signing environment variables, `./gradlew :app:bundleRelease` produces an **unsigned** `app/build/outputs/bundle/release/app-release.aab`. It is useful for build checks but cannot be uploaded to Play. No release keystore or password belongs in this repository.

## Signing a Play bundle

1. Create an upload keystore using Android Studio's **Generate Signed Bundle / APK** flow, or use a securely held existing upload keystore. Save it outside this repository and back it up securely. Choose the Play app-signing key deliberately when setting up the app in Play Console; the upload key and installed app-signing key are different roles.
2. Set these four environment variables in the build process, using a password manager or CI secrets for passwords:

   | Variable | Value |
   | --- | --- |
   | `LITTLEWORDS_RELEASE_KEYSTORE` | Path to the upload keystore |
   | `LITTLEWORDS_RELEASE_STORE_PASSWORD` | Keystore password |
   | `LITTLEWORDS_RELEASE_KEY_ALIAS` | Upload key alias |
   | `LITTLEWORDS_RELEASE_KEY_PASSWORD` | Upload key password |

3. Run `./gradlew :app:bundleRelease`. The build fails if only some signing variables are present or the keystore path is missing. Verify the resulting AAB with `jarsigner -verify -certs app/build/outputs/bundle/release/app-release.aab`; it must report `jar verified`. Record its SHA-256 hash. The bundle's certificate is the **upload** certificate; verify Play Console's **app signing** certificate separately before an upgrade test.
4. Upload the AAB to an internal test track, install it through Play, and check launch, profile data, reading, scoring, history, and an upgrade from the previous Play build. Increase `versionCode` for each subsequent Play upload.

The release variant retains application ID `com.littlewords.app`. It currently has version code 11 and version name 1.9; check these again immediately before submitting. No production release key has been created or configured here.

## Existing family-phone installation

The supplied `deliverables/LittleWords-debug.apk` is signed with an Android debug certificate. Its SHA-256 certificate fingerprint is `81:A0:B6:62:66:AF:95:88:C9:76:F4:7C:26:84:D6:AF:05:2D:35:0A:CC:00:5E:34:95:AB:D6:7E:C6:6B:D3:92` (verified on the checked-in 1.10 APK and matching the prior 1.9 APK). Android permits an in-place update only when the application ID and signing certificate remain compatible and the version code increases. A Play build signed with a different app-signing certificate cannot replace this installation while preserving its app-private data. The AAB upload certificate alone does not determine that compatibility.

Backup and device-transfer backup are disabled. The app has no export/restore feature. Therefore **do not uninstall the debug app or clear its storage while its history is needed**. Before switching that phone to a differently signed Play build, add and test an export in a debug-signed update, save the export outside the app, and test importing it into the Play build on a separate device. Then verify all profiles, the active profile, settings, stage achievements, sessions (including any in-progress session), cards, and attempts before removing the original installation. The export/import design should include a clear user flow in `design/prototype.html` before Android implementation, under `AGENTS.md`.

For a new device with no Little Words data, install the Play build directly. For a device already on a Play build, test the normal Play-to-Play update path. Do not use the development keystore as the long-term Play app-signing key merely to make the first transition appear seamless.

References: [Android app signing](https://developer.android.com/studio/publish/app-signing), [how app updates work](https://developer.android.com/google/play/app-updates), and [build and test an app bundle](https://developer.android.com/guide/app-bundle/test).
