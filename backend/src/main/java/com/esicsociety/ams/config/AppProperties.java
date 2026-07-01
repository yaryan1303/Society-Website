package com.esicsociety.ams.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Strongly-typed application configuration bound from the {@code app.*} keys in
 * application.yml (which in turn read environment variables). No secrets are
 * hard-coded; sensible dev defaults are provided.
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Admin admin = new Admin();
    private Mail mail = new Mail();
    private String baseUrl = "http://localhost:5173";
    private String corsAllowedOrigins = "http://localhost:5173";
    private BigDecimal compulsoryDepositDefault = new BigDecimal("1500");
    private BigDecimal loanAnnualRatePct = new BigDecimal("8");
    private int resetTokenExpiryMinutes = 60;
    private boolean seedDemo = true;

    public static class Jwt {
        private String secret;
        private long expiryMinutes = 240;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getExpiryMinutes() { return expiryMinutes; }
        public void setExpiryMinutes(long expiryMinutes) { this.expiryMinutes = expiryMinutes; }
    }

    public static class Admin {
        private String accountNo = "ADMIN001";
        private String password = "Admin@12345";
        private String email = "admin@esicsociety.local";

        public String getAccountNo() { return accountNo; }
        public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class Mail {
        private boolean enabled = false;
        private String from = "no-reply@esicsociety.local";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
    }

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }
    public Mail getMail() { return mail; }
    public void setMail(Mail mail) { this.mail = mail; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getCorsAllowedOrigins() { return corsAllowedOrigins; }
    public void setCorsAllowedOrigins(String corsAllowedOrigins) { this.corsAllowedOrigins = corsAllowedOrigins; }
    public BigDecimal getCompulsoryDepositDefault() { return compulsoryDepositDefault; }
    public void setCompulsoryDepositDefault(BigDecimal compulsoryDepositDefault) { this.compulsoryDepositDefault = compulsoryDepositDefault; }
    public BigDecimal getLoanAnnualRatePct() { return loanAnnualRatePct; }
    public void setLoanAnnualRatePct(BigDecimal loanAnnualRatePct) { this.loanAnnualRatePct = loanAnnualRatePct; }
    public int getResetTokenExpiryMinutes() { return resetTokenExpiryMinutes; }
    public void setResetTokenExpiryMinutes(int resetTokenExpiryMinutes) { this.resetTokenExpiryMinutes = resetTokenExpiryMinutes; }
    public boolean isSeedDemo() { return seedDemo; }
    public void setSeedDemo(boolean seedDemo) { this.seedDemo = seedDemo; }
}
