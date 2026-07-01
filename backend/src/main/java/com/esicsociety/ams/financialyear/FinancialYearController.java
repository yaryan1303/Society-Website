package com.esicsociety.ams.financialyear;

import com.esicsociety.ams.financialyear.dto.FinancialYearDto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/financial-years")
public class FinancialYearController {

    private final FinancialYearService service;

    public FinancialYearController(FinancialYearService service) {
        this.service = service;
    }

    /** Years for the dropdown (newest first). Any authenticated user. */
    @GetMapping
    public List<FinancialYearDto> list() {
        return service.listAll().stream().map(FinancialYearDto::of).toList();
    }

    @GetMapping("/current")
    public FinancialYearDto current() {
        return FinancialYearDto.of(service.currentYear());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public FinancialYearDto create(@Valid @RequestBody CreateYearRequest req) {
        return FinancialYearDto.of(service.create(req.startYear()));
    }

    public record CreateYearRequest(@Min(2000) @Max(2100) int startYear) {}
}
