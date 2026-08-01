# Auto-Categorization Rules - User Guide

## Overview
Auto-categorization rules allow Piggy Ledger to automatically assign spending categories (e.g., *Groceries*, *Fuel*, *Streaming*) to parsed transactions based on merchant keywords.

---

## How Smart Keyword Mapping Works

When an SMS contains specific merchant names, Piggy Ledger checks your auto-categorization keywords:

| Merchant Keyword in SMS | Automatically Assigned Category |
| :--- | :--- |
| `Uber`, `Careem`, `Gasoline`, `Taqa` | **Transportation** |
| `Carrefour`, `Seoudi`, `Lulu`, `Metro` | **Groceries** |
| `Netflix`, `Spotify`, `Cinema`, `PlayStation` | **Entertainment** |
| `McDonalds`, `Starbucks`, `KFC`, `Costa` | **Dining Out / Coffee** |
| `Vodafone Bill`, `Electricity`, `Gas`, `Water` | **Utilities & Bills** |

---

## Customizing Category Rules

### Step-by-Step Instructions:
1. Navigate to **Settings** > **Auto-Categorization Rules**.
2. Tap **+ Add Rule**.
3. **Keyword**: Enter the merchant or keyword to look out for (e.g., `Gourmet`).
4. **Target Category**: Select the matching category (e.g., `Groceries`).
5. Tap **Save Rule**.

Now, whenever an incoming SMS mentions that keyword, Piggy Ledger pre-selects your chosen category automatically!
