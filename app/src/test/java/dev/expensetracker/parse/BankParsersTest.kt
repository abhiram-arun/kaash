package dev.expensetracker.parse

import dev.expensetracker.parse.Channel.*
import dev.expensetracker.parse.Direction.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankParsersTest {
    private fun p(i: Int) = BankParsers.parse(SAMPLE_SMS[i].second)!!

    @Test fun federalUpiDebit() = assertEquals(
        ParsedTxn(7700, DEBIT, "8868", UPI, "MANIGANDAN U", "668635377490", 1048944), p(0))

    @Test fun federalCardSpend() {
        assertEquals(ParsedTxn(2400, DEBIT, "6612", CARD, "631948987062 anil s", null, null), p(1))
        assertEquals("ANIL S", p(1).merchantKey)
    }

    @Test fun canaraUpiDebit() = assertEquals(
        ParsedTxn(2750000, DEBIT, "437", UPI, "myG KOLLAM F", "668125295496", 5853306), p(2))

    @Test fun canaraCardDebit() = assertEquals(
        ParsedTxn(11254, DEBIT, "437", CARD, "norealNOIDA", "631205681380", 8603306), p(3))

    @Test fun canaraUpiCredit() = assertEquals(
        ParsedTxn(2000000, CREDIT, "437", UPI, "ABHIRAM ARUN", "130616170003", 6697298), p(4))

    @Test fun federalCredit() = assertEquals(
        ParsedTxn(55333, CREDIT, "8868", OTHER, "", null, 76119), p(5))

    @Test fun nonTransactionReturnsNull() {
        val otp = "123456 is your OTP for login to FedMobile. Do not share it with anyone -Federal Bank"
        assertNull(BankParsers.parse(otp))
        assertFalse(BankParsers.looksLikeTxn(otp))
    }

    @Test fun senderAllowlist() {
        assertTrue(BankParsers.isBankSender("JD-FEDBNK-S"))
        assertTrue(BankParsers.isBankSender("AX-CANBNK"))
        assertFalse(BankParsers.isBankSender("+919876543210")) // a phone number can't spoof a bank alert
    }

    @Test fun paise() {
        assertEquals(123450, BankParsers.paise("1,234.5"))
        assertEquals(7700, BankParsers.paise("77"))
    }
}
