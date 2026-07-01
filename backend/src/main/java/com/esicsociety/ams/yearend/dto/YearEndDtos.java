package com.esicsociety.ams.yearend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public final class YearEndDtos {

    private YearEndDtos() {}

    /** Per-member amount to move from compulsory deposit into "favour stood". */
    public record FavourStoodSplit(
            @NotNull Long memberId,
            @NotNull @PositiveOrZero BigDecimal amount) {}

    public record CloseYearRequest(
            @Valid List<FavourStoodSplit> splits) {}

    public record CloseYearResult(
            String closedYear,
            String nextYear,
            int membersProcessed,
            BigDecimal totalFavourStoodMoved) {}

    /** Preview shown to the admin before closing: each member's carry-forward figures. */
    public record MemberCarryForward(
            Long memberId,
            String accountNo,
            String name,
            BigDecimal sharesClosing,
            BigDecimal compulsoryDepositClosing,
            BigDecimal compulsoryDepositDeposits,
            BigDecimal otherDepositClosing,
            BigDecimal loanOutstanding,
            BigDecimal interestOutstanding) {}
}
