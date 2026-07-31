# Supabase Setup Instructions

1. Open your **Supabase Dashboard** and navigate to your project.
2. Select **SQL Editor** from the left-hand menu.
3. Click **+ New Query**.
4. Copy the entire contents of the `supabase_schema.sql` file located in the root of this repository.
5. Paste the SQL code into the editor and click **Run**.

This script will:
- Create all the necessary tables (Goals, Transactions, Loans, Accounts, etc.).
- Enable Row Level Security (RLS) on all tables to ensure data privacy.
- Create policies so users can only access and modify their own data based on the Clerk authentication token.
