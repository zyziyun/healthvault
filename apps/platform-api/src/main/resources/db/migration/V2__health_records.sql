-- Domain model for the health records. Additive, forward-only migration.
create table documents (
    id          bigserial primary key,
    user_id     bigint      not null references users(id),
    source_type varchar(32) not null,          -- lab_report, prescription, ...
    storage_key varchar(512) not null,         -- object key in S3
    status      varchar(16) not null default 'queued',
    uploaded_at timestamptz not null default now()
);
create index idx_documents_user on documents(user_id);

create table reports (
    id          bigserial primary key,
    document_id bigint not null references documents(id),
    report_type varchar(64),
    report_date date,
    summary     text
);
create index idx_reports_document on reports(document_id);

-- catalog of indicator types, shared across all users
create table indicators (
    id             bigserial primary key,
    name           varchar(128) not null unique,
    unit           varchar(32),
    reference_low  numeric,
    reference_high numeric
);

create table readings (
    id           bigserial primary key,
    report_id    bigint  not null references reports(id),
    indicator_id bigint  not null references indicators(id),
    value        numeric not null,
    status       varchar(16),                  -- normal, high, low
    measured_at  timestamptz not null
);
create index idx_readings_report on readings(report_id);
-- composite index for the trend query: one indicator over time
create index idx_readings_indicator_time on readings(indicator_id, measured_at);
