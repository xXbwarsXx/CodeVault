package hu.cardvault.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.autofill.AutofillManager
import android.view.View
import android.widget.EditText
import android.text.InputType
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    private var authenticated = false
    private var authInProgress = false
    private var authCallback: (() -> Unit)? = null
    private var lockCallback: (() -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContent {
            var darkMode by remember { mutableStateOf(false) }
            CardVaultTheme(darkMode) {
                var unlocked by remember { mutableStateOf(false) }
                var cardNumber by remember { mutableStateOf("") }
                var expiry by remember { mutableStateOf("") }
                var cvc by remember { mutableStateOf("") }
                var message by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    authCallback = { unlocked = true; authenticated = true }
                    lockCallback = { unlocked = false; cardNumber = ""; expiry = ""; cvc = "" }
                    if (authenticated) unlocked = true else authenticate()
                }

                if (!unlocked) {
                    LockedScreen()
                } else {
                    CardVaultScreen(
                        cardNumber = cardNumber,
                        expiry = expiry,
                        cvc = cvc,
                        darkMode = darkMode,
                        message = message,
                        onDarkModeChange = { darkMode = it },
                        onCardNumberChange = { cardNumber = it },
                        onExpiryChange = { expiry = it },
                        onCvcChange = { cvc = it },
                        onCopy = { label, value ->
                            if (value.isBlank()) {
                                message = "Először töltsd ki a mezőt az automatikus kitöltéssel."
                            } else {
                                copySensitive(label, value)
                                message = "$label másolva. A vágólap 30 másodperc múlva törlődik."
                            }
                        },
                        onMessageDismiss = { message = null }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!authenticated && !authInProgress) {
            handler.post { authenticate() }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        authenticated = false
        lockCallback?.invoke()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            authenticated = false
            authInProgress = false
            lockCallback?.invoke()
        }
    }

    private fun authenticate() {
        if (authInProgress || isFinishing) return
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val manager = BiometricManager.from(this)
        val canAuth = manager.canAuthenticate(authenticators)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            authInProgress = false
            finish()
            return
        }
        authInProgress = true
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                authInProgress = false
                authenticated = true
                authCallback?.invoke()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                authInProgress = false
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED) {
                    finish()
                } else {
                    handler.postDelayed({ authenticate() }, 700)
                }
            }
            override fun onAuthenticationFailed() { /* allow another attempt */ }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("CardVault feloldása")
            .setSubtitle("Azonosítsd magad a kártyaadatok eléréséhez")
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
    }

    private fun copySensitive(label: String, value: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipLabel = "$label-${System.currentTimeMillis()}"
        clipboard.setPrimaryClip(ClipData.newPlainText(clipLabel, value))
        handler.postDelayed({
            if (clipboard.hasPrimaryClip() && clipboard.primaryClipDescription?.label == clipLabel) {
                clipboard.clearPrimaryClip()
            }
        }, 30_000)
    }
}

