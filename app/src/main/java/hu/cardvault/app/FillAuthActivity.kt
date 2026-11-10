package hu.cardvault.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.autofill.AutofillManager
import android.service.autofill.Dataset
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class FillAuthActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        val flags = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    try {
                        val card = SecureStore(this@FillAuthActivity).load() ?: run { finish(); return }
                        @Suppress("DEPRECATION")
                        val ids = intent.getParcelableArrayListExtra<AutofillId>("fields_ids").orEmpty()
                        val types = intent.getStringArrayListExtra("fields_types").orEmpty()
                        val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
                            setTextViewText(android.R.id.text1, "CardVault")
                        }
                        val dataset = Dataset.Builder()
                        ids.zip(types).forEach { (id, type) ->
                            dataset.setValue(id, AutofillValue.forText(if (type == "number") card.first else card.second), presentation)
                        }
                        setResult(Activity.RESULT_OK, Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset.build()))
                    } catch (_: Exception) { setResult(Activity.RESULT_CANCELED) }
                    finish()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { finish() }
            })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle("CardVault automatikus kitöltés")
            .setSubtitle("Kártyaadatok biztonságos kitöltése")
            .setAllowedAuthenticators(flags).build())
    }
}
