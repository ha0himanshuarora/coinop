-- =========================================================
-- Coinop — Supabase schema (simplified, manual-entry edition)
-- Run once in the Supabase SQL editor on a fresh project.
-- Auth: email + password (Authentication > Providers > Email).
-- Tip for a personal app: turn OFF "Confirm email" so sign-up logs you in.
-- =========================================================

-- Per-user settings (currency + timezone; appearance is stored on-device)
create table app_settings (
    user_id uuid primary key default auth.uid() references auth.users(id) on delete cascade,
    currency_code text not null default 'INR',
    timezone text,
    updated_at timestamptz default now()
);

-- Bank accounts you create yourself (name + opening balance)
create table accounts (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    name text not null,
    opening_balance numeric not null default 0,
    sort int not null default 0,
    is_archived boolean not null default false,
    created_at timestamptz default now(),
    unique (user_id, name)
);

-- Dynamic categories
create table categories (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    name text not null,
    icon text,
    color text,                              -- hex like #F59E0B
    created_at timestamptz default now(),
    unique (user_id, name)
);

-- Keyword -> category rules (higher priority wins)
create table category_rules (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    keyword text not null,
    category_id uuid not null references categories(id) on delete cascade,
    priority int not null default 0,
    created_at timestamptz default now()
);
create index idx_rules_user_priority on category_rules(user_id, priority desc);

-- Money in / money out. amount is always positive; type says the direction.
-- A transfer between two of your own accounts is two rows sharing transfer_id,
-- flagged is_transfer so it never counts as spending or income.
create table entries (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    account_id uuid not null references accounts(id) on delete cascade,
    date date not null,
    amount numeric not null check (amount > 0),
    type text not null check (type in ('in', 'out')),
    category_id uuid references categories(id) on delete set null,
    note text,
    is_manual_category boolean not null default false,   -- rules never overwrite these
    is_transfer boolean not null default false,
    transfer_id uuid,
    created_at timestamptz default now()
);
create index idx_entries_account_date on entries(account_id, date desc);
create index idx_entries_user_date on entries(user_id, date);
create index idx_entries_transfer on entries(transfer_id);

-- Hierarchical budgets: annual -> monthly -> weekly -> daily
create table budgets (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    period_type text not null check (period_type in ('annual', 'monthly', 'weekly', 'daily')),
    parent_budget_id uuid references budgets(id) on delete cascade,
    category_id uuid references categories(id) on delete cascade,   -- null = overall budget
    start_date date not null,
    end_date date not null,
    amount numeric not null check (amount >= 0),
    created_at timestamptz default now()
);
create unique index uq_budget_period on budgets (
    user_id, period_type, start_date,
    coalesce(category_id, '00000000-0000-0000-0000-000000000000'::uuid)
);
create index idx_budgets_parent on budgets(parent_budget_id);

-- Demat / mutual-fund accounts (Upstox, Groww, ...) you create yourself
create table invest_accounts (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    name text not null,
    kind text not null default 'demat',
    created_at timestamptz default now(),
    unique (user_id, name)
);

-- Holdings you enter by hand. Prices are fetched live in the app.
--   kind = 'stock' -> symbol is a Yahoo Finance symbol, e.g. RELIANCE.NS
--   kind = 'mf'    -> symbol is the AMFI scheme code, e.g. 120503
-- avg_cost is per unit, always in INR (the market currency).
create table holdings (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    invest_account_id uuid not null references invest_accounts(id) on delete cascade,
    kind text not null check (kind in ('stock', 'mf')),
    symbol text not null,
    name text not null,
    quantity numeric not null check (quantity >= 0),
    avg_cost numeric not null default 0,
    created_at timestamptz default now()
);
create index idx_holdings_account on holdings(invest_account_id);

-- Home dashboard widgets
create table home_widgets (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    widget_type text not null,   -- 'balance_summary' | 'net_worth' | 'budget_progress' | 'top_categories'
    position int not null default 0,
    is_visible boolean not null default true,
    unique (user_id, widget_type)
);

-- Account balance = opening balance + money in - money out.
-- security_invoker makes the view obey the row level security of the caller.
create view account_balances with (security_invoker = true) as
select
    a.id,
    a.name,
    a.opening_balance,
    a.sort,
    a.is_archived,
    a.opening_balance + coalesce(sum(case e.type when 'in' then e.amount else -e.amount end), 0) as balance
from accounts a
left join entries e on e.account_id = a.id
group by a.id;

-- Used when you change the app currency and choose "convert at live rate".
create or replace function convert_all_amounts(rate numeric)
returns void
language plpgsql
security invoker
as $$
begin
    if rate is null or rate <= 0 then
        raise exception 'invalid rate';
    end if;
    update accounts set opening_balance = round(opening_balance * rate, 2) where user_id = auth.uid();
    update entries set amount = greatest(round(amount * rate, 2), 0.01) where user_id = auth.uid();
    update budgets set amount = round(amount * rate, 2) where user_id = auth.uid();
end;
$$;

-- =========================================================
-- Row level security: every row belongs to exactly one user
-- =========================================================
alter table app_settings enable row level security;
alter table accounts enable row level security;
alter table categories enable row level security;
alter table category_rules enable row level security;
alter table entries enable row level security;
alter table budgets enable row level security;
alter table invest_accounts enable row level security;
alter table holdings enable row level security;
alter table home_widgets enable row level security;

create policy owner_all on app_settings for all using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy owner_all on accounts for all using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy owner_all on categories for all using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy owner_all on category_rules for all using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy owner_all on entries for all using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy owner_all on budgets for all using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy owner_all on invest_accounts for all using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy owner_all on holdings for all using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy owner_all on home_widgets for all using (user_id = auth.uid()) with check (user_id = auth.uid());

-- Mutual fund investments (amount + date), added after the first release
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

-- Account actions (Settings > Account)
-- Delete all of the signed-in user's data but keep the login, currency/timezone and home widgets.
-- Everything else hangs off these four tables with "on delete cascade".
create or replace function public.delete_my_data()
returns void
language plpgsql
security invoker
set search_path = public
as $$
begin
    delete from accounts        where user_id = auth.uid();
    delete from categories      where user_id = auth.uid();
    delete from budgets         where user_id = auth.uid();
    delete from invest_accounts where user_id = auth.uid();
end;
$$;

-- Delete the login itself; every table references auth.users with "on delete cascade".
create or replace function public.delete_my_account()
returns void
language plpgsql
security definer
set search_path = public, auth
as $$
begin
    delete from auth.users where id = auth.uid();
end;
$$;

revoke all on function public.delete_my_data() from public, anon;
revoke all on function public.delete_my_account() from public, anon;
grant execute on function public.delete_my_data() to authenticated;
grant execute on function public.delete_my_account() to authenticated;
