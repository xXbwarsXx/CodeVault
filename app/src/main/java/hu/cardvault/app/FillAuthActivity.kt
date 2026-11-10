package hu.cardvault.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.service.autofill.Dataset
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.view.autofill.AutofillManager
import android.widget.RemoteViews
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Runs a new strong-biometric prompt for every selected Autofill value. */
class FillAuthActivity : FragmentActivity() {
    companion object {
        const val EXTRA_FIELD_ID = "cardvault_field_id"
        const val EXTRA_VALUE_TYPE = "cardvault_value_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This is a transient Autofill authentication surface, not an app screen.
        // Keep the target app visible underneath the system biometric prompt.
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)

        val fieldId = getFieldId() ?: run {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        val valueType = intent.getStringExtra(EXTRA_VALUE_TYPE)
        if (valueType != "number" && valueType != "expiry") {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
        if (BiometricManager.from(this).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    try {
                        // Decryption happens only after this fresh biometric approval.
                        val card = SecureStore(this@FillAuthActivity).load()
                            ?: throw IllegalStateException("Nincs mentett kártya")
                        val value = if (valueType == "number") card.first else card.second
                        val label = if (valueType == "number") "CardVault – kártyaszám" else "CardVault – lejárat"
                        val presentation = RemoteViews(packageName, R.layout.autofill_presentation).apply {
                            setTextViewText(R.id.autofill_title, label)
                            setTextViewText(R.id.autofill_subtitle, "Hitelesítve")
                        }
                        val dataset = Dataset.Builder()
                            .setValue(fieldId, AutofillValue.forText(value), presentation)
                            .build()
                        val resultIntent = Intent().putExtra(
                            AutofillManager.EXTRA_AUTHENTICATION_RESULT,
                            dataset
                        )
                        setResult(Activity.RESULT_OK, resultIntent)
                    } catch (_: Exception) {
                        setResult(Activity.RESULT_CANCELED)
                    }
                    finish()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }

                override fun onAuthenticationFailed() {
                    // Keep the prompt open so the user can retry; no data is released.
                }
            }
        )

        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("CardVault – jóváhagyás")
                .setSubtitle("Az adat kitöltéséhez igazold magad ujjlenyomattal")
                .setNegativeButtonText("Mégse")
                .setAllowedAuthenticators(authenticators)
                .build()
        )
    }

    @Suppress("DEPRECATION")
    private fun getFieldId(): AutofillId? {
        return if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_FIELD_ID, AutofillId::class.java)
        } else {
            intent.getParcelableExtra(EXTRA_FIELD_ID)
        }
    }
}
