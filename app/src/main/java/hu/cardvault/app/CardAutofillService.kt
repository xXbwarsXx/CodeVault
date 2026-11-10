package hu.cardvault.app

import android.app.PendingIntent
import android.content.Intent
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.view.View
import android.view.autofill.AutofillId
import android.app.assist.AssistStructure
import android.widget.RemoteViews

/**
 * Offers CardVault in any enabled app's currently focused text field.
 * The user explicitly chooses which value to insert; the value itself is not
 * released until FillAuthActivity completes a fresh strong-biometric check.
 */
class CardAutofillService : AutofillService() {
    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        try {
            if (!SecureStore(this).hasCard()) {
                callback.onSuccess(null)
                return
            }

            val structure = request.fillContexts.lastOrNull()?.structure
            if (structure == null) {
                callback.onSuccess(null)
                return
            }

            val packageNameOfTarget = structure.activityComponent?.packageName
            if (packageNameOfTarget.isNullOrBlank() ||
                packageNameOfTarget == packageName ||
                !AllowedApps.isAllowed(this, packageNameOfTarget)
            ) {
                callback.onSuccess(null)
                return
            }

            // Only the field that currently has focus is targeted. We deliberately
            // do not require card-number/expiry hints: Steam and other apps often
            // expose ordinary text inputs instead of payment-specific fields.
            val focusedId = findFocusedTextField(structure)
            if (focusedId == null) {
                callback.onSuccess(null)
                return
            }

            val response = FillResponse.Builder()
                .addDataset(authenticatedDataset(focusedId, "number"))
                .addDataset(authenticatedDataset(focusedId, "expiry"))
                .build()

            callback.onSuccess(response)
        } catch (_: Exception) {
            callback.onSuccess(null)
        }
    }

    private fun authenticatedDataset(fieldId: AutofillId, valueType: String): android.service.autofill.Dataset {
        val title = if (valueType == "number") "Kártyaszám" else "Lejárati dátum"
        val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
            setTextViewText(android.R.id.text1, "$title – ujjlenyomat szükséges")
        }

        val authIntent = Intent(this, FillAuthActivity::class.java).apply {
            putExtra(FillAuthActivity.EXTRA_FIELD_ID, fieldId)
            putExtra(FillAuthActivity.EXTRA_VALUE_TYPE, valueType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val requestCode = if (valueType == "number") 4101 else 4102
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            authIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return android.service.autofill.Dataset.Builder()
            // A null placeholder keeps the real card data out of the Autofill UI.
            .setValue(fieldId, null, presentation)
            .setAuthentication(pendingIntent.intentSender)
            .build()
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }
}

/** Returns only the currently focused, editable text field. */
private fun findFocusedTextField(structure: AssistStructure): AutofillId? {
    var focused: AutofillId? = null

    fun visit(node: AssistStructure.ViewNode) {
        if (focused != null) return

        val id = node.autofillId
        val isTextField = node.autofillType == View.AUTOFILL_TYPE_TEXT ||
            node.className?.endsWith("EditText", ignoreCase = true) == true ||
            node.htmlInfo != null

        if (node.isFocused && id != null && isTextField) {
            focused = id
            return
        }

        for (i in 0 until node.childCount) {
            visit(node.getChildAt(i))
            if (focused != null) return
        }
    }

    for (i in 0 until structure.windowNodeCount) {
        visit(structure.getWindowNodeAt(i).rootViewNode)
        if (focused != null) break
    }
    return focused
}
