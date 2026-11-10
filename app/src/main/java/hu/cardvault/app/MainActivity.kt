package hu.cardvault.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import android.content.pm.PackageManager
import android.view.autofill.AutofillManager

class MainActivity : FragmentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var unlocked by mutableStateOf(false)
    private var number by mutableStateOf("")
    private var expiry by mutableStateOf("")
    private var cvc by mutableStateOf("")
    private var authInProgress = false
    private var hasStarted = false
    private var status by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val context = LocalContext.current
            var dark by remember { mutableStateOf(false) }
            var appsPage by remember { mutableStateOf(false) }
            var allowed by remember { mutableStateOf(AllowedApps.get(context)) }
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                        .verticalScroll(rememberScrollState()).padding(20.dp)) {
                        Text("CardVault", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        if (!unlocked) {
                            Text("A kártyaadatokhoz azonosítás szükséges.")
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { authenticate() }) { Text("Feloldás") }
                        } else if (appsPage) {
                            Text("Engedélyezett alkalmazások", style = MaterialTheme.typography.titleLarge)
                            Text("A CardVault csak a kiválasztott alkalmazásokban kínál kitöltést. A lista a megnyitható alkalmazásokat mutatja.")
                            Spacer(Modifier.height(12.dp))
                            val apps = remember {
                                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                                @Suppress("DEPRECATION")
                                context.packageManager.queryIntentActivities(intent, 0)
                                    .map { it.activityInfo.packageName to it.loadLabel(context.packageManager).toString() }
                                    .distinctBy { it.first }.sortedBy { it.second.lowercase() }
                            }
                            apps.forEach { (pkg, label) ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("$label\n$pkg", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                    Checkbox(checked = pkg in allowed, onCheckedChange = {
                                        AllowedApps.set(context, pkg, it)
                                        allowed = AllowedApps.get(context)
                                    })
                                }
                                HorizontalDivider()
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { appsPage = false }) { Text("Vissza") }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Sötét mód")
                                Switch(checked = dark, onCheckedChange = { dark = it })
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("Titkosított kártyatároló", style = MaterialTheme.typography.titleLarge)
                            Text("A kártyaszám és a lejárat menthető. A CVC csak ideiglenesen marad a memóriában.")
                            Spacer(Modifier.height(14.dp))
                            OutlinedTextField(number, { number = it.filter(Char::isDigit).take(19) },
                                label = { Text("Kártyaszám") }, modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(expiry, { expiry = it.take(7) },
                                label = { Text("Lejárat (HH/ÉÉ)") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(cvc, { cvc = it.filter(Char::isDigit).take(4) },
                                label = { Text("CVC – nem tároljuk") }, modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = {
                                try {
                                    SecureStore(context).save(number, expiry)
                                    status = "Kártyaadatok titkosítva mentve."
                                } catch (e: Exception) { status = "Mentés sikertelen: ellenőrizd az adatokat és az azonosítást." }
                            }, modifier = Modifier.fillMaxWidth()) { Text("Kártyaadatok mentése") }
                            OutlinedButton(onClick = {
                                SecureStore(context).delete(); number = ""; expiry = ""; cvc = ""
                                status = "Tárolt kártya törölve."
                            }, modifier = Modifier.fillMaxWidth()) { Text("Tárolt kártya törlése") }
                            HorizontalDivider(Modifier.padding(vertical = 12.dp))
                            OutlinedButton(onClick = { copy("Kártyaszám", number) }, modifier = Modifier.fillMaxWidth()) { Text("Kártyaszám másolása") }
                            OutlinedButton(onClick = { copy("Lejárat", expiry) }, modifier = Modifier.fillMaxWidth()) { Text("Lejárat másolása") }
                            OutlinedButton(onClick = { copy("CVC", cvc) }, modifier = Modifier.fillMaxWidth()) { Text("CVC másolása") }
                            HorizontalDivider(Modifier.padding(vertical = 12.dp))
                            Button(onClick = { appsPage = true }, modifier = Modifier.fillMaxWidth()) { Text("Engedélyezett alkalmazások") }
                            OutlinedButton(onClick = {
                                startActivity(Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                                    data = android.net.Uri.parse("package:$packageName")
                                })
                            }, modifier = Modifier.fillMaxWidth()) { Text("CardVault beállítása kitöltőként") }
                            Text("Az Android által felismerhető fizetési mezőket tölti ki, kizárólag jóváhagyás után. A CVC-t nem tölti ki automatikusan.",
                                style = MaterialTheme.typography.bodySmall)
                            if (status.isNotEmpty()) Text(status, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        handler.post { authenticate() }
    }

    override fun onStart() {
        super.onStart()
        if (hasStarted && !unlocked) handler.post { authenticate() }
        hasStarted = true
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            unlocked = false; number = ""; expiry = ""; cvc = ""
        }
    }

    private fun authenticate() {
        if (authInProgress || isFinishing || unlocked) return
        val flags = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (BiometricManager.from(this).canAuthenticate(flags) != BiometricManager.BIOMETRIC_SUCCESS) {
            status = "Állíts be képernyőzárat vagy erős biometrikus hitelesítést."
            return
        }
        authInProgress = true
        BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authInProgress = false
                    try {
                        val stored = SecureStore(this@MainActivity).load()
                        number = stored?.first.orEmpty(); expiry = stored?.second.orEmpty()
                        cvc = ""; unlocked = true
                    } catch (_: Exception) {
                        unlocked = false
                        status = "A titkosított adatok nem érhetők el. Próbáld újra a feloldást."
                    }
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { authInProgress = false }
            }).authenticate(BiometricPrompt.PromptInfo.Builder()
                .setTitle("CardVault feloldása")
                .setSubtitle("Ujjlenyomat vagy készülék-PIN/minta")
                .setAllowedAuthenticators(flags).build())
    }

    private fun copy(label: String, value: String) {
        if (value.isBlank()) { status = "$label: nincs megadva adat."; return }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val tag = "CardVault-${System.nanoTime()}"
        clipboard.setPrimaryClip(ClipData.newPlainText(tag, value))
        status = "$label másolva; a CardVault 30 mp múlva megpróbálja törölni a vágólapról."
        handler.postDelayed({
            if (clipboard.primaryClipDescription?.label == tag) clipboard.clearPrimaryClip()
        }, 30_000)
    }
}
