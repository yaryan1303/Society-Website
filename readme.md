# ESIC Society — Account Management System

A web-based Account Management System for the **ESIC Employees Cooperative Credit & Thrift
Society Ltd.** that digitizes the hand-written "Personal Ledger": per-member **shares,
compulsory & other deposits, loans with 8% reducing-balance interest**, the *"members in whose
favour stood"* register, **per-financial-year ledgers (Apr–Mar) with year-end close &
carry-forward**, and **Excel export** — with a public branded landing page, JWT auth, and
separate admin / member dashboards.

- **Frontend:** React (Vite + React Router), plain CSS
- **Backend:** Spring Boot 3 (Java 21), Spring Web / Security / Data JPA, Flyway, Apache POI
- **Database:** PostgreSQL (required) — Flyway migrations, `ddl-auto=validate`
- **Auth:** stateless JWT, BCrypt password hashing

---

## Repository layout

```
/backend     Spring Boot API (Maven)
/frontend    React app (Vite)
docker-compose.yml   PostgreSQL (+ optional backend & frontend under the "full" profile)
.env.example         all configuration, no secrets
/reference_images    photos of the real paper ledger
```

---

## Quick start

### Option A — one command (everything in Docker)

```bash
cp .env.example .env          # then edit secrets (JWT_SECRET, ADMIN_PASSWORD, SMTP…)
docker compose --profile full up --build
```

- Frontend → http://localhost:5173
- Backend  → http://localhost:8080
- PostgreSQL → localhost:5432

### Option B — database in Docker, apps run locally (best for development)

```bash
# 1. Start PostgreSQL
docker compose up -d db

# 2. Backend (port 8080)
cd backend
mvn spring-boot:run

# 3. Frontend (port 5173) — in another terminal
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**.

---

## Seeded demo accounts

On first start the backend seeds (idempotent):

| Role | Account No. | Password | Notes |
|------|-------------|----------|-------|
| **Admin** | `ADMIN001` | `Admin@12345` | From `ADMIN_ACCOUNT_NO` / `ADMIN_PASSWORD`. Change after first login. |
| **Member** | `1583` | `Demo@12345` | Demo member *Yogesh Kr. Yadav* with sample shares, deposits and a loan that reproduces the reference ledger figures (₹1,83,000 → ₹1,220, etc.). Set `SEED_DEMO=false` to skip. |

> Real member accounts created by an admin get a random temporary password and are **forced to
> change it on first login**. The demo member skips the forced change so you can click straight in.

---

## Configuration

All settings come from environment variables (see `.env.example`); nothing is hard-coded.

| Variable | Purpose | Default |
|----------|---------|---------|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | PostgreSQL connection | local dev values |
| `JWT_SECRET` | HS256 signing key (**≥ 32 chars**) | dev placeholder — override in prod |
| `JWT_EXPIRY_MIN` | Token lifetime (minutes) | `240` |
| `APP_BASE_URL` | Frontend URL used in reset-link emails | `http://localhost:5173` |
| `ADMIN_ACCOUNT_NO` / `ADMIN_PASSWORD` / `ADMIN_EMAIL` | Seeded admin | `ADMIN001` / `Admin@12345` |
| `SEED_DEMO` | Seed the demo member 1583 | `true` |
| `COMPULSORY_DEPOSIT_DEFAULT` | Default monthly deposit | `1500` |
| `LOAN_ANNUAL_RATE_PCT` | Loan interest rate (% p.a.) | `8` |
| `MAIL_ENABLED` | Send real email vs. log the link | `false` |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` / `MAIL_FROM` | SMTP (provide your own) | — |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins | localhost dev ports |

### Email

The email-based **forgot/reset** and **welcome** flows use SMTP. Set `MAIL_ENABLED=true` and the
`MAIL_*` variables to send real mail. When `MAIL_ENABLED=false` (default for local demos), the
reset/welcome content — including the reset link — is **logged to the backend console** instead,
so the whole flow is testable without an SMTP server.

---

## Key business rules (as implemented)

- **Interest** = `outstanding × 8% ÷ 12`, rounded to the nearest rupee (HALF_UP). Reducing
  balance: each month's charge is on the current outstanding only. Verified against the ledger:
  `183000→1220, 173000→1153, 163000→1087, 153000→1020` (see the unit test).
- **Monthly interest is admin-posted** per loan ("Post interest"); unpaid interest accumulates
  as a running interest balance.
- **Repayments split into principal + interest**; the loan and interest balances each drop, and
  the next interest charge is on the new lower balance.
- **Payment mode** CASH/BANK is tagged on every repayment; **a receipt number is required for
  CASH** (enforced server-side).
- **Compulsory deposit** is a fixed monthly amount (default ₹1,500, per member); the admin
  records each month.
- **Year-end close (31 March)** locks the year and carries shares, compulsory deposit
  (remainder), other deposit, loan outstanding and interest balance forward as opening balances.
  The admin enters a per-member amount to move from the compulsory deposit into *"members in
  whose favour stood"* (e.g. 18,000 → 5,000 favour-stood + 13,000 carried).
- **Multiple concurrent loans** per member are supported; each keeps its own balances.
- **Security:** every member-data endpoint enforces *owner-or-admin* server-side — a member
  requesting another member's data gets **403**. Write/admin endpoints require `ROLE_ADMIN`.
  All balances are computed by the backend and never trusted from the client.

---

## Committee carousel & photos

The landing page shows an auto-scrolling, swipeable carousel of committee members read from
`frontend/src/data/members.json`. Drop the real photos into `frontend/public/members/` using the
filenames in that JSON (e.g. `virender-dahiya.jpg`). Until then a neutral placeholder avatar is
shown automatically.

---

## Tests

```bash
cd backend
mvn test
```

- **`InterestCalculatorTest`** — pure unit tests proving the interest engine reproduces the
  reference ledger figures exactly (and rounding / edge cases). No database required.

> Integration tests against a real PostgreSQL (via Testcontainers) for the repayment flow and
> year-end carry-forward can be added later; they were intentionally left out of this build.

---

## API overview

| Area | Endpoints |
|------|-----------|
| Auth | `POST /auth/login`, `/auth/change-password`, `/auth/forgot-password`, `/auth/reset-password` |
| Members (admin) | `GET/POST /members`, `GET/PUT/DELETE /members/{id}`, `POST /members/{id}/reset-password` |
| Financial years | `GET /financial-years`, `/financial-years/current`, `POST /financial-years` |
| Ledgers | `GET/POST /members/{id}/{shares\|compulsory-deposits\|other-deposits}`, `PUT/DELETE …/{entryId}` |
| Loans | `GET/POST /members/{id}/loans`, `POST …/{loanId}/interest`, `…/repayments`, `…/close`, `DELETE …/txns/{txnId}` |
| Favour stood | `GET/POST /members/{id}/favour-stood`, `DELETE …/{entryId}` |
| Year-end | `GET /financial-years/{id}/year-end/preview`, `POST /financial-years/{id}/close` |
| Export | `GET /members/{id}/export`, `GET /export/all` (admin) |

All non-auth endpoints require the `Authorization: Bearer <jwt>` header.

---

## Notes & assumptions

- Money is stored as `NUMERIC(15,2)` / `BigDecimal` — never floating point.
- The brief lists the **interest balance** among carry-forward items, so it is carried forward;
  if the society resets interest yearly instead, adjust `YearEndService`.
- Deleting a member cascades to their ledger/loan rows (FK `ON DELETE CASCADE`). Prefer
  deactivating (set inactive) to preserve history.
