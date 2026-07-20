-- Dropit 데이터베이스 스키마
-- Supabase 대시보드의 SQL Editor에서 한 번 실행하세요.

create table if not exists public.makers (
  id uuid primary key references auth.users(id) on delete cascade,
  name text not null check (char_length(trim(name)) between 1 and 20),
  initials text not null default '',
  role text not null check (char_length(trim(role)) between 1 and 40),
  bio text not null check (char_length(trim(bio)) between 1 and 100),
  tone text not null default '#d9e6ff',
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

alter table public.makers add column if not exists avatar_url text;

create table if not exists public.apps (
  id text primary key,
  owner_id uuid not null references auth.users(id) on delete cascade,
  name text not null check (char_length(trim(name)) between 1 and 40),
  tagline text not null check (char_length(trim(tagline)) between 1 and 80),
  description text not null default '' check (char_length(description) <= 500),
  category text not null check (category in ('게임', '생산성', '학습', '생성기', '소셜', '실험')),
  thumbnail_variant text not null default 'new',
  thumbnail_url text,
  app_url text not null,
  github_url text,
  maker jsonb not null,
  tech_tags text[] not null default '{}',
  plays integer not null default 0 check (plays >= 0),
  likes integer not null default 0 check (likes >= 0),
  created_at timestamptz not null default timezone('utc', now()),
  source text not null default 'submitted' check (source in ('seed', 'submitted'))
);

create table if not exists public.app_bookmarks (
  user_id uuid not null references auth.users(id) on delete cascade,
  app_id text not null references public.apps(id) on delete cascade,
  created_at timestamptz not null default timezone('utc', now()),
  primary key (user_id, app_id)
);

create table if not exists public.app_likes (
  user_id uuid not null references auth.users(id) on delete cascade,
  app_id text not null references public.apps(id) on delete cascade,
  created_at timestamptz not null default timezone('utc', now()),
  primary key (user_id, app_id)
);

create index if not exists apps_created_at_idx on public.apps (created_at desc);
create index if not exists apps_owner_id_idx on public.apps (owner_id);

alter table public.makers enable row level security;
alter table public.apps enable row level security;
alter table public.app_bookmarks enable row level security;
alter table public.app_likes enable row level security;

grant usage on schema public to anon, authenticated;
grant select on public.makers, public.apps to anon, authenticated;
grant insert, update on public.makers to authenticated;
grant insert, update, delete on public.apps to authenticated;
grant select, insert, delete on public.app_bookmarks to authenticated;
grant select, insert, delete on public.app_likes to authenticated;

drop policy if exists "makers are publicly readable" on public.makers;
create policy "makers are publicly readable"
  on public.makers for select
  using (true);

drop policy if exists "users can create their own maker profile" on public.makers;
create policy "users can create their own maker profile"
  on public.makers for insert to authenticated
  with check (auth.uid() = id);

drop policy if exists "users can update their own maker profile" on public.makers;
create policy "users can update their own maker profile"
  on public.makers for update to authenticated
  using (auth.uid() = id)
  with check (auth.uid() = id);

drop policy if exists "apps are publicly readable" on public.apps;
create policy "apps are publicly readable"
  on public.apps for select
  using (true);

drop policy if exists "users can create their own apps" on public.apps;
create policy "users can create their own apps"
  on public.apps for insert to authenticated
  with check (auth.uid() = owner_id);

drop policy if exists "users can update their own apps" on public.apps;
create policy "users can update their own apps"
  on public.apps for update to authenticated
  using (auth.uid() = owner_id)
  with check (auth.uid() = owner_id);

drop policy if exists "users can delete their own apps" on public.apps;
create policy "users can delete their own apps"
  on public.apps for delete to authenticated
  using (auth.uid() = owner_id);

drop policy if exists "users can read their own bookmarks" on public.app_bookmarks;
create policy "users can read their own bookmarks"
  on public.app_bookmarks for select to authenticated
  using (auth.uid() = user_id);

drop policy if exists "users can create their own bookmarks" on public.app_bookmarks;
create policy "users can create their own bookmarks"
  on public.app_bookmarks for insert to authenticated
  with check (auth.uid() = user_id);

drop policy if exists "users can delete their own bookmarks" on public.app_bookmarks;
create policy "users can delete their own bookmarks"
  on public.app_bookmarks for delete to authenticated
  using (auth.uid() = user_id);

drop policy if exists "users can read their own likes" on public.app_likes;
create policy "users can read their own likes"
  on public.app_likes for select to authenticated
  using (auth.uid() = user_id);

create or replace function public.increment_app_plays(p_app_id text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  update public.apps
  set plays = plays + 1
  where id = p_app_id;
end;
$$;

create or replace function public.toggle_app_like(p_app_id text)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  next_liked boolean;
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다.';
  end if;

  if exists (
    select 1
    from public.app_likes
    where user_id = auth.uid() and app_id = p_app_id
  ) then
    delete from public.app_likes
    where user_id = auth.uid() and app_id = p_app_id;

    update public.apps
    set likes = greatest(likes - 1, 0)
    where id = p_app_id;

    next_liked := false;
  else
    insert into public.app_likes (user_id, app_id)
    values (auth.uid(), p_app_id);

    update public.apps
    set likes = likes + 1
    where id = p_app_id;

    next_liked := true;
  end if;

  return next_liked;
end;
$$;

revoke all on function public.increment_app_plays(text) from public;
grant execute on function public.increment_app_plays(text) to anon, authenticated;

revoke all on function public.toggle_app_like(text) from public;
grant execute on function public.toggle_app_like(text) to authenticated;
