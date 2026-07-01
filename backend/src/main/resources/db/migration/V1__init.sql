-- =============================================================================
-- ESIC Society AMS — initial schema (PostgreSQL)
-- Money columns are numeric(15,2). Audit/time columns are timestamp (no tz).
-- =============================================================================

create table member (
    id                        bigserial primary key,
    created_at                timestamp     not null,
    updated_at                timestamp     not null,
    account_no                varchar(32)   not null,
    name                      varchar(160)  not null,
    father_or_husband_name    varchar(160),
    email                     varchar(160),
    address                   varchar(255),
    role                      varchar(16)   not null,
    password_hash             varchar(100)  not null,
    must_change_password      boolean       not null,
    active                    boolean       not null,
    compulsory_deposit_amount numeric(15,2) not null,
    max_credit_limit          numeric(15,2),
    constraint uq_member_account_no unique (account_no)
);

create table financial_year (
    id         bigserial primary key,
    created_at timestamp   not null,
    updated_at timestamp   not null,
    label      varchar(9)  not null,
    start_date date        not null,
    end_date   date        not null,
    closed     boolean     not null,
    constraint uq_financial_year_label unique (label)
);

-- ---- Shares / deposits share the same Dr/Cr/Balance shape -------------------
create table share_txn (
    id            bigserial primary key,
    created_at    timestamp     not null,
    updated_at    timestamp     not null,
    member_id     bigint        not null references member(id) on delete cascade,
    year_id       bigint        not null references financial_year(id),
    txn_date      date          not null,
    dr            numeric(15,2) not null,
    cr            numeric(15,2) not null,
    balance_after numeric(15,2) not null,
    particulars   varchar(255),
    opening       boolean       not null
);
create index idx_share_txn_member_year on share_txn (member_id, year_id, id);

create table compulsory_deposit_txn (
    id            bigserial primary key,
    created_at    timestamp     not null,
    updated_at    timestamp     not null,
    member_id     bigint        not null references member(id) on delete cascade,
    year_id       bigint        not null references financial_year(id),
    txn_date      date          not null,
    dr            numeric(15,2) not null,
    cr            numeric(15,2) not null,
    balance_after numeric(15,2) not null,
    particulars   varchar(255),
    opening       boolean       not null
);
create index idx_cd_txn_member_year on compulsory_deposit_txn (member_id, year_id, id);

create table other_deposit_txn (
    id            bigserial primary key,
    created_at    timestamp     not null,
    updated_at    timestamp     not null,
    member_id     bigint        not null references member(id) on delete cascade,
    year_id       bigint        not null references financial_year(id),
    txn_date      date          not null,
    dr            numeric(15,2) not null,
    cr            numeric(15,2) not null,
    balance_after numeric(15,2) not null,
    particulars   varchar(255),
    opening       boolean       not null
);
create index idx_od_txn_member_year on other_deposit_txn (member_id, year_id, id);

-- ---- Loans ------------------------------------------------------------------
create table loan (
    id                     bigserial primary key,
    created_at             timestamp     not null,
    updated_at             timestamp     not null,
    member_id              bigint        not null references member(id) on delete cascade,
    bond_no                varchar(64),
    condition_of_repayment varchar(255),
    sureties               varchar(255),
    opening_date           date          not null,
    principal_amount       numeric(15,2) not null,
    principal_outstanding  numeric(15,2) not null,
    interest_outstanding   numeric(15,2) not null,
    closed                 boolean       not null
);
create index idx_loan_member on loan (member_id);

create table loan_txn (
    id                     bigserial primary key,
    created_at             timestamp     not null,
    updated_at             timestamp     not null,
    loan_id                bigint        not null references loan(id) on delete cascade,
    year_id                bigint        not null references financial_year(id),
    txn_date               date          not null,
    txn_type               varchar(20)   not null,
    cb_folio               varchar(64),
    loan_dr                numeric(15,2) not null,
    loan_cr                numeric(15,2) not null,
    loan_balance_after     numeric(15,2) not null,
    interest_charged       numeric(15,2) not null,
    interest_paid          numeric(15,2) not null,
    interest_balance_after numeric(15,2) not null,
    payment_mode           varchar(8),
    receipt_no             varchar(64),
    note                   varchar(255)
);
create index idx_loan_txn_loan on loan_txn (loan_id, id);
create index idx_loan_txn_year on loan_txn (year_id);

-- ---- Members in whose favour stood -----------------------------------------
create table favour_stood_entry (
    id         bigserial primary key,
    created_at timestamp     not null,
    updated_at timestamp     not null,
    member_id  bigint        not null references member(id) on delete cascade,
    year_id    bigint        not null references financial_year(id),
    entry_date date          not null,
    amount     numeric(15,2) not null,
    note       varchar(255)
);
create index idx_favour_member_year on favour_stood_entry (member_id, year_id);

-- ---- Password reset tokens --------------------------------------------------
create table password_reset_token (
    id         bigserial primary key,
    created_at timestamp    not null,
    updated_at timestamp    not null,
    member_id  bigint       not null references member(id) on delete cascade,
    token      varchar(128) not null,
    expires_at timestamp    not null,
    used       boolean      not null,
    constraint uq_reset_token unique (token)
);
create index idx_reset_token_member on password_reset_token (member_id);
