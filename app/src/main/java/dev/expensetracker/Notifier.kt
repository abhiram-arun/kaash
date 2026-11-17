package dev.expensetracker

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class Notifier(private val ctx: Context) {
    private val nm = ctx.getSystemService(NotificationManager::class.java)

    init {
        // Separate channels so new-transaction pings can be muted without losing budget/bill alerts.
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(ALERTS, "Budgets & bills", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(NEW_TXN, "New transactions", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(SPLITS, "Unsettled splits", NotificationManager.IMPORTANCE_DEFAULT),
            )
        )
    }

    /** [txnId] makes the tap open that transaction's tag picker. */
    fun show(id: Int, title: String, text: String, channel: String = ALERTS, txnId: Long? = null) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val intent = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .apply { txnId?.let { putExtra(EXTRA_TXN, it) } }
        val open = PendingIntent.getActivity(ctx, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        nm.notify(
            channel, // tag: ids only need to be unique within a channel, so txn #1001 can't replace person #1's reminder
            id,
            Notification.Builder(ctx, channel)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
    }

    companion object {
        const val ALERTS = "alerts"
        const val NEW_TXN = "new_txn"
        const val SPLITS = "splits"
        const val EXTRA_TXN = "txnId"
    }
}
