package com.esicsociety.ams.auth;

import com.esicsociety.ams.auth.dto.AuthDtos;
import com.esicsociety.ams.common.exception.ApiExceptions.BadRequestException;
import com.esicsociety.ams.config.AppProperties;
import com.esicsociety.ams.mail.MailService;
import com.esicsociety.ams.member.Member;
import com.esicsociety.ams.member.MemberRepository;
import com.esicsociety.ams.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final MemberRepository memberRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;
    private final AppProperties props;
    private final SecureRandom random = new SecureRandom();

    public AuthService(AuthenticationManager authenticationManager, MemberRepository memberRepository,
                       PasswordResetTokenRepository tokenRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, MailService mailService, AppProperties props) {
        this.authenticationManager = authenticationManager;
        this.memberRepository = memberRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailService = mailService;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest req) {
        // Throws BadCredentialsException (-> 401) on bad password / unknown account / disabled.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.accountNo().trim(), req.password()));

        Member member = memberRepository.findByAccountNo(req.accountNo().trim()).orElseThrow();
        String token = jwtService.generateToken(member);
        return new AuthDtos.LoginResponse(token, "Bearer", member.getId(), member.getAccountNo(),
                member.getName(), member.getRole(), member.isMustChangePassword(),
                props.getJwt().getExpiryMinutes());
    }

    /** First-login change and voluntary change. Requires the caller to be authenticated. */
    @Transactional
    public void changePassword(Long memberId, AuthDtos.ChangePasswordRequest req) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        if (!passwordEncoder.matches(req.currentPassword(), member.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(req.newPassword(), member.getPasswordHash())) {
            throw new BadRequestException("New password must be different from the current one");
        }
        member.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        member.setMustChangePassword(false);
        memberRepository.save(member);
    }

    /** Always succeeds from the caller's perspective (no account-existence leak). */
    @Transactional
    public void forgotPassword(AuthDtos.ForgotPasswordRequest req) {
        Optional<Member> maybe = memberRepository.findByAccountNo(req.accountNo().trim());
        if (maybe.isEmpty()) {
            log.info("Forgot-password requested for unknown account {} (ignored)", req.accountNo());
            return;
        }
        Member member = maybe.get();
        if (member.getEmail() == null || member.getEmail().isBlank()) {
            log.warn("Member {} has no email on file; cannot send reset link", member.getAccountNo());
            return;
        }
        tokenRepository.invalidateAllForMember(member.getId());

        PasswordResetToken prt = new PasswordResetToken();
        prt.setMember(member);
        prt.setToken(newRawToken());
        prt.setExpiresAt(LocalDateTime.now().plusMinutes(props.getResetTokenExpiryMinutes()));
        prt.setUsed(false);
        tokenRepository.save(prt);

        String resetUrl = props.getBaseUrl() + "/reset?token=" + prt.getToken();
        mailService.sendPasswordResetLink(member.getEmail(), member.getName(), resetUrl);
    }

    @Transactional
    public void resetPassword(AuthDtos.ResetPasswordRequest req) {
        PasswordResetToken prt = tokenRepository.findByToken(req.token())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset link"));
        if (!prt.isValid()) {
            throw new BadRequestException("Invalid or expired reset link");
        }
        Member member = prt.getMember();
        member.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        member.setMustChangePassword(false);
        memberRepository.save(member);

        prt.setUsed(true);
        tokenRepository.save(prt);
    }

    private String newRawToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
