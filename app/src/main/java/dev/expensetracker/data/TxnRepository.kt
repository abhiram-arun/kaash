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

    /** [notify] is true only for live SMS; inbox backfill must not ping for every historic message. */
    suspend fun ingest(sender: String, body: String, timestamp: Long, notify: Boolean = false) {
        if (!BankParsers.isBankSender(sender)) return
        val p = BankParsers.parse(body)
        if (p == null) {
            if (BankParsers.looksLikeTxn(body)) dao.insertUnparsed(Unparsed(sender = sender, body = body, timestamp = timestamp))
            else dao.deleteUnparsed(sender, timestamp) // noise logged by an older, looser filter
            return
        }
        dao.deleteUnparsed(sender, timestamp) // a format the parser has since learned leaves the unparsed list on rescan
        val key = p.merchantKey
        val cat = if (key.isBlank()) null else dao.ruleFor(key)
        val id = dao.insertTxn(
            Txn(
                amountPaise = p.amountPaise, direction = p.direction, account = p.account, channel = p.channel,
                counterparty = p.counterparty, merchantKey = key, ref = p.ref, balancePaise = p.balancePaise,
                timestamp = timestamp, categoryId = cat, source = "SMS", raw = body,
            )
        )
        if (id == -1L) {
            dao.keepEarliest(body, timestamp)
            p.balancePaise?.let { dao.fillBalance(p.account, p.amountPaise, timestamp, it) }
            return
        }
        if (notify && cat == null) {
            val credit = p.direction == Direction.CREDIT
            notifier.show(
                id = id.toInt(),
                title = (if (credit) "+" else "−") + rupees(p.amountPaise) + " · " + displayPayee(p.counterparty, p.direction),
                text = "New ${if (credit) "credit" else "spend"} on ··${p.account}. Tap to tag it.",
                channel = Notifier.NEW_TXN,
                txnId = id,
            )
        }
        if (p.direction != Direction.DEBIT) return
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

    /** Adds [name] (reusing an existing person, case-insensitively) to this txn's split, or updates their share. */
    suspend fun addSplit(t: Txn, name: String, amountPaise: Long) = db.withTransaction {
        val clean = name.trim().replace(Regex("\\s+"), " ")
        val pid = dao.personId(clean) ?: dao.insertPerson(Person(name = clean))
        val existing = dao.split(t.id, pid)
        dao.upsertSplit(
            existing?.copy(amountPaise = amountPaise)
                ?: Split(txnId = t.id, personId = pid, amountPaise = amountPaise, createdAt = System.currentTimeMillis())
        )
    }

    /** Budgets live on topics; a subtopic tag counts toward its parent topic's budget. */
    private suspend fun checkBudget(cat: Long, timestamp: Long) {
        val ym = YearMonth.from(timestamp.toLocalDate())
        if (ym != YearMonth.now()) return // backfilled history shouldn't fire alerts
        val topic = dao.topicOf(cat) ?: return
        val b = dao.budget(topic) ?: return
        val (from, to) = monthRange(ym)
        val spent = dao.spent(topic, from, to)
        val level = budgetLevel(spent, b.monthlyLimitPaise)
        val monthKey = ym.year * 100 + ym.monthValue
        val already = if (b.alertMonth == monthKey) b.alertLevel else 0
        if (level <= already) return
        dao.upsertBudget(b.copy(alertMonth = monthKey, alertLevel = level))
        val name = dao.categoryName(topic) ?: "Budget"
        notifier.show(
            id = 2000 + topic.toInt(),
            title = if (level >= 100) "$name budget exceeded" else "$name budget at 80%",
            text = "${rupees(spent)} of ${rupees(b.monthlyLimitPaise)} spent this month",
        )
    }

    /** Inserts missing TAXONOMY topics/subtopics and refreshes colours/icons; matches by name so ids (and tags) are kept. */
    suspend fun seedCategories() = db.withTransaction {
        val existing = dao.allCategories()
        for (t in TAXONOMY) {
            val topic = existing.firstOrNull { it.parentId == null && it.name == t.name }
            val topicId = when {
                topic == null -> dao.insertCategory(Category(name = t.name, color = t.color, icon = t.icon))
                topic.color != t.color || topic.icon != t.icon -> topic.id.also { dao.updateCategory(topic.copy(color = t.color, icon = t.icon)) }
                else -> topic.id
            }
            for ((name, icon) in t.subs) {
                val sub = existing.firstOrNull { it.parentId == topicId && it.name == name }
                when {
                    sub == null -> dao.insertCategory(Category(name = name, color = t.color, icon = icon, parentId = topicId))
                    sub.color != t.color || sub.icon != icon -> dao.updateCategory(sub.copy(color = t.color, icon = icon))
                }
            }
        }
    }
}
