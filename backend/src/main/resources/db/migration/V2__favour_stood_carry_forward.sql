-- =============================================================================
-- Favour-stood now carries forward year to year (like the deposit ledgers).
-- The year-end close writes a cumulative "opening" row into the next year so the
-- prior years' amounts stay visible in the Favour Stood tab. Flag those rows.
-- =============================================================================
alter table favour_stood_entry
    add column opening boolean not null default false;
