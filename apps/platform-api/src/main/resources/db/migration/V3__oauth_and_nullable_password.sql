-- OAuth users sign in via a provider and have no local password.
alter table users alter column password_hash drop not null;

-- One user can link several external identities (google, github, ...).
create table oauth_accounts (
    id               bigserial primary key,
    user_id          bigint      not null references users(id),
    provider         varchar(32) not null,
    provider_user_id varchar(255) not null,
    created_at       timestamptz not null default now(),
    unique (provider, provider_user_id)
);
create index idx_oauth_accounts_user on oauth_accounts(user_id);
