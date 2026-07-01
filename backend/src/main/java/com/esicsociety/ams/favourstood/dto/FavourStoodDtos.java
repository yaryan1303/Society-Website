package com.esicsociety.ams.favourstood.dto;

import com.esicsociety.ams.favourstood.FavourStoodEntry;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class FavourStoodDtos {

    private FavourStoodDtos() {}

    public record EntryRequest(
            @NotNull LocalDate entryDate,
            @NotNull @Positive BigDecimal amount,
            @Size(max = 255) String note) {}

    public record Response(
            Long id,
            Long memberId,
            Long yearId,
            LocalDate entryDate,
            BigDecimal amount,
            String note,
            boolean opening) {

        public static Response of(FavourStoodEntry e) {
            return new Response(e.getId(), e.getMember().getId(), e.getFinancialYear().getId(),
                    e.getEntryDate(), e.getAmount(), e.getNote(), e.isOpening());
        }
    }
}
