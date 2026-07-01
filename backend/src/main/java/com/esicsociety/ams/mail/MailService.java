package com.esicsociety.ams.mail;

import com.esicsociety.ams.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails. When {@code app.mail.enabled=true} and an SMTP
 * sender is configured, real email is sent; otherwise the message body is logged
 * to the server console so the password flows are demoable without SMTP.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final AppProperties props;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public MailService(AppProperties props, ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.props = props;
        this.mailSenderProvider = mailSenderProvider;
    }

    public void sendWelcome(String toEmail, String name, String accountNo, String tempPassword) {
        String loginUrl = props.getBaseUrl() + "/login";
        String subject = "Your ESIC Society account";
        String body = """
                Dear %s,

                An account has been created for you on the ESIC Employees Cooperative
                Credit & Thrift Society portal.

                  Account number : %s
                  Temporary password: %s

                Please sign in at %s and set a new password on first login.
                The temporary password will stop working once you change it.

                — ESIC Society
                """.formatted(name, accountNo, tempPassword, loginUrl);
        send(toEmail, subject, body);
    }

    public void sendPasswordResetLink(String toEmail, String name, String resetUrl) {
        String subject = "Reset your ESIC Society password";
        String body = """
                Dear %s,

                We received a request to reset your password. Use the link below
                (valid for %d minutes):

                  %s

                If you did not request this, you can ignore this email.

                — ESIC Society
                """.formatted(name, props.getResetTokenExpiryMinutes(), resetUrl);
        send(toEmail, subject, body);
    }

    private void send(String to, String subject, String body) {
        JavaMailSender sender = props.getMail().isEnabled() ? mailSenderProvider.getIfAvailable() : null;
        if (sender == null) {
            log.info("""
                    [MAIL DISABLED — logging instead of sending]
                    To: {}
                    Subject: {}
                    {}""", to, subject, body);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(props.getMail().getFrom());
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            sender.send(msg);
            log.info("Sent '{}' email to {}", subject, to);
        } catch (Exception ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage());
        }
    }
}
