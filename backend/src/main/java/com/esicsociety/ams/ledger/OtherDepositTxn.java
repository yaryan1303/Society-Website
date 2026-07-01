package com.esicsociety.ams.ledger;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Other (flexible) deposit ledger with full Dr / Cr / Balance. */
@Entity
@Table(name = "other_deposit_txn")
public class OtherDepositTxn extends AbstractLedgerTxn {
}
