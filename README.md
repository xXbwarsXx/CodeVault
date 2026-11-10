# CardVault

Minimal Android card autofill helper for Samsung Galaxy devices.

## Build on GitHub (no Android Studio required)

1. Create a new GitHub repository named `CardVault` (public or private).
2. Upload the **contents** of this folder to the repository root, preserving `.github/workflows/build.yml`.
3. Open **Actions** → **Build CardVault APK** → **Run workflow**.
4. When the run finishes, open the run and download **CardVault-debug-apk** from Artifacts.
5. Extract `app-debug.apk` and install it on the phone.

The workflow uses Temurin JDK 17, Gradle 8.11.1, Android SDK 36, and `gradle assembleDebug`.

## Autofill setup

CardVault does not store payment card data. Android Autofill must be enabled and configured with a provider that has the card saved. The provider decides which fields (especially the security code) it is willing to fill.

## Security notes

- Android biometric prompt allows BIOMETRIC_WEAK (including supported face unlock) or device credential (PIN/pattern/password).
- `FLAG_SECURE` prevents screenshots and hides the app preview in Recents on supported Android builds.
- The three copy actions are separate; clipboard content is cleared after 30 seconds when it is still the copied CardVault item.
- This is an initial debug build. Review and test the lock lifecycle on the target Samsung firmware before relying on it for payment credentials.
