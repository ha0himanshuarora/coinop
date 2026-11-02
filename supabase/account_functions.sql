-- Run once in the Supabase SQL Editor. Used by Settings > Account.

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
