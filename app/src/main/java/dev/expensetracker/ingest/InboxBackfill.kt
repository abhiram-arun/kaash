package dev.expensetracker.ingest

import android.content.Context
import android.provider.Telephony.Sms
import dev.expensetracker.data.TxnRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Re-ingests every inbox SMS; safe to run repeatedly because inserts dedupe. Returns messages scanned. */
suspend fun backfillInbox(ctx: Context, repo: TxnRepository): Int = withContext(Dispatchers.IO) {
    var n = 0
    ctx.contentResolver.query(
        Sms.Inbox.CONTENT_URI, arrayOf(Sms.ADDRESS, Sms.BODY, Sms.DATE_SENT, Sms.DATE), null, null, null,
    )?.use { c ->
        while (c.moveToNext()) {
            val sender = c.getString(0) ?: continue
            val body = c.getString(1) ?: continue
            val ts = c.getLong(2).takeIf { it > 0 } ?: c.getLong(3)
            repo.ingest(sender, body, ts)
            n++
        }
    }
    n
}
