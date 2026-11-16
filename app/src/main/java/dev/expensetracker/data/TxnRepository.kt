package dev.expensetracker.data

import androidx.room.withTransaction
import dev.expensetracker.Notifier
import dev.expensetracker.monthRange
import dev.expensetracker.parse.BankParsers
import dev.expensetracker.parse.Direction
import dev.expensetracker.rupees
import dev.expensetracker.toLocalDate
import java.time.YearMonth

class TxnRepository(private val db: AppDatabase, private val notifier: Notifier) {
    val dao = db.dao()

    suspend fun ingest(sender: String, body: String, timestamp: Long, source: String = "SMS") {
        if (!BankParsers.isBankSender(sender)) return
        val p = BankParsers.parse(body)
        if (p == null) {
            if (BankParsers.looksLikeTxn(body)) dao.insertUnparsed(Unparsed(sender = sender, body = body, timestamp = timestamp))
            return
        }
        val key = p.merchantKey
        val cat = if (key.isBlank()) null else dao.ruleFor(key)
        val id = dao.insertTxn(
            Txn(
                amountPaise = p.amountPaise, direction = p.direction, account = p.account, channel = p.channel,
                counterparty = p.counterparty, merchantKey = key, ref = p.ref, balancePaise = p.balancePaise,
                timestamp = timestamp, categoryId = cat, source = source, raw = body,
            )
        )
        if (id == -1L || p.direction != Direction.DEBIT) return
        cat?.let { checkBudget(it, timestamp) }
        dao.allBills().firstOrNull { it.matches(key, p.amountPaise, timestamp.toLocalDate()) }
            ?.let { dao.upsertBill(it.copy(paidCount = it.paidCount + 1, lastPaidTxnId = id)) }
    }

    /** Tags this txn, remembers the merchant for the future, and back-fills its other untagged txns. */
    suspend fun tag(t: Txn, cat: Long) {
        db.withTransaction {
            dao.setCategory(t.id, cat)
            if (t.merchantKey.isNotBlank()) {
                dao.upsertRule(MerchantRule(t.merchantKey, cat))
                dao.tagUntagged(t.merchantKey, cat)
            }
        }
        if (t.direction == Direction.DEBIT) checkBudget(cat, t.timestamp)
    }

    private suspend fun checkBudget(cat: Long, timestamp: Long) {
        val ym = YearMonth.from(timestamp.toLocalDate())
        if (ym != YearMonth.now()) return // backfilled history shouldn't fire alerts
        val b = dao.budget(cat) ?: return
        val (from, to) = monthRange(ym)
        val spent = dao.spent(cat, from, to)
        val level = budgetLevel(spent, b.monthlyLimitPaise)
        val monthKey = ym.year * 100 + ym.monthValue
        val already = if (b.alertMonth == monthKey) b.alertLevel else 0
        if (level <= already) return
        dao.upsertBudget(b.copy(alertMonth = monthKey, alertLevel = level))
        val name = dao.categoryName(cat) ?: "Budget"
        notifier.show(
            id = 2000 + cat.toInt(),
            title = if (level >= 100) "$name budget exceeded" else "$name budget at 80%",
            text = "${rupees(spent)} of ${rupees(b.monthlyLimitPaise)} spent this month",
        )
    }

    suspend fun seedCategories() {
        if (dao.categoryCount() > 0) return
        dao.insertCategories(
            listOf(
                "Food" to 0xFFE57373, "Groceries" to 0xFF81C784, "Transport" to 0xFF64B5F6,
                "Rent" to 0xFF9575CD, "Bills" to 0xFFFFB74D, "Shopping" to 0xFFF06292,
                "Transfers" to 0xFF90A4AE, "Income" to 0xFF4DB6AC, "Other" to 0xFFA1887F,
            ).map { (n, c) -> Category(name = n, color = c) }
        )
    }
}
