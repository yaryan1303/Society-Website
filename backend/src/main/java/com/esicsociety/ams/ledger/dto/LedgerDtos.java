package com.esicsociety.ams.ledger.dto;

import com.esicsociety.ams.ledger.AbstractLedgerTxn;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Request/response DTOs for the Dr/Cr/Balance ledgers (shares + deposits). */
public final class LedgerDtos {

    private LedgerDtos() {}

    /**
     * One ledger entry to record. For compulsory deposits, {@code cr} may be left
     * null to default to the member's fixed monthly amount (handled in the controller).
     */
    public record EntryRequest(
            @NotNull LocalDate txnDate,
            @DecimalMin(value = "0.00", message = "Dr cannot be negative") BigDecimal dr,
            @DecimalMin(value = "0.00", message = "Cr cannot be negative") BigDecimal cr,
            @Size(max = 255) String particulars) {}

    public record EntryResponse(
            Long id,
            LocalDate txnDate,
            BigDecimal dr,
            BigDecimal cr,
            BigDecimal balanceAfter,
            String particulars,
            boolean opening) {

        public static EntryResponse of(AbstractLedgerTxn t) {
            return new EntryResponse(t.getId(), t.getTxnDate(), t.getDr(), t.getCr(),
                    t.getBalanceAfter(), t.getParticulars(), t.isOpening());
        }
    }

    public record LedgerView(
            String section,
            Long memberId,
            Long yearId,
            BigDecimal openingBalance,
            BigDecimal closingBalance,
            List<EntryResponse> entries) {}
}
