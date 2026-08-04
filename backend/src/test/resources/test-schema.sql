create extension if not exists pgcrypto;

drop table if exists public.app_comments cascade;
drop table if exists public.app_bookmarks cascade;
drop table if exists public.app_likes cascade;
drop table if exists public.site_visitors cascade;
drop table if exists public.crew_members cascade;
drop table if exists public.crew_access_codes cascade;
drop table if exists public.apps cascade;
drop table if exists public.makers cascade;

create table public.makers (
  id uuid primary key,
  name text not null,
  initials text not null default '',
  avatar_url text,
  role text not null,
  bio text not null,
  tone text not null default '#d9e6ff',
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create table public.crew_access_codes (
  id bigint generated always as identity primary key,
  code_hash text not null unique,
  active boolean not null default true,
  created_at timestamptz not null default timezone('utc', now())
);

create table public.crew_members (
  user_id uuid primary key,
  verified_at timestamptz not null default timezone('utc', now())
);

create table public.apps (
  id text primary key,
  owner_id uuid not null,
  name text not null,
  tagline text not null,
  description text not null default '',
  category text not null,
  categories text[] not null default '{}',
  thumbnail_variant text not null default 'new',
  thumbnail_url text,
  app_url text not null,
  github_url text,
  maker jsonb not null,
  tech_tags text[] not null default '{}',
  plays integer not null default 0,
  likes integer not null default 0,
  created_at timestamptz not null default timezone('utc', now()),
  deleted_at timestamptz,
  source text not null default 'submitted'
);

create table public.app_bookmarks (
  user_id uuid not null,
  app_id text not null references public.apps(id) on delete cascade,
  created_at timestamptz not null default timezone('utc', now()),
  primary key (user_id, app_id)
);

create table public.app_likes (
  user_id uuid not null,
  app_id text not null references public.apps(id) on delete cascade,
  created_at timestamptz not null default timezone('utc', now()),
  primary key (user_id, app_id)
);

create table public.app_comments (
  id uuid primary key default gen_random_uuid(),
  app_id text not null references public.apps(id) on delete cascade,
  user_id uuid not null,
  parent_id uuid references public.app_comments(id) on delete cascade,
  title text not null default '',
  content text not null,
  created_at timestamptz not null default timezone('utc', now())
);

create table public.site_visitors (
  visitor_id text not null,
  visited_on date not null,
  created_at timestamptz not null default timezone('utc', now()),
  primary key (visitor_id, visited_on)
);
