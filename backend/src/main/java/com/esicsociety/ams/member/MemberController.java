package com.esicsociety.ams.member;

import com.esicsociety.ams.member.dto.MemberDtos;
import com.esicsociety.ams.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/members")
public class MemberController {

    private final MemberService service;

    public MemberController(MemberService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<MemberDtos.Response> list() {
        return service.listMembers().stream().map(MemberDtos.Response::of).toList();
    }

    /** A member may fetch their own profile; admin may fetch anyone's. */
    @GetMapping("/{id}")
    public MemberDtos.Response get(@PathVariable Long id) {
        CurrentUser.requireOwnerOrAdmin(id);
        return MemberDtos.Response.of(service.getMember(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberDtos.CreatedResponse create(@Valid @RequestBody MemberDtos.CreateRequest req) {
        return service.createMember(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public MemberDtos.Response update(@PathVariable Long id, @Valid @RequestBody MemberDtos.UpdateRequest req) {
        return MemberDtos.Response.of(service.updateMember(id, req));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> resetPassword(@PathVariable Long id) {
        return Map.of("temporaryPassword", service.resetToTempPassword(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.deleteMember(id);
    }
}