@Composable
private fun CardVaultTheme(dark: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scheme = if (android.os.Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) darkColorScheme(
        primary = Color(0xFFD0BCFF), secondary = Color(0xFFCCC2DC),
        background = Color(0xFF141218), surface = Color(0xFF211F26)
    ) else lightColorScheme(
        primary = Color(0xFF6750A4), secondary = Color(0xFF625B71),
        background = Color(0xFFF7F2FA), surface = Color(0xFFFFF7FF)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
private fun LockedScreen() {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(18.dp))
            Text("CardVault", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Azonosítás szükséges", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CardVaultScreen(
    cardNumber: String, expiry: String, cvc: String, darkMode: Boolean,
    message: String?, onDarkModeChange: (Boolean) -> Unit,
    onCardNumberChange: (String) -> Unit, onExpiryChange: (String) -> Unit,
    onCvcChange: (String) -> Unit, onCopy: (String, String) -> Unit,
    onMessageDismiss: () -> Unit
) {
    val activity = LocalContext.current as? ComponentActivity
    BackHandler { activity?.finish() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Default.CreditCard, null, modifier = Modifier.padding(12.dp).size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("CardVault", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("Biztonságos kártyakitöltő", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sötét", fontSize = 12.sp)
                    Switch(checked = darkMode, onCheckedChange = onDarkModeChange)
                }
            }
            Spacer(Modifier.height(28.dp))
            Text("Bankkártya adatai", fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("Koppints egy mezőre az Android automatikus kitöltésének megnyitásához.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            Spacer(Modifier.height(22.dp))

            AutofillField("Kártyaszám", cardNumber, "creditCardNumber", InputType.TYPE_CLASS_NUMBER,
                onCardNumberChange, Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            AutofillField("Lejárati dátum", expiry, "creditCardExpirationDate", InputType.TYPE_CLASS_DATETIME,
                onExpiryChange, Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            AutofillField("CVC / CVV", cvc, "creditCardSecurityCode", InputType.TYPE_CLASS_NUMBER,
                onCvcChange, Modifier.fillMaxWidth())

            Spacer(Modifier.height(20.dp))
            CopyButton("Kártyaszám másolása", cardNumber) { onCopy("CardVault kártyaszám", cardNumber) }
            Spacer(Modifier.height(10.dp))
            CopyButton("Lejárati dátum másolása", expiry) { onCopy("CardVault lejárat", expiry) }
            Spacer(Modifier.height(10.dp))
            CopyButton("CVC másolása", cvc) { onCopy("CardVault CVC", cvc) }

            Spacer(Modifier.height(24.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Adatvédelem", fontWeight = FontWeight.SemiBold)
                        Text("A CardVault nem menti el a kártyaadatokat. A másolt adat 30 mp után törlődik a vágólapról.",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Az automatikus kitöltéshez legyen beállítva egy kártyaadatokat kezelő Autofill-szolgáltatás a telefonon.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (message != null) {
                Spacer(Modifier.height(12.dp))
                Card {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(message, Modifier.weight(1f), fontSize = 12.sp)
                        TextButton(onClick = onMessageDismiss) { Text("OK") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AutofillField(
    label: String, value: String, hint: String, inputType: Int,
    onValueChange: (String) -> Unit, modifier: Modifier = Modifier
) {
    val fieldBackground = MaterialTheme.colorScheme.surfaceVariant.toArgb()
    val fieldStroke = MaterialTheme.colorScheme.outline.toArgb()
    val fieldText = MaterialTheme.colorScheme.onSurface.toArgb()
    Column(modifier) {
        Text(label, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(58.dp),
            factory = { ctx ->
                EditText(ctx).apply {
                    setSingleLine(true)
                    textSize = 16f
                    setTextColor(fieldText)
                    setPadding(18, 0, 18, 0)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(fieldBackground)
                        cornerRadius = 18f
                        setStroke(1, fieldStroke)
                    }
                    this.inputType = inputType
                    setAutofillHints(when (hint) {
                        "creditCardNumber" -> View.AUTOFILL_HINT_CREDIT_CARD_NUMBER
                        "creditCardExpirationDate" -> View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE
                        else -> View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE
                    })
                    importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
                    setOnClickListener { requestAutofill() }
                    setOnFocusChangeListener { view, hasFocus ->
                        if (hasFocus) view.post { (ctx.getSystemService(Context.AUTOFILL_SERVICE) as? AutofillManager)?.requestAutofill(view) }
                    }
                    addTextChangedListener(object : android.text.TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { onValueChange(s?.toString().orEmpty()) }
                        override fun afterTextChanged(s: android.text.Editable?) {}
                    })
                }
            },
            update = { edit ->
                edit.setTextColor(fieldText)
                (edit.background as? android.graphics.drawable.GradientDrawable)?.apply {
                    setColor(fieldBackground)
                    setStroke(1, fieldStroke)
                }
                if (edit.text.toString() != value) edit.setText(value)
            }
        )
    }
}

@Composable
private fun CopyButton(label: String, value: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp)) {
        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label)
    }
}
