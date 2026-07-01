package com.esicsociety.ams.common;

/**
 * Classifies a row in the loan ledger so the UI/export can render it like the
 * paper book.
 * <ul>
 *   <li>OPENING        — carried-forward balance at the start of a year (dr=cr=0)</li>
 *   <li>DISBURSAL      — money lent out (loan Dr)</li>
 *   <li>INTEREST_CHARGE— monthly interest charged on the outstanding balance</li>
 *   <li>REPAYMENT      — a payment split into principal (loan Cr) + interest paid</li>
 * </ul>
 */
public enum LoanTxnType {
    OPENING,
    DISBURSAL,
    INTEREST_CHARGE,
    REPAYMENT
}
