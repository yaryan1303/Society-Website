package com.esicsociety.ams.favourstood;

import com.esicsociety.ams.favourstood.dto.FavourStoodDtos;
import com.esicsociety.ams.financialyear.FinancialYear;
import com.esicsociety.ams.financialyear.FinancialYearService;
import com.esicsociety.ams.member.Member;
import com.esicsociety.ams.member.MemberService;
import com.esicsociety.ams.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/members/{memberId}/favour-stood")
public class FavourStoodController {

    private final FavourStoodService service;
    private final MemberService memberService;
    private final FinancialYearService yearService;

    public FavourStoodController(FavourStoodService service, MemberService memberService,
                                 FinancialYearService yearService) {
        this.service = service;
        this.memberService = memberService;
        this.yearService = yearService;
    }

    @GetMapping
    public List<FavourStoodDtos.Response> list(@PathVariable Long memberId,
                                               @RequestParam(required = false) Long yearId) {
        CurrentUser.requireOwnerOrAdmin(memberId);
        FinancialYear year = resolveYear(yearId);
        return service.list(memberId, year.getId()).stream().map(FavourStoodDtos.Response::of).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public FavourStoodDtos.Response add(@PathVariable Long memberId,
                                        @RequestParam(required = false) Long yearId,
                                        @Valid @RequestBody FavourStoodDtos.EntryRequest req) {
        Member member = memberService.getMember(memberId);
        FinancialYear year = resolveYear(yearId);
        return FavourStoodDtos.Response.of(
                service.addManual(member, year, req.entryDate(), req.amount(), req.note()));
    }

    @DeleteMapping("/{entryId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long memberId, @PathVariable Long entryId) {
        service.delete(entryId);
    }

    private FinancialYear resolveYear(Long yearId) {
        return yearId != null ? yearService.getById(yearId) : yearService.currentYear();
    }
}
