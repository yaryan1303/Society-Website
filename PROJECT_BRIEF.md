# Cooperative Society – Account Management System

> Build brief for Claude Code. Read this file fully before writing any code.
> A detailed functional specification is in `AMS_Specification.docx` (same folder) and
> reference ledger images are in `/reference-images`. Consult both.

---

## 1. What we are building

A web-based **Account Management System** for a cooperative credit & thrift society that
digitizes a hand-written member ledger. It replaces a paper "Personal Ledger" book.

**Stack (required):**
- Frontend: **React** (Vite + React Router, plain CSS or Tailwind, your choice — keep it clean and simple)
- Backend: **Spring Boot** (Java 17+, Spring Web, Spring Security, Spring Data JPA)
- Database: **PostgreSQL** (required — use it for dev, test, and production)
- Auth: **JWT** (stateless), BCrypt password hashing
- Build: Maven for backend, npm for frontend
- API: REST, JSON

Organize as a monorepo:
```
/backend    (Spring Boot)
/frontend   (React)
            └── public/members/        (committee member photos, provided by owner)
            └── src/data/members.json   (carousel data: name, designation, photo)
/reference-images
AMS_Specification.docx
PROJECT_BRIEF.md
README.md   (you generate this)
```

---

## 2. Two roles, two dashboards

| Role | Capabilities |
|------|--------------|
| **ADMIN** | Create/edit/delete member accounts; record every transaction (shares, deposits, loans, repayments); edit/correct entries; run year-end close; export to Excel; view any member. |
| **MEMBER (USER)** | Log in to a personal dashboard; **view their own account only** (read-only); export own data. A member can NEVER see another member's data — enforce this at the API layer, not just the UI. |

---

## 3. Authentication & accounts

1. **No public signup.** Only ADMIN creates member accounts.
2. On creation, system generates a **temporary password**; admin shares account number + temp password with the member.
3. **First login forces a password change** before any data is shown. Temp password stops working after change.
4. **Login = Account Number + Password** (account number is the ledger account no., e.g. 1583).
5. **Forgot password:** email-based reset link (token with expiry). Same email channel used for first-login flow.
6. Passwords stored **BCrypt-hashed**; admin can reset but never view a password.

Implement: `POST /auth/login`, `POST /auth/forgot-password`, `POST /auth/reset-password`,
`POST /auth/change-password` (first-login + voluntary). JWT returned on login carries role.

---

## 4. Yearly ledgers (CRITICAL business rule)

- The society runs on the **Indian financial year: 1 April → 31 March**.
- Keep a **separate ledger per member per financial year**.
- **Year-end close (run by admin at 31 March):** lock the year, carry every closing balance
  (shares, compulsory deposit, other deposit, loan outstanding, interest balance) forward as
  the **opening balance of the new year**.
- Users/admin can switch the viewed year via a dropdown (e.g. 2025-26, 2026-27).

---

## 5. Ledger sections to implement

Each section mirrors a column group in the paper ledger. All amounts use Dr / Cr / running Balance
where noted. **All balances are computed by the backend, never trusted from the client.**

### 5.1 Shares
- Members can **buy shares any month, any time**. Each purchase = a Cr; balance runs up.
- Admin can record a Dr (reduction) if needed.
- Fields: date, dr, cr, balance(auto).

### 5.2 Compulsory Deposit
- Fixed monthly deposit, **April through March**.
- Running balance maintained.
- **Year-end deposit split (admin-decided amount — NOT fixed):** at 31 March the admin enters an
  amount to move into "Members in whose favour stood"; the remainder stays as the carried-forward
  compulsory-deposit balance.
  - Example: total 18,000; admin moves 5,000 → favour-stood; 13,000 stays as next-year opening balance.

### 5.3 Other Deposit
- Flexible deposit with full Dr / Cr / Balance, admin-managed.

### 5.4 Loans & Interest (most detailed)
Fields per loan record: date, C.B. folio, bond no., condition of repayment, name of sureties,
loan Dr/Cr/Balance, interest Dr/Cr/Balance, payment mode, receipt no.

