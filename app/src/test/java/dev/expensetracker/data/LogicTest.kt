package dev.expensetracker.data

import dev.expensetracker.ui.billsInMonth
import dev.expensetracker.work.reminderTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class LogicTest {
    private val jan31 = LocalDate.of(2026, 1, 31)
    private fun bill(merchant: String? = null, paid: Int = 0) =
        RecurringBill(name = "Rent", amountPaise = 1_000_000, period = Period.MONTHLY, anchorEpochDay = jan31.toEpochDay(), paidCount = paid, merchantKey = merchant)

    @Test fun budgetThresholds() {
        assertEquals(0, budgetLevel(79_99, 100_00))
        assertEquals(80, budgetLevel(80_00, 100_00))
        assertEquals(100, budgetLevel(100_00, 100_00))
        assertEquals(0, budgetLevel(500, 0))
    }

    @Test fun monthlyBillDoesNotDriftAfterFebruary() {
        assertEquals(LocalDate.of(2026, 2, 28), bill(paid = 1).dueDate())
        assertEquals(LocalDate.of(2026, 3, 31), bill(paid = 2).dueDate())
    }

    @Test fun billMatching() {
        val due = jan31
        assertTrue(bill().matches("ANYONE", 1_030_000, due.plusDays(3)))       // amount within 5%
        assertFalse(bill().matches("ANYONE", 1_100_000, due))                  // amount off by 10%
        assertFalse(bill().matches("ANYONE", 1_000_000, due.plusDays(6)))      // outside ±5 days
        assertTrue(bill("LANDLORD").matches("LANDLORD", 1, due))               // merchant beats amount
        assertFalse(bill("LANDLORD").matches("OTHER", 1_000_000, due))
    }

    @Test fun reminders() {
        val b = bill() // due Jan 31, remind 3 days before
        assertNull(reminderTitle(b, jan31.minusDays(4)))
        assertEquals("Rent due in 3 days", reminderTitle(b, jan31.minusDays(3)))
        assertEquals("Rent due tomorrow", reminderTitle(b, jan31.minusDays(1)))
        assertEquals("Rent due today", reminderTitle(b, jan31))
        assertEquals("Overdue: Rent", reminderTitle(b, jan31.plusDays(1)))
    }

    @Test fun calendarOccurrences() {
        val weekly = RecurringBill(name = "Milk", amountPaise = 100, period = Period.WEEKLY, anchorEpochDay = LocalDate.of(2026, 11, 2).toEpochDay())
        assertEquals(5, billsInMonth(listOf(weekly), YearMonth.of(2026, 11)).size) // Nov 2, 9, 16, 23, 30
        assertEquals(setOf(LocalDate.of(2026, 2, 28)), billsInMonth(listOf(bill()), YearMonth.of(2026, 2)).keys)
    }
}
