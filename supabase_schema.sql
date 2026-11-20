-- Smart Risk Analyzer — Supabase PostgreSQL Schema & API Grants
-- Project ID: wrqpnupqlucbgyiapugx
-- Run this SQL once in Supabase Dashboard -> SQL Editor

create table if not exists public.app_users (
    id text primary key,
    email text not null unique,
    display_name text not null,
    password_hash text not null,
    password_salt text not null,
    role text not null default 'USER',
    must_change_password boolean not null default false,
    created_at_millis bigint not null
);

create table if not exists public.health_profiles (
    user_id text primary key references public.app_users(id) on delete cascade,
    display_name text,
    allergies_json text not null default '[]',
    medical_conditions_json text not null default '[]',
    dietary_preferences_json text not null default '[]',
    chemical_sensitivities_json text not null default '[]',
    created_at_millis bigint not null,
    updated_at_millis bigint not null
);

create table if not exists public.scans (
    id text primary key,
    user_id text not null references public.app_users(id) on delete cascade,
    product_name text not null,
    brand text not null default '',
    category text not null,
    score bigint not null,
    rating_label text not null,
    summary text not null default '',
    standards_cited_json text not null default '[]',
    personal_alerts_json text not null default '[]',
    negatives_json text not null default '[]',
    positives_json text not null default '[]',
    ingredients_json text not null default '[]',
    healthier_alternative_json text,
    raw_input_text text not null default '',
    recipes_json text,
    created_at_millis bigint not null,
    updated_at_millis bigint not null
);

create table if not exists public.top_rated_products (
    id text primary key,
    user_id text not null,
    product_name text not null,
    brand text not null default '',
    category text not null,
    score bigint not null,
    rating_label text not null,
    highlight text not null default '',
    created_at_millis bigint not null
);

create index if not exists idx_scans_user_created on public.scans (user_id, created_at_millis desc);
create index if not exists idx_top_rated_score on public.top_rated_products (score desc);

-- Disable RLS for app-managed email/password auth prototype access
alter table public.app_users disable row level security;
alter table public.health_profiles disable row level security;
alter table public.scans disable row level security;
alter table public.top_rated_products disable row level security;

-- CRITICAL: Grant schema & table access to Supabase Data API roles (prevents PGRST205 404 errors)
grant usage on schema public to anon, authenticated;
grant select, insert, update, delete on table public.app_users to anon, authenticated;
grant select, insert, update, delete on table public.health_profiles to anon, authenticated;
grant select, insert, update, delete on table public.scans to anon, authenticated;
grant select, insert, update, delete on table public.top_rated_products to anon, authenticated;

-- Force PostgREST Data API to reload its schema cache immediately
notify pgrst, 'reload schema';
