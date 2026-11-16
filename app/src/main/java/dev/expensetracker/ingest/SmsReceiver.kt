package dev.expensetracker.ingest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import dev.expensetracker.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (parts.isNullOrEmpty()) return
        val sender = parts[0].originatingAddress ?: return
        val body = parts.joinToString("") { it.messageBody.orEmpty() } // multi-part SMS
        // Matches the inbox's date_sent column, so backfill and live receipt dedupe to one row.
        val ts = parts[0].timestampMillis
        val repo = (ctx.applicationContext as App).repo
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { repo.ingest(sender, body, ts, notify = true) } finally { pending.finish() }
        }
    }
}
