package com.esicsociety.ams.export;

import com.esicsociety.ams.financialyear.FinancialYear;
import com.esicsociety.ams.financialyear.FinancialYearService;
import com.esicsociety.ams.member.Member;
import com.esicsociety.ams.member.MemberService;
import com.esicsociety.ams.security.CurrentUser;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ExportController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExcelExportService exportService;
    private final MemberService memberService;
    private final FinancialYearService yearService;

    public ExportController(ExcelExportService exportService, MemberService memberService,
                            FinancialYearService yearService) {
        this.exportService = exportService;
        this.memberService = memberService;
        this.yearService = yearService;
    }

    /** A member may export their own ledger; admin may export anyone's. */
    @GetMapping("/members/{memberId}/export")
    public ResponseEntity<byte[]> exportMember(@PathVariable Long memberId,
                                               @RequestParam(required = false) Long yearId) {
        CurrentUser.requireOwnerOrAdmin(memberId);
        Member member = memberService.getMember(memberId);
        FinancialYear year = resolveYear(yearId);
        byte[] body = exportService.exportMember(member, year);
        String filename = "ledger_" + member.getAccountNo() + "_" + year.getLabel() + ".xlsx";
        return file(body, filename);
    }

    /** Admin-only: every member's ledger for a year, one sheet each. */
    @GetMapping("/export/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportAll(@RequestParam(required = false) Long yearId) {
        FinancialYear year = resolveYear(yearId);
        List<Member> members = memberService.listMembers();
        byte[] body = exportService.exportAll(members, year);
        String filename = "all_members_" + year.getLabel() + ".xlsx";
        return file(body, filename);
    }

    private ResponseEntity<byte[]> file(byte[] body, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(XLSX);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(body.length);
        return new ResponseEntity<>(body, headers, 200);
    }

    private FinancialYear resolveYear(Long yearId) {
        return yearId != null ? yearService.getById(yearId) : yearService.currentYear();
    }
}
