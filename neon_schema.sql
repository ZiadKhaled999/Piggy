-- Run this in your Neon / PostgreSQL SQL Editor

-- If you previously created tables with UUID columns, drop them or alter column types:
-- DROP TABLE IF EXISTS pending_transactions, account_transactions, accounts, loan_payments, loans, transactions, goals CASCADE;

-- 1. Create Goals table
CREATE TABLE IF NOT EXISTS goals (
    id TEXT PRIMARY KEY,
    "userId" TEXT NOT NULL,
    name TEXT NOT NULL,
    "targetAmount" DOUBLE PRECISION NOT NULL,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL,
    "isSynced" BOOLEAN DEFAULT true,
    "is_deleted" BOOLEAN DEFAULT false
);

-- 2. Create Transactions table
CREATE TABLE IF NOT EXISTS transactions (
    id TEXT PRIMARY KEY,
    "userId" TEXT NOT NULL,
    "goalId" TEXT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    amount DOUBLE PRECISION NOT NULL,
    note TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    deadline BIGINT,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL,
    "isSynced" BOOLEAN DEFAULT true,
    "is_deleted" BOOLEAN DEFAULT false
);

-- 3. Create Loans table
CREATE TABLE IF NOT EXISTS loans (
    id TEXT PRIMARY KEY,
    "userId" TEXT NOT NULL,
    type TEXT NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    "contactName" TEXT NOT NULL,
    phone TEXT,
    email TEXT,
    "photoUri" TEXT,
    social TEXT,
    note TEXT NOT NULL,
    "isPaidOff" BOOLEAN DEFAULT false,
    timestamp BIGINT NOT NULL,
    deadline BIGINT,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL,
    "isSynced" BOOLEAN DEFAULT true,
    "is_deleted" BOOLEAN DEFAULT false
);

-- 4. Create Loan Payments table
CREATE TABLE IF NOT EXISTS loan_payments (
    id TEXT PRIMARY KEY,
    "userId" TEXT NOT NULL,
    "loanId" TEXT NOT NULL REFERENCES loans(id) ON DELETE CASCADE,
    amount DOUBLE PRECISION NOT NULL,
    timestamp BIGINT NOT NULL,
    note TEXT,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL,
    "isSynced" BOOLEAN DEFAULT true,
    "is_deleted" BOOLEAN DEFAULT false
);

-- 5. Create Accounts table
CREATE TABLE IF NOT EXISTS accounts (
    id TEXT PRIMARY KEY,
    "userId" TEXT NOT NULL,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    icon_color TEXT NOT NULL,
    icon_name TEXT NOT NULL,
    logo_url TEXT,
    local_logo_path TEXT,
    currency TEXT NOT NULL,
    starting_balance DOUBLE PRECISION NOT NULL,
    current_balance DOUBLE PRECISION NOT NULL,
    exclude_from_all BOOLEAN DEFAULT false,
    credit_limit DOUBLE PRECISION,
    available_credit DOUBLE PRECISION,
    payment_due_day INTEGER,
    card_numbers TEXT,
    bank_account_no TEXT,
    provider TEXT,
    insta_pay_fee BOOLEAN DEFAULT false,
    label TEXT,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL,
    "isSynced" BOOLEAN DEFAULT true,
    "is_deleted" BOOLEAN DEFAULT false
);

-- 6. Create Account Transactions table
CREATE TABLE IF NOT EXISTS account_transactions (
    id TEXT PRIMARY KEY,
    "userId" TEXT NOT NULL,
    account_id TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    amount DOUBLE PRECISION NOT NULL,
    merchant TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    source TEXT NOT NULL,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL,
    "isSynced" BOOLEAN DEFAULT true,
    "is_deleted" BOOLEAN DEFAULT false
);

-- 7. Create Pending Transactions table
CREATE TABLE IF NOT EXISTS pending_transactions (
    id TEXT PRIMARY KEY,
    "userId" TEXT NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    merchant TEXT NOT NULL,
    raw_sms_body TEXT NOT NULL,
    sender TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL,
    "isSynced" BOOLEAN DEFAULT true,
    "is_deleted" BOOLEAN DEFAULT false
);

