-- Run once in the Supabase SQL Editor if you already ran schema.sql before this change.
-- Mutual fund investments: one row per amount + date (lump sum or SIP instalment).
-- The fund's total units and average cost on `holdings` are kept in step by the app.
create table if not exists holding_lots (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    holding_id uuid not null references holdings(id) on delete cascade,
    invest_date date not null,
    amount numeric not null check (amount > 0),   -- INR put in
    nav numeric not null,                         -- NAV used (on invest_date, or the next published one)
    units numeric not null,                       -- amount / nav
    created_at timestamptz default now()
);
create index if not exists idx_holding_lots_holding on holding_lots(holding_id);

alter table holding_lots enable row level security;
drop policy if exists owner_all on holding_lots;
create policy owner_all on holding_lots for all using (user_id = auth.uid()) with check (user_id = auth.uid());
