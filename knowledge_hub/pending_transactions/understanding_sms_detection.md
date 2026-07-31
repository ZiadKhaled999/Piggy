# Understanding SMS Detection - User Guide

## How SMS Parsing Works (Privately & Locally)
Piggy Ledger inspects incoming financial SMS messages directly on your phone hardware. **No text messages are ever uploaded to external servers or cloud services.**

---

## What Information is Extracted?
When your bank sends a notification (e.g., `"You spent EGP 250.00 at Supermarket with CIB card ending 1234"`), Piggy Ledger extracts:
1. **Transaction Type**: Expense, Income, or Transfer.
2. **Amount**: Currency value (e.g., `250.00`).
3. **Merchant / Sender**: Name of store or receiver (e.g., `Supermarket`).
4. **Account Identifier**: Matched bank sender header (e.g., `CIB`).
5. **Timestamp**: Exact time the message arrived.

---

## Setting Up SMS Detection Permissions

### Step-by-Step Instructions:
1. Open Piggy Ledger and go to **Settings** > **SMS Automation**.
2. Toggle on **Auto-Detect Bank SMS**.
3. Android will display a system permission dialog asking to grant SMS Read permissions.
4. Tap **Allow**.

---

## Troubleshooting SMS Detection
- **Messages Not Appearing?**: Ensure the bank sender name is listed under **Account Identifiers** (e.g., `CIB`, `VodafoneCash`).
- **Battery Saver Interference**: Make sure battery optimization for Piggy Ledger is set to "Unrestricted" or "Optimized" so background notifications can be parsed immediately.
