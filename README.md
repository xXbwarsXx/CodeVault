# CardVault – secure Autofill build

## Autofill behavior
- CardVault offers two explicit choices in every enabled app's currently focused editable text field: **Kártyaszám** and **Lejárati dátum**.
- It does not depend on payment-specific field names or HTML hints. This is intended for apps such as Steam that expose generic text boxes.
- Only the currently focused field is filled. It never sprays card data into every text field on the screen.
- Each choice is an authenticated Autofill dataset. Android launches `FillAuthActivity`, which requires a fresh `BIOMETRIC_STRONG` authentication. The actual value is decrypted only after successful authentication.
- Cancel, biometric failure, or missing strong biometric enrollment returns no data.
- CVC is not persisted and is not included in Autofill.

## Storage
- Card number and expiry are encrypted with AES-256-GCM.
- The AES key is generated in Android Keystore and is non-exportable; the key requires user authentication for cryptographic use.
- Android backup is disabled for app data.

## UI
- Dark-mode preference is persisted.
- Material You dynamic color can be toggled and persisted on Android 12+.
- A manual accent-color palette is available when dynamic colors are disabled.
- The content respects system safe drawing insets so it does not draw under the status bar.

## Important compatibility note
Android must expose the focused control as an Autofill-capable node in its AssistStructure for any Autofill service to receive it. Some apps/WebViews/custom controls suppress Autofill entirely. In that case, no third-party Autofill provider can force Android to offer the field. Test the Steam checkout UI after enabling CardVault in Android's Autofill service settings and allowing Steam in CardVault.

## Build
The GitHub Actions workflow uses JDK 17, Android SDK 36, Gradle 8.11.1 and runs `gradle assembleDebug`.


Autofill UI update: the suggestion now uses a dedicated dark RemoteViews layout. The authentication activity is translucent and does not launch MainActivity; it displays the Android BiometricPrompt over the target app and returns the authenticated Dataset through EXTRA_AUTHENTICATION_RESULT. The authentication PendingIntent is mutable so the Android Autofill framework can attach its fill-in arguments.
