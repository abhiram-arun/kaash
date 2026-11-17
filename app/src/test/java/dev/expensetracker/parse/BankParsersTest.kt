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
        assertEquals(ParsedTxn(2400, DEBIT, "6612", CARD, "631948987062 anil s", null, 1630587), p(1)) // bal = available limit
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

    @Test fun federalUpiMandate() = assertEquals(
        ParsedTxn(300000, DEBIT, "8868", UPI, "Indian Clear", "667207262930", null), p(6))

    @Test fun federalNeftCredit() = assertEquals(
        ParsedTxn(389200, CREDIT, "8868", OTHER, "JUSPAY TEC", "HDFCH01401356531", 2065539), p(7))

    @Test fun federalCardPaymentReceived() = assertEquals(
        ParsedTxn(84900, CREDIT, "6612", CARD, "", null, null), p(8))

    @Test fun declinedAndOtpAreNotLoggedAsUnparsed() {
        assertFalse(BankParsers.looksLikeTxn("Hi, e-mandate on Federal Bank Credit Card 5599 is active for Merchant: Anthropic, Amt: INR 2399.00, Freq: monthly"))
        assertFalse(BankParsers.looksLikeTxn("231082 is the OTP for txn of INR 112.54 at BIGTREE EN on your Debit Card ending 0003 valid for 5 mins.-Canara Bank"))
        assertFalse(BankParsers.looksLikeTxn("Trxn of INR 2,399.00 at anthropic* claude sub on 01-11-2026 declined on your Credit Card ending with 5599 - Federal Bank"))
        assertTrue(BankParsers.looksLikeTxn("Rs.500 debited from a/c XX8868 towards something new"))
    }

    @Test fun nonTransactionReturnsNull() {
        val otp = "123456 is your OTP for login to FedMobile. Do not share it with anyone -Federal Bank"
        assertNull(BankParsers.parse(otp))
        assertFalse(BankParsers.looksLikeTxn(otp))
    }

    @Test fun senderAllowlist() {
        assertTrue(BankParsers.isBankSender("JD-FEDBNK-S"))
        assertTrue(BankParsers.isBankSender("AX-CANBNK"))
        assertTrue(BankParsers.isBankSender("AD-FEDSMS-S"))
        assertFalse(BankParsers.isBankSender("+919876543210")) // a phone number can't spoof a bank alert
    }

    @Test fun paise() {
        assertEquals(123450, BankParsers.paise("1,234.5"))
        assertEquals(7700, BankParsers.paise("77"))
    }
}
