# Account Identifiers - Knowledge Hub Overview

## Introduction
**Account Identifiers** are smart text tags and SMS sender header keywords that link incoming notifications to your accounts in Piggy Ledger.

---

## What is an Account Identifier?
When a bank sends an SMS (e.g., from sender `CIB-EG` or `V-Cash`), Android identifies the sender by an alphanumeric header. An **Account Identifier** tells Piggy Ledger: *"Whenever an SMS arrives from 'CIB-EG', route this transaction to my CIB Bank Account."*

---

## Default Built-in Identifiers
Piggy Ledger comes pre-configured with dozens of popular Egyptian and international bank headers:
- **CIB**: `CIB`, `CIB-EG`, `CIBBank`
- **National Bank of Egypt**: `NBE`, `NBE-EG`, `AlAhli`
- **Banque Misr**: `BanqueMisr`, `BM-EG`
- **Vodafone Cash**: `VodafoneCash`, `V-Cash`, `Vodafone`
- **InstaPay**: `InstaPay`, `IPN`
- **Orange Cash / Etisalat / WE**: `OrangeCash`, `EtisalatCash`, `WEPay`

---

## Why Manage Account Identifiers?
- Ensure 100% accurate SMS parsing without wrong account assignment.
- Add custom bank senders or foreign bank headers if traveling abroad.
