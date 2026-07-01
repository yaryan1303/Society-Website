package com.esicsociety.ams.ledger;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Shares ledger: Cr = purchase, Dr = reduction, running share balance. */
@Entity
@Table(name = "share_txn")
public class ShareTxn extends AbstractLedgerTxn {
}
