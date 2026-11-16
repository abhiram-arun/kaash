package dev.expensetracker.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.expensetracker.parse.Channel
import dev.expensetracker.parse.Direction
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

// Both unique indexes dedupe with INSERT OR IGNORE: ref catches the same UPI/RRN twice,
// (account, amount, timestamp) catches ref-less SMS seen by both the live receiver and the inbox backfill.
@Entity(
    indices = [
        Index(value = ["ref"], unique = true),
        Index(value = ["account", "amountPaise", "timestamp"], unique = true),
        Index("timestamp"),
        Index("merchantKey"),
    ]
)
data class Txn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountPaise: Long,
    val direction: Direction,
    val account: String,
    val channel: Channel,
    val counterparty: String,
    val merchantKey: String,
    val ref: String?,
    val balancePaise: Long?,
    val timestamp: Long,
    val categoryId: Long?,
    val source: String,
    val raw: String,
)

/** parentId == null → topic; otherwise a subtopic. A txn may be tagged with either. */
@Entity
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Long,
    val parentId: Long? = null,
    val icon: String? = null,
)

@Entity
data class MerchantRule(@PrimaryKey val merchantKey: String, val categoryId: Long)

/** alertMonth = yyyymm of the last alert, alertLevel = highest threshold (80/100) already notified that month. */
@Entity
data class Budget(
    @PrimaryKey val categoryId: Long,
    val monthlyLimitPaise: Long,
    val alertMonth: Int = 0,
    val alertLevel: Int = 0,
)

enum class Period {
    WEEKLY, MONTHLY, YEARLY;

    // Always offset from the anchor so a bill on the 31st doesn't drift to the 28th after February.
    fun advance(anchor: LocalDate, n: Int): LocalDate = when (this) {
        WEEKLY -> anchor.plusWeeks(n.toLong())
        MONTHLY -> anchor.plusMonths(n.toLong())
        YEARLY -> anchor.plusYears(n.toLong())
    }
}

@Entity
data class RecurringBill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountPaise: Long,
    val period: Period,
    val anchorEpochDay: Long,
    val paidCount: Int = 0,
    val remindDaysBefore: Int = 3,
    val merchantKey: String? = null,
    val lastPaidTxnId: Long? = null,
) {
    fun dueDate(n: Int = paidCount): LocalDate = period.advance(LocalDate.ofEpochDay(anchorEpochDay), n)

    /** A debit pays this bill if it lands within 5 days of the due date and matches merchant (if set) or amount ±5%. */
    fun matches(key: String, amountPaise: Long, day: LocalDate): Boolean {
        if (abs(ChronoUnit.DAYS.between(dueDate(), day)) > 5) return false
        return merchantKey?.let { it == key } ?: (abs(amountPaise - this.amountPaise) * 20 <= this.amountPaise)
    }
}

@Entity(indices = [Index(value = ["sender", "timestamp"], unique = true)])
data class Unparsed(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val body: String,
    val timestamp: Long,
)

/** Someone the owner splits expenses with. Names are matched case-insensitively before inserting. */
@Entity(indices = [Index(value = ["name"], unique = true)])
data class Person(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String)

/** [amountPaise] is this person's share of the txn, owed back to the owner. */
@Entity(indices = [Index("txnId"), Index("personId")])
data class Split(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val txnId: Long,
    val personId: Long,
    val amountPaise: Long,
    val settled: Boolean = false,
    val createdAt: Long,
    val settledAt: Long? = null,
    val lastReminded: Long? = null,
)

data class SplitDetail(
    @Embedded val split: Split,
    val personName: String,
    val payee: String,
    val txnTime: Long,
    val txnAmount: Long,
)

data class CatTotal(val categoryId: Long?, val total: Long)

/** Month totals per account; `sample` is any raw SMS for that account, used to tell bank and card apart. */
data class AccountTotal(val account: String, val spent: Long, val received: Long, val sample: String)

data class AccountInfo(val bank: String, val isCard: Boolean)

fun accountInfo(raw: String) = AccountInfo(
    bank = when {
        raw.contains("canara", ignoreCase = true) -> "Canara Bank"
        raw.contains("federal", ignoreCase = true) -> "Federal Bank"
        else -> "Bank"
    },
    isCard = raw.contains("credit card", ignoreCase = true),
)

/** Payee for display: drops the long VPA/terminal number banks prefix to card merchants. */
fun displayPayee(counterparty: String, direction: Direction): String =
    counterparty.replace(Regex("^\\d{6,}\\s*"), "").ifBlank { if (direction == Direction.CREDIT) "Money received" else "Payment" }

/** 0, 80 or 100: the highest budget threshold the month's spend has crossed. */
fun budgetLevel(spentPaise: Long, limitPaise: Long) = when {
    limitPaise <= 0 -> 0
    spentPaise >= limitPaise -> 100
    spentPaise * 5 >= limitPaise * 4 -> 80
    else -> 0
}
