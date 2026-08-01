# Calculating Remaining Balances - User Guide

## Overview
Understanding loan math in Piggy Ledger ensures complete financial transparency. This document explains the exact calculations used across debt summaries.

---

## Formulas Used in Payoffs & Loans

### 1. Loan Remaining Balance
$$\text{Remaining Balance} = \text{Original Principal Amount} - \sum (\text{All Partial Repayments Logged})$$

### 2. Total Receivables (Money Owed to You)
$$\text{Total Receivables} = \sum (\text{Remaining Balances of All Active "I Lent" Loans})$$

### 3. Total Payables (Debts You Owe)
$$\text{Total Payables} = \sum (\text{Remaining Balances of All Active "I Borrowed" Debts})$$

---

## Impact on Net Worth Calculation

```
Net Worth = (Total Liquid Account Balances + Total Receivables) - Total Payables
```

### Why This Formula Protects You:
- If you lend $1,000 out of your bank account, your bank account drops by $1,000, but your **Net Worth does not decrease** because the $1,000 is transferred to **Total Receivables**.
- As the borrower repays you $400 cash, your Receivables drop to $600, and your Cash balance increases by $400. Net Worth remains perfectly balanced throughout the process!