-- 8. Create User Preferences & Onboarding Table
CREATE TABLE IF NOT EXISTS user_preferences (
    "userId" TEXT PRIMARY KEY,
    "hasOnboarded" BOOLEAN DEFAULT false,
    "hasLanguageSelected" BOOLEAN DEFAULT false,
    "hasHeardAboutUs" BOOLEAN DEFAULT false,
    "personalizedIntent" INTEGER DEFAULT -1,
    "personalizedIntensity" INTEGER DEFAULT -1,
    "savingMode" TEXT DEFAULT 'piggy',
    "customIdentifiersJson" TEXT DEFAULT '{}',
    "isBiometricLockEnabled" BOOLEAN DEFAULT false,
    "isScreenshotProtectionEnabled" BOOLEAN DEFAULT false,
    "isPremium" BOOLEAN DEFAULT false,
    "premiumExpiryTimestamp" BIGINT DEFAULT 0,
    "isLifetimePremium" BOOLEAN DEFAULT false,
    "preferredAccountId" TEXT,
    "appCurrency" TEXT DEFAULT 'USD',
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL,
    "isSynced" BOOLEAN DEFAULT true,
    "is_deleted" BOOLEAN DEFAULT false
);

-- 9. Create Streak Dates Table
CREATE TABLE IF NOT EXISTS streak_dates (
    id TEXT PRIMARY KEY,
    "userId" TEXT NOT NULL,
    "dateStr" TEXT NOT NULL,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL,
    "isSynced" BOOLEAN DEFAULT true,
    "is_deleted" BOOLEAN DEFAULT false
);

-- 10. Create AI Conversations Table
CREATE TABLE IF NOT EXISTS ai_conversations (
    id TEXT PRIMARY KEY,
    "userId" TEXT NOT NULL,
    title TEXT NOT NULL,
    "isPinned" BOOLEAN DEFAULT false,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL,
    "isSynced" BOOLEAN DEFAULT true,
    "is_deleted" BOOLEAN DEFAULT false
);

-- 11. Create AI Chat Messages Table
CREATE TABLE IF NOT EXISTS ai_chat_messages (
    id TEXT PRIMARY KEY,
    "userId" TEXT NOT NULL,
    "conversationId" TEXT NOT NULL,
    role TEXT NOT NULL,
    content TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL,
    "isSynced" BOOLEAN DEFAULT true,
    "is_deleted" BOOLEAN DEFAULT false
);

-- 12. Create Onboarding Answers Table
CREATE TABLE IF NOT EXISTS onboarding_answers (
    id TEXT PRIMARY KEY,
    "userId" TEXT,
    "key" TEXT NOT NULL,
    "value" TEXT NOT NULL,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL,
    "isSynced" BOOLEAN DEFAULT true,
    "is_deleted" BOOLEAN DEFAULT false
);

-- ENABLE ROW LEVEL SECURITY (RLS) FOR ALL TABLES
ALTER TABLE goals ENABLE ROW LEVEL SECURITY;
ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE loans ENABLE ROW LEVEL SECURITY;
ALTER TABLE loan_payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE account_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE pending_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE streak_dates ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_chat_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE onboarding_answers ENABLE ROW LEVEL SECURITY;

-- DROP OLD POLICIES IF PRESENT
DROP POLICY IF EXISTS "Enable all access for goals" ON goals;
DROP POLICY IF EXISTS "Enable all access for transactions" ON transactions;
DROP POLICY IF EXISTS "Enable all access for loans" ON loans;
DROP POLICY IF EXISTS "Enable all access for loan_payments" ON loan_payments;
DROP POLICY IF EXISTS "Enable all access for accounts" ON accounts;
DROP POLICY IF EXISTS "Enable all access for account_transactions" ON account_transactions;
DROP POLICY IF EXISTS "Enable all access for pending_transactions" ON pending_transactions;
DROP POLICY IF EXISTS "Enable all access for user_preferences" ON user_preferences;
DROP POLICY IF EXISTS "Enable all access for streak_dates" ON streak_dates;
DROP POLICY IF EXISTS "Enable all access for ai_conversations" ON ai_conversations;
DROP POLICY IF EXISTS "Enable all access for ai_chat_messages" ON ai_chat_messages;
DROP POLICY IF EXISTS "Enable all access for onboarding_answers" ON onboarding_answers;

