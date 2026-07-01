package com.esicsociety.ams.ledger;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Compulsory deposit ledger: fixed monthly deposit (Cr), withdrawals (Dr). */
@Entity
@Table(name = "compulsory_deposit_txn")
public class CompulsoryDepositTxn extends AbstractLedgerTxn {
}
