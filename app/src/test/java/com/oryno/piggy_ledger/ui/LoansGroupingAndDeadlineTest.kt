package com.oryno.piggy_ledger.ui

import com.oryno.piggy_ledger.data.Loan
import com.oryno.piggy_ledger.data.LoanPayment
import com.oryno.piggy_ledger.data.LoanType
import com.oryno.piggy_ledger.service.DeadlineAndReminderUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class LoansGroupingAndDeadlineTest {

    @Test
    fun groupLoansByContact_gathersSameContactLogsIgnoringCaseAndWhitespace() {
        val loans = listOf(
            Loan(id = "1", type = LoanType.LENT, amount = 100.0, contactName = "John", note = "Lunch"),
            Loan(id = "2", type = LoanType.BORROWED, amount = 40.0, contactName = "Sara", note = "Coffee"),
            Loan(id = "3", type = LoanType.LENT, amount = 250.0, contactName = "  john ", note = "Tickets"),
            Loan(id = "4", type = LoanType.BORROWED, amount = 50.0, contactName = "JOHN", note = "Uber")
        )

        val groups = groupLoansByContact(loans)

        assertEquals(2, groups.size)
        val johnGroup = groups[0]
        assertEquals("john", johnGroup.key)
        assertEquals("John", johnGroup.displayName)
        assertEquals(listOf("1", "3", "4"), johnGroup.loans.map { it.id })

        val saraGroup = groups[1]
        assertEquals("sara", saraGroup.key)
        assertEquals("Sara", saraGroup.displayName)
        assertEquals(listOf("2"), saraGroup.loans.map { it.id })
    }

    @Test
    fun groupLoansByContact_keepsBlankNamedLoansSeparate() {
        val loans = listOf(
            Loan(id = "blank-1", type = LoanType.LENT, amount = 50.0, contactName = "   ", note = "A"),
            Loan(id = "blank-2", type = LoanType.LENT, amount = 75.0, contactName = "", note = "B")
        )

        val groups = groupLoansByContact(loans)

        assertEquals(2, groups.size)
        assertEquals(listOf("blank-1"), groups[0].loans.map { it.id })
        assertEquals(listOf("blank-2"), groups[1].loans.map { it.id })
    }

    @Test
    fun overdueFilter_excludesRemovedOrPaidOffLogsWhilePreservingActiveSiblingsForSameContact() {
        val now = System.currentTimeMillis()
        val pastDeadline = now - TimeUnit.DAYS.toMillis(2)

        val johnOverdue = Loan(
            id = "loan-john-1",
            type = LoanType.LENT,
            amount = 500.0,
            contactName = "John",
            note = "Rent split",
            deadline = pastDeadline
        )
        val johnSecondOverdue = Loan(
            id = "loan-john-2",
            type = LoanType.BORROWED,
            amount = 150.0,
            contactName = "John",
            note = "Dinner",
            deadline = pastDeadline
        )

        // Initially both overdue logs are returned
        val initialOverdue = DeadlineAndReminderUtils.filterOverdueLoansWithRemaining(
            loans = listOf(johnOverdue, johnSecondOverdue),
            payments = emptyList(),
            nowMillis = now
        )
        assertEquals(2, initialOverdue.size)

        // Deleting the first log removes it immediately from overdue candidates, leaving only the second
        val afterFirstDeleted = DeadlineAndReminderUtils.filterOverdueLoansWithRemaining(
            loans = listOf(johnOverdue.copy(is_deleted = true), johnSecondOverdue),
            payments = emptyList(),
            nowMillis = now
        )
        assertEquals(listOf("loan-john-2"), afterFirstDeleted.map { it.first.id })

        // Paying off / deleting the remaining log leaves zero overdue loans
        val afterBothRemovedOrSettled = DeadlineAndReminderUtils.filterOverdueLoansWithRemaining(
            loans = listOf(
                johnOverdue.copy(is_deleted = true),
                johnSecondOverdue
            ),
            payments = listOf(
                LoanPayment(id = "pay-1", loanId = johnSecondOverdue.id, amount = 150.0)
            ),
            nowMillis = now
        )
        assertTrue(afterBothRemovedOrSettled.isEmpty())
    }
}