DROP POLICY IF EXISTS "Users can manage their own goals" ON goals;
DROP POLICY IF EXISTS "Users can manage their own transactions" ON transactions;
DROP POLICY IF EXISTS "Users can manage their own loans" ON loans;
DROP POLICY IF EXISTS "Users can manage their own loan_payments" ON loan_payments;
DROP POLICY IF EXISTS "Users can manage their own accounts" ON accounts;
DROP POLICY IF EXISTS "Users can manage their own account_transactions" ON account_transactions;
DROP POLICY IF EXISTS "Users can manage their own pending_transactions" ON pending_transactions;

-- 13. Assets table (new)
CREATE TABLE IF NOT EXISTS assets (
    id TEXT PRIMARY KEY,
    "userId" TEXT NOT NULL,
    type TEXT NOT NULL,
    name TEXT NOT NULL,
    "weightGrams" DOUBLE PRECISION DEFAULT 0,
    purity TEXT DEFAULT '21k',
    "principalAmount" DOUBLE PRECISION DEFAULT 0,
    "annualRate" DOUBLE PRECISION DEFAULT 0,
    "purchasePrice" DOUBLE PRECISION DEFAULT 0,
    "startDate" BIGINT NOT NULL,
    "maturityDate" BIGINT,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL,
    "isSynced" BOOLEAN DEFAULT false,
    "is_deleted" BOOLEAN DEFAULT false
);

-- 14. Live market rates table (new, singleton)
CREATE TABLE IF NOT EXISTS live_market_rates (
    id TEXT PRIMARY KEY,
    "gold24k" DOUBLE PRECISION DEFAULT 4120.0,
    "gold21k" DOUBLE PRECISION DEFAULT 3605.0,
    "gold18k" DOUBLE PRECISION DEFAULT 3090.0,
    "silver999" DOUBLE PRECISION DEFAULT 52.0,
    "silver925" DOUBLE PRECISION DEFAULT 48.0,
    "silver900" DOUBLE PRECISION DEFAULT 46.8,
    "lastUpdated" BIGINT NOT NULL
);

-- 15. Portfolio history table (new)
CREATE TABLE IF NOT EXISTS portfolio_history (
    id TEXT PRIMARY KEY,
    date BIGINT NOT NULL,
    "totalPortfolioValue" DOUBLE PRECISION DEFAULT 0
);

-- 16. Journal entries table (new)
CREATE TABLE IF NOT EXISTS journal_entries (
    id TEXT PRIMARY KEY,
    "userId" TEXT NOT NULL,
    date BIGINT NOT NULL,
    description TEXT NOT NULL,
    type TEXT NOT NULL,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL,
    "isSynced" BOOLEAN DEFAULT false,
    "is_deleted" BOOLEAN DEFAULT false
);

-- 17. Postings table (new)
CREATE TABLE IF NOT EXISTS postings (
    id TEXT PRIMARY KEY,
    "journalEntryId" TEXT NOT NULL,
    "accountId" TEXT NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    "isDebit" BOOLEAN DEFAULT false,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL,
    "isSynced" BOOLEAN DEFAULT false,
    "is_deleted" BOOLEAN DEFAULT false
);
CREATE INDEX IF NOT EXISTS idx_postings_journal ON postings("journalEntryId");
CREATE INDEX IF NOT EXISTS idx_postings_account ON postings("accountId");