- **Interest = simple interest, 8% per year on the CURRENT OUTSTANDING balance (reducing balance).**
- **Monthly interest formula:** `monthly_interest = outstanding_balance * 0.08 / 12`
  - Verified against the sample ledger: 183000→1220, 173000→1153, 163000→1087, 153000→1020. Your code must reproduce these exactly (round to nearest rupee).
- A member may pay interest **every month or skip months** — keep a running interest balance so unpaid interest accumulates and is always visible. Dashboard shows "interest due this month".
- **A repayment splits into principal + interest** (admin enters the split). Reduce loan balance by the principal part, reduce interest balance by the interest part, then recompute next month's interest on the new lower balance.
- **Payment mode = CASH or BANK**, tagged on every repayment. **Receipt No. is required only when mode = CASH.** Validate this on the backend.

### 5.5 Members in whose favour stood
- Records only **amount + date**, only when an amount is credited.
- This is where the year-end deposit-split amount (5.2) lands. Admin-managed; members can view their own.

---

## 6. Frontend branding & public landing page

**Society identity (use everywhere — page title, login header, footer):**
- Full name: **ESIC Employees Cooperative Credit & Thrift Society Ltd.**
- Hindi line (optional, show under the English name): ईएसआईसी सहकारी समिति, मुख्यालय
- Tagline: *"Together for a Stronger Society, Better Services and a Brighter Future."*
- Motto strip: Transparency • Accountability • Trust • Progress
- Use a clean blue + green palette (navy/blue primary, green accent) to match the society's existing poster identity. Modern, professional, mobile-responsive.

**Build an interactive, user-friendly public landing page** (this is the page visitors and members see before logging in; the login form lives here or one click away):
- Header with society name + logo placeholder.
- A short "Our Commitment to Members" / "Why choose us" section (transparency, monthly statements, regular reports, quick grievance resolution, digital records) — pull supporting copy from `AMS_Specification.docx`.
- **A moving (auto-scrolling) carousel of society members** — see 6.1.
- Clear call-to-action to the login page.

### 6.1 Moving members carousel (on the public landing page)

A horizontally auto-scrolling / sliding strip of member cards that moves on its own
(continuous marquee or auto-advancing carousel), pauses on hover, and is swipeable on mobile.
Each card shows the member's **photo, name, and designation**.

- Photos will be **provided as image files** by the owner — place them in
  `frontend/public/members/` and reference by filename. Build the component to read from a
  simple data array/JSON (`frontend/src/data/members.json`) so cards are easy to add/edit later.
- Until the real photos are dropped in, fall back to a neutral placeholder avatar so the
  layout works during development.
- Group/label by their role on the committee (President, Secretary, Vice President, Accountant,
  Treasurer, Executive Members).

