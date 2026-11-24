package com.oryno.piggy_ledger.service

import com.oryno.piggy_ledger.data.Loan
import com.oryno.piggy_ledger.data.LoanPayment
import com.oryno.piggy_ledger.data.LoanType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DeadlineAndReminderUtilsTest {

    @Test
    fun computeRemainingLoanAmount_subtractsActivePaymentsAndIgnoresDeleted() {
        val loan = Loan(
            id = "loan-1",
            type = LoanType.LENT,
            amount = 1000.0,
            contactName = "Ahmed",
            note = "Test",
            deadline = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)
        )
        val payments = listOf(
            LoanPayment(loanId = "loan-1", amount = 250.0, is_deleted = false),
            LoanPayment(loanId = "loan-1", amount = 150.0, is_deleted = false),
            LoanPayment(loanId = "loan-1", amount = 500.0, is_deleted = true),
            LoanPayment(loanId = "loan-2", amount = 900.0, is_deleted = false)
        )

        val remaining = DeadlineAndReminderUtils.computeRemainingLoanAmount(loan, payments)
        assertEquals(600.0, remaining, 0.001)
    }

    @Test
    fun filterOverdueLoansWithRemaining_excludesSettledPaidDeletedAndFutureLoans() {
        val now = System.currentTimeMillis()
        val pastDeadline = now - TimeUnit.DAYS.toMillis(1)
        val futureDeadline = now + TimeUnit.DAYS.toMillis(3)

        val partialLoan = Loan(
            id = "loan-partial",
            type = LoanType.BORROWED,
            amount = 500.0,
            contactName = "Sara",
            note = "Partial",
            deadline = pastDeadline
        )
        val settledViaLogsLoan = Loan(
            id = "loan-settled",
            type = LoanType.LENT,
            amount = 300.0,
            contactName = "Omar",
            note = "Settled",
            deadline = pastDeadline
        )
        val paidOffLoan = Loan(
            id = "loan-paid",
            type = LoanType.LENT,
            amount = 400.0,
            contactName = "Mona",
            note = "Paid",
            isPaidOff = true,
            deadline = pastDeadline
        )
        val deletedLoan = Loan(
            id = "loan-deleted",
            type = LoanType.LENT,
            amount = 400.0,
            contactName = "Tarek",
            note = "Deleted",
            is_deleted = true,
            deadline = pastDeadline
        )
        val futureLoan = Loan(
            id = "loan-future",
            type = LoanType.LENT,
            amount = 400.0,
            contactName = "Ali",
            note = "Future",
            deadline = futureDeadline
        )

        val payments = listOf(
            LoanPayment(loanId = "loan-partial", amount = 175.0),
            LoanPayment(loanId = "loan-settled", amount = 200.0),
            LoanPayment(loanId = "loan-settled", amount = 100.0)
        )

        val result = DeadlineAndReminderUtils.filterOverdueLoansWithRemaining(
            loans = listOf(partialLoan, settledViaLogsLoan, paidOffLoan, deletedLoan, futureLoan),
            payments = payments,
            nowMillis = now
        )

        assertEquals(1, result.size)
        assertEquals("loan-partial", result[0].first.id)
        assertEquals(325.0, result[0].second, 0.001)
    }

    @Test
    fun calendarDaysBetween_handlesSameDayNextDayAndOverdue() {
        val base = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 10, 22, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val sameDayEarlier = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 10, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val tomorrowMorning = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 11, 7, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val yesterday = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 9, 23, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertEquals(0, DeadlineAndReminderUtils.calendarDaysBetween(base, sameDayEarlier))
        assertEquals(1, DeadlineAndReminderUtils.calendarDaysBetween(base, tomorrowMorning))
        assertEquals(-1, DeadlineAndReminderUtils.calendarDaysBetween(base, yesterday))
    }

    @Test
    fun reminderOptionConversions_roundTripSupportedLeadDays() {
        assertEquals(0, DeadlineAndReminderUtils.parseReminderOptionToDays("On renewal day"))
        assertEquals(1, DeadlineAndReminderUtils.parseReminderOptionToDays("1 day before"))
        assertEquals(2, DeadlineAndReminderUtils.parseReminderOptionToDays("2 days before"))
        assertEquals(3, DeadlineAndReminderUtils.parseReminderOptionToDays("3 days before"))
        assertEquals(7, DeadlineAndReminderUtils.parseReminderOptionToDays("1 week before"))
        assertNull(DeadlineAndReminderUtils.parseReminderOptionToDays("None"))

        assertEquals("On renewal day", DeadlineAndReminderUtils.daysToReminderOption(0))
        assertEquals("1 day before", DeadlineAndReminderUtils.daysToReminderOption(1))
        assertEquals("2 days before", DeadlineAndReminderUtils.daysToReminderOption(2))
        assertEquals("3 days before", DeadlineAndReminderUtils.daysToReminderOption(3))
        assertEquals("1 week before", DeadlineAndReminderUtils.daysToReminderOption(7))
    }

    @Test
    fun notificationScheduler_assignsDistinctRequestCodesPerDeadlineSlot() {
        val codes = NotificationScheduler.DEADLINE_ALARM_HOURS.map { hour ->
            NotificationScheduler.alarmRequestCode(NotificationWorker.TYPE_DEADLINE, hour)
        }
        assertEquals(3, codes.size)
        assertEquals(3, codes.distinct().size)
    }
}