-- 18. Subscriptions table (new)
CREATE TABLE IF NOT EXISTS subscriptions (
    id TEXT PRIMARY KEY,
    "userId" TEXT NOT NULL,
    name TEXT NOT NULL,
    "logoUrl" TEXT,
    amount DOUBLE PRECISION NOT NULL,
    currency TEXT DEFAULT 'EGP',
    "billingCycle" TEXT DEFAULT 'MONTHLY',
    "nextRenewalDate" BIGINT NOT NULL,
    "accountId" TEXT DEFAULT '',
    "autoDebit" BOOLEAN DEFAULT false,
    "isActive" BOOLEAN DEFAULT true,
    category TEXT DEFAULT 'subscriptions',
    status TEXT DEFAULT 'ACTIVE',
    "hasFreeTrial" BOOLEAN DEFAULT false,
    "trialDays" INTEGER DEFAULT 0,
    "trialEndsAt" BIGINT DEFAULT 0,
    "totalPaidCount" INTEGER DEFAULT 0,
    "totalSpent" DOUBLE PRECISION DEFAULT 0,
    "reminderEnabled" BOOLEAN DEFAULT true,
    "reminderLeadDays" INTEGER DEFAULT 2,
    "reminderDailyRepeat" BOOLEAN DEFAULT false,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL,
    "isSynced" BOOLEAN DEFAULT false,
    "is_deleted" BOOLEAN DEFAULT false
);
CREATE INDEX IF NOT EXISTS idx_subscriptions_account ON subscriptions("accountId");

-- Add missing columns to existing tables (newly added data fields)
ALTER TABLE goals ADD COLUMN IF NOT EXISTS "backedByAssetId" TEXT;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS "accountId" TEXT;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS "goldWeightGrams" DOUBLE PRECISION;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS "goldKarat" INTEGER;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS "isAutoUpdateGoldPrice" BOOLEAN DEFAULT false;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS "certificatePrincipal" DOUBLE PRECISION;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS "certificateInterestRate" DOUBLE PRECISION;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS "certificatePayoutFrequency" TEXT;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS "isSystemAccount" BOOLEAN DEFAULT false;

-- ENABLE RLS FOR NEW TABLES
ALTER TABLE assets ENABLE ROW LEVEL SECURITY;
ALTER TABLE live_market_rates ENABLE ROW LEVEL SECURITY;
ALTER TABLE portfolio_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE journal_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE postings ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;

-- DROP OLD POLICIES IF PRESENT (new tables)
DROP POLICY IF EXISTS "Enable all access for assets" ON assets;
DROP POLICY IF EXISTS "Enable all access for live_market_rates" ON live_market_rates;
DROP POLICY IF EXISTS "Enable all access for portfolio_history" ON portfolio_history;
DROP POLICY IF EXISTS "Enable all access for journal_entries" ON journal_entries;
DROP POLICY IF EXISTS "Enable all access for postings" ON postings;
DROP POLICY IF EXISTS "Enable all access for subscriptions" ON subscriptions;

-- CREATE PERMISSIVE POLICIES FOR NEW TABLES
CREATE POLICY "Enable all access for assets" ON assets FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Enable all access for live_market_rates" ON live_market_rates FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Enable all access for portfolio_history" ON portfolio_history FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Enable all access for journal_entries" ON journal_entries FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Enable all access for postings" ON postings FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Enable all access for subscriptions" ON subscriptions FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Enable all access for goals" ON goals FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Enable all access for transactions" ON transactions FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Enable all access for loans" ON loans FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Enable all access for loan_payments" ON loan_payments FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Enable all access for accounts" ON accounts FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Enable all access for account_transactions" ON account_transactions FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Enable all access for pending_transactions" ON pending_transactions FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Enable all access for user_preferences" ON user_preferences FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Enable all access for streak_dates" ON streak_dates FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Enable all access for ai_conversations" ON ai_conversations FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Enable all access for ai_chat_messages" ON ai_chat_messages FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Enable all access for onboarding_answers" ON onboarding_answers FOR ALL USING (true) WITH CHECK (true);

