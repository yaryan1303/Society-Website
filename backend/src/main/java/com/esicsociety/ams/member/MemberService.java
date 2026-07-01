package com.esicsociety.ams.member;

import com.esicsociety.ams.common.Role;
import com.esicsociety.ams.common.exception.ApiExceptions.BadRequestException;
import com.esicsociety.ams.common.exception.ApiExceptions.ConflictException;
import com.esicsociety.ams.common.exception.ApiExceptions.NotFoundException;
import com.esicsociety.ams.config.AppProperties;
import com.esicsociety.ams.mail.MailService;
import com.esicsociety.ams.member.dto.MemberDtos;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
public class MemberService {

    /** Unambiguous alphabet for temp passwords (no O/0, I/l/1). */
    private static final char[] PWD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#%".toCharArray();
    private static final int TEMP_PWD_LEN = 10;

    private final MemberRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final AppProperties props;
    private final SecureRandom random = new SecureRandom();

    public MemberService(MemberRepository repository, PasswordEncoder passwordEncoder,
                         MailService mailService, AppProperties props) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public List<Member> listMembers() {
        return repository.findByRoleOrderByNameAsc(Role.MEMBER);
    }

    @Transactional(readOnly = true)
    public Member getMember(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Member not found: " + id));
    }

    @Transactional
    public MemberDtos.CreatedResponse createMember(MemberDtos.CreateRequest req) {
        if (repository.existsByAccountNo(req.accountNo())) {
            throw new ConflictException("Account number already exists: " + req.accountNo());
        }
        String tempPassword = generateTempPassword();

        Member m = new Member();
        m.setAccountNo(req.accountNo().trim());
        m.setName(req.name().trim());
        m.setFatherOrHusbandName(trimOrNull(req.fatherOrHusbandName()));
        m.setEmail(req.email().trim());
        m.setAddress(trimOrNull(req.address()));
        m.setRole(Role.MEMBER);
        m.setPasswordHash(passwordEncoder.encode(tempPassword));
        m.setMustChangePassword(true);
        m.setActive(true);
        m.setCompulsoryDepositAmount(
                req.compulsoryDepositAmount() != null ? req.compulsoryDepositAmount()
                        : props.getCompulsoryDepositDefault());
        m.setMaxCreditLimit(req.maxCreditLimit());

        Member saved = repository.save(m);
        mailService.sendWelcome(saved.getEmail(), saved.getName(), saved.getAccountNo(), tempPassword);
        return new MemberDtos.CreatedResponse(MemberDtos.Response.of(saved), tempPassword);
    }

    @Transactional
    public Member updateMember(Long id, MemberDtos.UpdateRequest req) {
        Member m = getMember(id);
        if (m.isAdmin()) {
            throw new BadRequestException("The admin account cannot be edited here");
        }
        m.setName(req.name().trim());
        m.setFatherOrHusbandName(trimOrNull(req.fatherOrHusbandName()));
        m.setEmail(req.email().trim());
        m.setAddress(trimOrNull(req.address()));
        if (req.compulsoryDepositAmount() != null) {
            m.setCompulsoryDepositAmount(req.compulsoryDepositAmount());
        }
        m.setMaxCreditLimit(req.maxCreditLimit());
        if (req.active() != null) {
            m.setActive(req.active());
        }
        return repository.save(m);
    }

    /** Admin resets a member's password to a fresh temp password (forces change). */
    @Transactional
    public String resetToTempPassword(Long id) {
        Member m = getMember(id);
        if (m.isAdmin()) {
            throw new BadRequestException("Use the change-password flow for the admin account");
        }
        String tempPassword = generateTempPassword();
        m.setPasswordHash(passwordEncoder.encode(tempPassword));
        m.setMustChangePassword(true);
        repository.save(m);
        mailService.sendWelcome(m.getEmail(), m.getName(), m.getAccountNo(), tempPassword);
        return tempPassword;
    }

    @Transactional
    public void deleteMember(Long id) {
        Member m = getMember(id);
        if (m.isAdmin()) {
            throw new BadRequestException("The admin account cannot be deleted");
        }
        // FKs are ON DELETE CASCADE, so the member's ledgers/loans are removed too.
        repository.delete(m);
    }

    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder(TEMP_PWD_LEN);
        for (int i = 0; i < TEMP_PWD_LEN; i++) {
            sb.append(PWD_ALPHABET[random.nextInt(PWD_ALPHABET.length)]);
        }
        return sb.toString();
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
