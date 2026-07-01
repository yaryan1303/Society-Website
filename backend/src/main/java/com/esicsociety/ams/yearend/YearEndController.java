package com.esicsociety.ams.yearend;

import com.esicsociety.ams.yearend.dto.YearEndDtos;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/financial-years/{yearId}")
@PreAuthorize("hasRole('ADMIN')")
public class YearEndController {

    private final YearEndService service;

    public YearEndController(YearEndService service) {
        this.service = service;
    }

    /** Closing balances for every member — shown before confirming the close. */
    @GetMapping("/year-end/preview")
    public List<YearEndDtos.MemberCarryForward> preview(@PathVariable Long yearId) {
        return service.preview(yearId);
    }

    @PostMapping("/close")
    public YearEndDtos.CloseYearResult close(@PathVariable Long yearId,
                                             @Valid @RequestBody(required = false) YearEndDtos.CloseYearRequest req) {
        return service.closeYear(yearId, req);
    }
}
