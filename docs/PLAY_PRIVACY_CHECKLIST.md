# Play privacy and family declarations — release checklist

This is a preparation checklist for the current Little Words Android source, not a completed Play Console declaration or a legal review. Recheck the final release bundle, its merged manifest, bundled SDKs, and any store or app changes before submitting answers. The draft policy is in [PRIVACY_POLICY_DRAFT.md](PRIVACY_POLICY_DRAFT.md).

## Evidence from the current app

- `app/src/main/AndroidManifest.xml` declares no permissions, including `INTERNET`, `RECORD_AUDIO`, or `AD_ID`. The only activity is the exported launcher activity. Its application configuration disables backup.
- `app/src/main/res/xml/data_extraction_rules.xml` excludes app data from cloud backup and device transfer.
- `app/build.gradle.kts` includes AndroidX Compose, lifecycle, Room, and test tooling; it does not declare an ads, analytics, crash-reporting, authentication, or networking SDK.
- `ReadingDatabase.kt` and `Entities.kt` store profile names, the active profile, per-profile settings, sessions, shown cards, attempts (including scores, timestamps, and reading duration), and stage achievements in a local Room database. `ReadingRepository.kt` uses these records to resume practice and calculate progress.
- There is no app account or server login. A reading **profile is local data**, not an online account. There is no in-app profile deletion or export. Settings opens an About & privacy summary explaining local storage and deletion; it is not yet the final hosted policy.

## Complete before the first Play submission

1. **Finish and host the policy.** Replace the publisher/contact placeholder in the draft with a real privacy contact or inquiry method. Make sure the publisher identity matches the listing. Publish one governing policy as a readable, non-editable page at an active, publicly accessible, non-geofenced URL; a repository file or PDF alone is insufficient. Enter that URL under **Policy and programs → App content → Privacy policy** in Play Console. Add the final policy link or complete policy text to the existing About & privacy screen. Check both destinations from a device without signing in.
2. **Target audience and content.** Declare the age groups actually intended for the finished app. The present design is parent-guided early reading and includes children, so apply the Families Policy Requirements; do not select an adult-only audience to avoid them. Review the bundled word and sentence catalogues and store artwork for age suitability. Google requires accurate target audience, Data safety, and content rating answers.
3. **Ads and app access.** For the source currently inspected, the likely Ads answer is **No**. Confirm this against the final bundle and listing. Describe any access instructions accurately; the current app has no sign-in gate.
4. **Data safety.** Complete the form even if no data is sent. Under Google's definition, “collect” means transmitting data off the device; solely local profile names and reading history are not declared as collected if they are never transmitted. For the current source, **no data collected or shared** is the expected answer. Confirm that the release artifact and all bundled libraries do not add transmission before selecting it. Explain local storage and deletion accurately in the privacy policy even when the Data safety form says no collection.
5. **Data deletion questions.** Answer that the current app does **not** create app accounts. Local reading profiles are not online accounts. Check the current Play Console wording and answer the remaining deletion questions accurately. Do not imply that individual profile deletion exists. Clearing app storage or uninstalling removes all local profiles and progress.
6. **Content rating.** Complete the current IARC content rating questionnaire for the actual app and resubmit it if content or functionality changes. A rating is required to publish on Play.
7. **Check release changes.** Any later networking, crash reports, support form, advertising, speech recognition, backup, export, or third-party SDK may change the policy, Families obligations, and Data safety answers. Reassess before release and after updates.

## Sources (Google Play, checked 30 September 2026)

- [User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311) — privacy policy contents, publisher/contact, retention/deletion, public URL, and Data safety consistency.
- [Prepare your app for review](https://support.google.com/googleplay/android-developer/answer/9859455) — in-app and store privacy policy for child-targeted apps, ads and app content declarations.
- [Data safety form guidance](https://support.google.com/googleplay/android-developer/answer/10787469) — every published app completes the form; “collect” means data transmitted off-device, including by SDKs.
- [Target audience and content](https://support.google.com/googleplay/android-developer/answer/9867159) and [Families Policy Requirements](https://support.google.com/googleplay/android-developer/answer/9893335) — child audience and accurate Console declarations.
- [Content Ratings](https://support.google.com/googleplay/android-developer/answer/9898843) — IARC questionnaire requirement.
- [App account deletion requirements](https://support.google.com/googleplay/android-developer/answer/13327111) — applies when an app enables account creation.
