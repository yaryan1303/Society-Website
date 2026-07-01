-- =============================================================================
-- Repayments now capture only the total amount paid; the system splits it into
-- interest (pending interest first) and principal. The split is re-derived
-- authoritatively during replay, so the entered total must be stored.
-- Back-fill existing repayment rows from their recorded principal + interest.
-- =============================================================================
alter table loan_txn
    add column payment_amount numeric(15,2);

update loan_txn
    set payment_amount = loan_cr + interest_paid
    where txn_type = 'REPAYMENT';
