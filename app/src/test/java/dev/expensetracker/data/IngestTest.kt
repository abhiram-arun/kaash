package dev.expensetracker.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.expensetracker.Notifier
import dev.expensetracker.parse.SAMPLE_SMS
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Real Room + SQLite: the dedupe lives in unique indexes, so only a real database can test it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class IngestTest {
    private val ctx = ApplicationProvider.getApplicationContext<Application>()
    private val db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
    private val repo = TxnRepository(db, Notifier(ctx))

    @After fun close() = db.close()

    private fun all() = runBlocking { db.dao().txns(0, Long.MAX_VALUE).first() }

    @Test fun sameSmsTwiceIsStoredOnce() = runBlocking {
        val (sender, body) = SAMPLE_SMS[0]
        repo.ingest(sender, body, 1_000)
        repo.ingest(sender, body, 1_000) // live receiver + inbox backfill
        repo.ingest(sender, body, 2_000) // same UPI ref, same account
        assertEquals(1, all().size)
    }

    @Test fun repeatedTextKeepsOneRowAtEarliestTime() = runBlocking {
        val (sender, body) = SAMPLE_SMS[5] // ref-less Federal credit
        repo.ingest(sender, body, 9_000) // a stray later copy first (what the old debug button did)
        repo.ingest(sender, body, 1_000) // then the inbox rescan with the real date_sent
        assertEquals(listOf(1_000L), all().map { it.timestamp })
    }

    @Test fun selfTransferKeepsBothSides() = runBlocking {
        // Federal debit and Canara credit of one UPI transfer carry the same ref.
        repo.ingest("VM-FEDBNK", "Debited Rs 500.00 from a/c X8868 on 16Nov26 19:57 via UPI to ABHIRAM ARUN. Ref 130616170003.Bal Rs 1000.00 -Federal Bank", 1_000)
        repo.ingest("AD-CANBNK", "Dear Customer, Acct XXX437 credited with INR 500.00 on 16/11/26 from ABHIRAM ARUN; UPI:130616170003; Bal INR 900.00-CanaraBank", 1_500)
        assertEquals(setOf("8868", "437"), all().map { it.account }.toSet())
    }

    @Test fun learnedFormatLeavesUnparsedList() = runBlocking {
        db.dao().insertUnparsed(Unparsed(sender = SAMPLE_SMS[6].first, body = SAMPLE_SMS[6].second, timestamp = 5_000))
        repo.ingest(SAMPLE_SMS[6].first, SAMPLE_SMS[6].second, 5_000)
        assertEquals(0, db.dao().unparsed().first().size)
        assertEquals(1, all().size)
    }
}
