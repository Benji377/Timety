# Roadmap

## Features
- Haptic feedback and sound
- Ambient background noise
- Subtasks
- Weekly or Monthly review
- Habit stacking
- Automatic cloud backups

## Socials
- XP and levels
- Achievements and badges
- Group focus
- specific locations on map boost XP

## Development
- Automated tests
- Modular project (widget / screen separation)

## iOS support
- Keep Flutter project iOS-compatible in CI (build only, no signing) on macOS runners.
- Set up a macOS machine with latest Xcode for local iOS debugging and release prep.
- Create Apple Developer account and enroll in Apple Developer Program.
- Configure app signing in Xcode (Team, Bundle Identifier, certificates, provisioning profiles).
- Add and verify all required iOS permissions in Info.plist (location, photos, notifications, etc.).
- Test on at least one real iPhone (permissions, notifications, backups, map/location flows).
- Produce signed IPA and distribute via TestFlight for external testers.
- Publish to App Store when QA is complete.

### iOS cost
- iOS compatibility builds in GitHub Actions can be done without Apple membership.
- To install on other people's iPhones reliably and distribute via TestFlight/App Store, Apple Developer Program is required.
- Apple Developer Program costs about 99 USD/year for an individual account in most regions (price/tax can vary by country).

## Google Play Store support
- Create Google Play Console account and verify developer profile.
- Prepare signed AAB release flow in CI (versioning, changelog, release notes, artifacts).
- Add Play required app content (privacy policy URL, data safety form, app access notes, target audience).
- Configure Android signing key strategy (upload key + Play App Signing).
- Run internal testing track first, then closed/open testing, then production rollout.
- Monitor Android vitals/crash reports and staged rollout metrics before full rollout.

### Google Play cost
- Google Play Console has a one-time developer registration fee (currently around 25 USD).

## F-Droid support
- Ensure app can be built from source with fully reproducible steps.
- Audit dependencies for F-Droid compliance (no proprietary trackers/SDKs in release flavor).
- Provide clear build instructions and metadata for maintainers.
- Consider a dedicated fdroid flavor if needed (e.g., disable non-free integrations).
- Submit app metadata and source repository to F-Droid for inclusion.
- Maintain fast update cadence and tag signed source releases for easier packaging.

### F-Droid cost
- Publishing on F-Droid is free.

## Amazon Appstore support (maybe)
- Create Amazon Developer account and app listing.
- Validate compatibility on Fire OS devices/emulators.
- Upload signed APK/AAB artifacts per store requirements and complete content rating.
- Configure store listing assets and regional availability.
- Test IAP/ads integrations if used (Amazon-specific SDK checks may be required).
- Set up release notes and update process alongside Google Play pipeline.

### Amazon Appstore cost
- Account and fee model may vary by region and account type; verify current terms in Amazon Developer Console before launch.