Seed `members.json` with these committee members (designations from the society's poster):
```json
[
  { "name": "Virender Dahiya",     "designation": "President",        "photo": "/members/virender-dahiya.jpg" },
  { "name": "Yogesh Kumar Yadav",  "designation": "Secretary",        "photo": "/members/yogesh-kumar-yadav.jpg" },
  { "name": "Sona Kumar Singh",    "designation": "Vice President",   "photo": "/members/sona-kumar-singh.jpg" },
  { "name": "Umesh Chaurasia",     "designation": "Accountant",       "photo": "/members/umesh-chaurasia.jpg" },
  { "name": "Kamlesh Kumar",       "designation": "Treasurer",        "photo": "/members/kamlesh-kumar.jpg" },
  { "name": "Aaditya Paliwal",     "designation": "Executive Member", "photo": "/members/aaditya-paliwal.jpg" },
  { "name": "Ashish Kumar",        "designation": "Executive Member", "photo": "/members/ashish-kumar.jpg" },
  { "name": "Shrikant Gupta",      "designation": "Executive Member", "photo": "/members/shrikant-gupta.jpg" },
  { "name": "Shivam Kumar",        "designation": "Executive Member", "photo": "/members/shivam-kumar.jpg" },
  { "name": "Vivek Kumar Pandey",  "designation": "Executive Member", "photo": "/members/vivek-kumar-pandey.jpg" }
]
```
Implement the carousel with CSS animation or a small library (e.g. embla-carousel / swiper) —
keep it lightweight, accessible (pausable, keyboard-reachable), and smooth.

## 7. Excel export
- Export any single member's yearly ledger, or all members for a chosen year, to **.xlsx**
  (use Apache POI on the backend). Keep the same column layout as the ledger so it's print/audit-ready.
- Members may export their own data only.

---

## 8. Database (PostgreSQL — required)

- Use PostgreSQL for development, testing, and production. **Do not use H2 or any in-memory DB.**
- Connect via Spring Data JPA with the `org.postgresql:postgresql` driver.
- Use **Flyway** (or Liquibase) for schema migrations — version the schema, don't rely on `ddl-auto=update` in production. `ddl-auto=validate` against Flyway-managed schema is fine.
- For integration tests, use **Testcontainers** (`org.testcontainers:postgresql`) so tests run against a real PostgreSQL container, not a different engine. Falls back gracefully if Docker is unavailable — note this in the README.
- Provide a `docker-compose.yml` at the repo root that starts PostgreSQL so the client can run the app with one command.

Suggested local config (`application.yml`), override via environment variables:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/society_db
    username: society
    password: society
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
```
Use environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) so credentials are never hard-coded. Money columns use `NUMERIC`/`BigDecimal` — never floating point.

## 9. Suggested data model (adjust as needed)
- `Member` (id, accountNo unique, name, fatherOrHusbandName, email, role, passwordHash, mustChangePassword, createdAt)
- `FinancialYear` (id, label e.g. "2025-26", startDate, endDate, closed boolean)
- `ShareTxn` (id, memberId, yearId, date, dr, cr, balanceAfter)
- `CompulsoryDepositTxn` (id, memberId, yearId, date, dr, cr, balanceAfter)
- `OtherDepositTxn` (id, memberId, yearId, date, dr, cr, balanceAfter)
- `Loan` (id, memberId, bondNo, conditionOfRepayment, sureties, openingDate, principalOutstanding, interestOutstanding)
- `LoanTxn` (id, loanId, yearId, date, cbFolio, loanDr, loanCr, loanBalanceAfter, interestCharged, interestPaid, interestBalanceAfter, paymentMode[CASH|BANK], receiptNo nullable)
- `FavourStoodEntry` (id, memberId, yearId, date, amount)
- `PasswordResetToken` (id, memberId, token, expiresAt)

Use BigDecimal for all money. Never use float/double for currency.

---

## 10. Security requirements (do not skip)
- Every member-data endpoint must verify the JWT subject owns the resource OR the caller is ADMIN. A member requesting another member's data must get 403.
- Role-based: write/admin endpoints require ROLE_ADMIN.
- Validate CASH→receiptNo rule, year-not-closed before edits, and that balances recompute server-side.

---

## 11. What to build, in order
1. Backend skeleton: entities, repositories, DTOs, JWT security, seed one ADMIN account.
2. Auth flow end-to-end (login, first-login change, forgot/reset).
3. Member CRUD (admin).
4. Shares + both deposit modules with auto-balance.
5. Loan module with interest engine + repayment split + cash/bank/receipt validation.
6. Favour-stood + year-end close + carry-forward.
7. Excel export.
8. React frontend: **public landing page (society branding + moving members carousel, see Section 6)**, login, admin dashboard (member list, entry forms, year dropdown, export), member dashboard (read-only views + own export).
9. README with run instructions, and a seeded demo (admin + the sample member "Yogesh Kr. Yadav", account 1583) so the client can click through.

Write tests for the interest engine and the year-end carry-forward — those are the highest-risk logic.

---

## 12. References in this repo
- `AMS_Specification.docx` — full functional spec written for the client; read it for the "why" and exact wording.
- `/reference_images` — photos of the actual paper ledger (member Yogesh Kr. Yadav, account 1583) showing the real column layout, the loan/interest figures used to verify the formula, and a second member ledger (Vandana Anand, account 1163). Match your screens and Excel export to this layout.

When anything is ambiguous, prefer the spec document, then ask me before inventing a rule.