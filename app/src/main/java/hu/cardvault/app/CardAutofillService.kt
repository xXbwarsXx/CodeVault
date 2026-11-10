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
import android.service.autofill.Dataset
import android.view.View
import android.view.autofill.AutofillId
import android.app.assist.AssistStructure
import android.widget.RemoteViews

class CardAutofillService : AutofillService() {
    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        try {
            if (!SecureStore(this).hasCard()) { callback.onSuccess(null); return }
            val structure = request.fillContexts.lastOrNull()?.structure ?: run { callback.onSuccess(null); return }
            val pkg = structure.activityComponent?.packageName ?: run { callback.onSuccess(null); return }
            if (pkg == packageName || !AllowedApps.isAllowed(this, pkg)) { callback.onSuccess(null); return }
            val fields = findCardFields(structure)
            if (fields.isEmpty()) { callback.onSuccess(null); return }
            val intent = Intent(this, FillAuthActivity::class.java).apply {
                putExtra("fields_ids", ArrayList(fields.map { it.first }))
                putExtra("fields_types", ArrayList(fields.map { it.second }))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pending = PendingIntent.getActivity(this, 42, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            val label = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
                setTextViewText(android.R.id.text1, "CardVault – kártyaadatok")
            }
            val builder = Dataset.Builder()
            fields.forEach { (id, _) -> builder.setValue(id, null, label) }
            builder.setAuthentication(pending.intentSender)
            callback.onSuccess(FillResponse.Builder().addDataset(builder.build()).build())
        } catch (_: Exception) { callback.onSuccess(null) }
    }
    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) { callback.onSuccess() }
}

/** Only confidently identified payment fields are offered; generic text inputs are not touched. */
internal fun findCardFields(structure: AssistStructure): List<Pair<AutofillId, String>> {
    val found = mutableListOf<Pair<AutofillId, String>>()
    fun visit(node: AssistStructure.ViewNode) {
        val hints = node.autofillHints?.joinToString(" ")?.lowercase().orEmpty()
        val metadata = listOf(node.idEntry, node.hint, node.htmlInfo?.attributes?.joinToString(" ") {
            "${it.first}=${it.second}"
        }).joinToString(" ").lowercase()
        val name = "$hints $metadata"
        val type = when {
            listOf("creditcardnumber", "cc-number", "cardnumber", "card_number", "card-number", "card number", "kártyaszám").any { name.contains(it) } -> "number"
            listOf("creditcardexpirationdate", "cc-exp", "cardexpiry", "card_expiry", "expiry", "expiration", "lejárat").any { name.contains(it) } -> "expiry"
            else -> null
        }
        if (type != null && node.autofillId != null && node.autofillType != View.AUTOFILL_TYPE_NONE) {
            found.add(node.autofillId to type)
        }
        for (i in 0 until node.childCount) visit(node.getChildAt(i))
    }
    for (i in 0 until structure.windowNodeCount) visit(structure.getWindowNodeAt(i).rootViewNode)
    return found.distinctBy { it.first }
}
