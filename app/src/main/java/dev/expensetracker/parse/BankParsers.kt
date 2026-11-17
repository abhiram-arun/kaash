package dev.expensetracker.parse

import java.math.BigDecimal

enum class Direction { DEBIT, CREDIT }
enum class Channel { UPI, CARD, OTHER }

data class ParsedTxn(
    val amountPaise: Long,
    val direction: Direction,
    val account: String,
    val channel: Channel,
    val counterparty: String,
    val ref: String?,
    val balancePaise: Long?,
) {
    val merchantKey get() = merchantKey(counterparty)
}

/** Uppercase, strip leading VPA/terminal digit runs, collapse whitespace: "631948987062    anil s" -> "ANIL S". */
fun merchantKey(party: String) =
    party.uppercase().replace(Regex("^\\d+\\s*"), "").replace(Regex("\\s+"), " ").trim()

object BankParsers {
    // shortcut: sender-ID allowlist for Federal + Canara only; add an ID here for each new bank.
    private val BANK_SENDERS = listOf("FEDBNK", "FEDSMS", "CANBNK") // Federal sends some credits as AD-FEDSMS-S

    private fun amt(name: String) = """(?<$name>[\d,]+(?:\.\d+)?)"""

    private class Rule(pattern: String, val direction: Direction, val channel: Channel) {
        val regex = Regex(pattern, RegexOption.IGNORE_CASE)
    }

    private val rules = listOf(
        // Federal: Debited Rs 77.00 from a/c X8868 on .. via UPI to NAME. Ref 123.Bal Rs 10489.44
        Rule("""Debited Rs ${amt("amt")} from a/c X+(?<acct>\d+) on .+? via UPI to (?<party>.+?)\. Ref (?<ref>\d+)\.\s*Bal Rs ${amt("bal")}""", Direction.DEBIT, Channel.UPI),
        // Federal autopay: Debited Rs 3000.00 from a/c X8868 for UPI mandate towards NAME on 02-11-2026 06:53:40. Ref: 667207262930
        Rule("""Debited Rs ${amt("amt")} from a/c X+(?<acct>\d+) for UPI mandate towards (?<party>.+?) on [\d-]+ [\d:]+\.\s*Ref:?\s?(?<ref>\d+)""", Direction.DEBIT, Channel.UPI),
        // Federal CC: INR 24.00 spent on your credit card ending with 6612 at MERCHANT on 15-11-2026. Available limit Rs.16,305.87
        // For cards, `bal` holds the available limit.
        Rule("""INR ${amt("amt")} spent on your credit card ending with (?<acct>\d+) at (?<party>.+?) on [\d-]+\.?(?:\s*Available limit Rs\.?\s?${amt("bal")})?""", Direction.DEBIT, Channel.CARD),
        // Federal: Rs.553.33 credited to your A/c XX8868 on .. BAL-Rs.761.19
        Rule("""Rs\.\s?${amt("amt")} credited to your A/c X+(?<acct>\d+) on .+?BAL-Rs\.\s?${amt("bal")}""", Direction.CREDIT, Channel.OTHER),
        // Federal (FEDSMS): Rs 3892 credited to your A/c XX8868 via NEFT from JUSPAY TEC on 11NOV2026 12:43:44 Ref No HDFCH01401356531 Bal:Rs 20655.39
        Rule("""Rs\.?\s?${amt("amt")} credited to your A/c X+(?<acct>\d+) via (?:NEFT|IMPS|RTGS) from (?<party>.+?) on \S+ [\d:]+ Ref No (?<ref>\w+) Bal:\s?Rs\.?\s?${amt("bal")}""", Direction.CREDIT, Channel.OTHER),
        // Federal (FEDSMS): Payment of Rs 849.00 is received towards your credit card number ending with 6612 on 10/11/2026
        Rule("""Payment of Rs\.?\s?${amt("amt")} is received towards your credit card number ending with (?<acct>\d+)""", Direction.CREDIT, Channel.CARD),
        // Canara: Acct XXX437 Dr. INR 27,500.00 on 11/11/26 to NAME; UPI: 123; Bal INR 58,533.06
        Rule("""Acct X+(?<acct>\d+) Dr\. INR ${amt("amt")} on \S+ to (?<party>.+?); UPI: ?(?<ref>\d+); Bal INR ${amt("bal")}""", Direction.DEBIT, Channel.UPI),
        // Canara card: CardTxn:A/C XXX437 Dr INR 112.54 on .. to MERCHANT;RRN:123;Bal INR 86,033.06
        Rule("""CardTxn:A/C X+(?<acct>\d+) Dr INR ${amt("amt")} on .+? to (?<party>.+?);\s?RRN:\s?(?<ref>\d+);\s?Bal INR ${amt("bal")}""", Direction.DEBIT, Channel.CARD),
        // Canara: Acct XXX437 credited with INR 20,000.00 on 03/10/26 from NAME; UPI:123; Bal INR 66,972.98
        Rule("""Acct X+(?<acct>\d+) credited with INR ${amt("amt")} on \S+ from (?<party>.+?); UPI: ?(?<ref>\d+); Bal INR ${amt("bal")}""", Direction.CREDIT, Channel.UPI),
    )

    fun isBankSender(sender: String) = BANK_SENDERS.any { sender.contains(it, ignoreCase = true) }

    /** Cheap pre-filter so OTPs/promos aren't logged as "unparsed". */
    fun looksLikeTxn(body: String) =
        Regex("""\b(Rs|INR)\.?\s?\d""", RegexOption.IGNORE_CASE).containsMatchIn(body) &&
            !Regex("""\bOTP\b|declined|e-mandate""", RegexOption.IGNORE_CASE).containsMatchIn(body)

    fun parse(body: String): ParsedTxn? {
        val text = body.replace(Regex("\\s+"), " ")
        for (r in rules) {
            val m = r.regex.find(text) ?: continue
            return ParsedTxn(
                amountPaise = paise(m.g("amt")!!),
                direction = r.direction,
                account = m.g("acct")!!,
                channel = r.channel,
                counterparty = m.g("party")?.trim().orEmpty(),
                ref = m.g("ref"),
                balancePaise = m.g("bal")?.let(::paise),
            )
        }
        return null
    }

    fun paise(s: String): Long = BigDecimal(s.replace(",", "")).movePointRight(2).toLong()

    // Not every rule declares every group; a missing group name throws.
    private fun MatchResult.g(name: String) =
        try { groups[name]?.value } catch (_: IllegalArgumentException) { null }
}
