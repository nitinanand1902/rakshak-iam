# Rakshak: Healthcare IAM Security Platform

This project is a healthcare Identity and Access Management demo built for the assignment brief. It demonstrates authentication, role-based authorization, sensitive identity protection, patient health data access, diagnosis notes, audit logging, and an IAM control dashboard.

## Assignment Mapping

| Requirement | Implementation |
| --- | --- |
| Simple healthcare web app | React frontend with Spring Boot backend |
| Account section with password / Aadhaar / sensitive info | Patient dashboard shows masked Aadhaar; backend stores encrypted identity values |
| General health and vital stats | Patient dashboard shows blood group, health summary, diagnosis, and doctor notes |
| Three roles | `PATIENT`, `DOCTOR`, `TECHNICIAN` |
| Patient can view own data | Patient login shows only their own profile and masked identity |
| Doctor can add diagnosis and notes | Doctor clinical workspace can create diagnosis notes |
| Technician diagnosis/read workflow | Technician can view patient records and diagnosis notes but cannot create diagnosis notes |
| Server communication testbed | Frontend calls real backend endpoints through Vite proxy |
| IAM control evaluation | IAM dashboard calls backend evaluation API and displays risk/score metrics |
| Intrusion / unauthorized access capture | Backend returns `401`/`403`, logs events, and audit page shows security trail |
| RBAC privilege testing | Backend enforces role restrictions and tested blocked escalation paths |

## Tech Stack

- Backend: Spring Boot 3.3, Spring Security, JWT, JPA/Hibernate, H2
- Frontend: React, Vite, Material UI, Recharts
- Security: BCrypt, JWT, AES-256/GCM, RBAC, audit logging, OAuth2/OIDC, SAML 2.0

## Security Features

- Passwords are stored with BCrypt strength 12.
- Aadhaar and other identity values are encrypted using AES-256/GCM before database persistence.
- **OAuth2 / OIDC SSO** — Sign in with Google or Microsoft Azure AD via Spring Security oauth2Login(). New users go through a role-selection onboarding flow; returning users get a direct JWT.
- **SAML 2.0 SSO** — Enterprise SSO via Azure AD (or any SAML IdP) using Spring Security saml2Login(). SP metadata auto-served at `/saml2/service-provider-metadata/default-idp`.
- JWT access tokens (15 min) + refresh tokens (7 days) with provisioning token for SSO onboarding.
- Raw identity values are never returned by APIs; only masked values such as `XXXX-XXXX-1234` are returned.
- JWT access tokens are short-lived.
- Backend RBAC is enforced with Spring Security and `@PreAuthorize`.
- Audit logs are persisted for login, access, diagnosis, identity, IAM, and security events.
- Audit log API masks usernames, resource IDs, and IP addresses.
- Centralized exception handling returns clean JSON errors instead of stack traces.

## Demo Accounts

| Role | Username | Password | What to Verify |
| --- | --- | --- | --- |
| Patient | `patient1` | `Patient@1234` | Own identity, masked Aadhaar, health summary, diagnosis notes |
| Doctor | `doctor1` | `Doctor@1234` | Patient records, add diagnosis, IAM dashboard, audit logs |
| Technician | `tech1` | `Tech@1234` | Patient records and diagnosis notes, no diagnosis create permission |

## Running the Project

Use JDK 21 for the backend. Maven on this machine may default to Java 26, which can break Lombok compilation.

Backend:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home mvn spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev -- --host 127.0.0.1
```

Open:

- Frontend: `http://127.0.0.1:5173`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Main Frontend Flows

1. Log in as `patient1`.
2. Verify the patient dashboard shows:
   - Masked Aadhaar
   - Blood group / health summary
   - Latest diagnosis and doctor notes
   - Role permissions

3. Log in as `doctor1`.
4. Verify:
   - Clinical workspace lists patients
   - Diagnosis notes are visible
   - Doctor can create a diagnosis note
   - IAM Security dashboard shows risk metrics
   - Audit Logs page shows backend audit events

5. Log in as `tech1`.
6. Verify:
   - Technician can view patient records and diagnosis notes
   - Technician cannot create diagnosis notes
   - Technician can run IAM evaluation for users

## Main API Endpoints

| Feature | Method | Endpoint | Access |
| --- | --- | --- | --- |
| Login | `POST` | `/api/v1/auth/login` | Public |
| Logout | `POST` | `/api/v1/auth/logout` | Authenticated |
| Refresh token | `POST` | `/api/v1/auth/refresh` | Public with `X-Refresh-Token` |
| Own patient profile | `GET` | `/api/v1/patients/me` | Patient |
| List patients | `GET` | `/api/v1/patients` | Doctor, Technician |
| Diagnosis notes | `GET` | `/api/v1/diagnosis/patients/{patientId}` | Patient, Doctor, Technician |
| Create diagnosis note | `POST` | `/api/v1/diagnosis` | Doctor |
| Identity documents | `GET` | `/api/v1/identity/patients/{patientId}` | Owner Patient, Doctor, Technician |
| Add identity document | `POST` | `/api/v1/identity/patients/{patientId}` | Owner Patient |
| Verify identity document | `PUT` | `/api/v1/identity/{documentId}/verify` | Doctor, Technician |
| IAM self evaluation | `GET` | `/api/v1/iam/evaluate/me` | Authenticated |
| IAM user evaluation | `GET` | `/api/v1/iam/evaluate/{username}` | Doctor, Technician |
| Audit logs | `GET` | `/api/v1/audit` | Doctor |

## Swagger Testing

1. Open `http://localhost:8080/swagger-ui/index.html`.
2. Call `POST /api/v1/auth/login` with a demo account.
3. Copy the `accessToken`.
4. Click **Authorize** and enter the JWT token.
5. Test protected endpoints according to the role.

Expected examples:

- Patient calling `/api/v1/audit` returns `403 ACCESS_DENIED`.
- Patient calling `/api/v1/patients/me` returns `200`.
- Doctor calling `/api/v1/audit` returns `200`.
- Doctor posting `/api/v1/diagnosis` returns `201`.
- Technician posting `/api/v1/diagnosis` returns `403 ACCESS_DENIED`.

## IAM Evaluation Logic

The IAM evaluation service checks:

- Account enabled/disabled status
- Account locked status
- Failed login attempts
- Identity document presence
- Identity document verification status
- Pending/rejected documents

It returns:

- `LOW`
- `MEDIUM`
- `HIGH`
- `CRITICAL`

The frontend maps this risk into authentication, authorization, and session security scores on the IAM dashboard.

## Threat / RBAC Test Matrix

| Test | Expected Result |
| --- | --- |
| No token calls protected endpoint | `401 AUTHENTICATION_REQUIRED` |
| Patient calls audit API | `403 ACCESS_DENIED` |
| Patient evaluates another user | `403 ACCESS_DENIED` |
| Technician creates diagnosis note | `403 ACCESS_DENIED` |
| Doctor creates diagnosis note | `201 CREATED` |
| Invalid login | `401 INVALID_CREDENTIALS` |
| Empty login request | `400 VALIDATION_ERROR` |
| Invalid identity document type | `422 BUSINESS_RULE_VIOLATION` |

## Exception Handling

All backend errors use a consistent JSON response:

```json
{
  "timestamp": "2026-05-31T...",
  "status": 403,
  "message": "Access denied...",
  "path": "/api/v1/audit",
  "errorCode": "ACCESS_DENIED"
}
```

Handled categories include validation errors, authentication failures, access denied errors, business rule errors, resource not found errors, token errors, and unexpected server errors.

## Verification Commands

Frontend:

```bash
cd frontend
npm run lint
npm run build
```

Backend:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home mvn test
```

## Demo Notes and Production Hardening

This project is intentionally configured for local assignment review. Before production use:

- Move JWT and AES keys out of `application.properties` into environment variables or a secret manager.
- Disable or protect Swagger and H2 console outside the dev profile.
- Restrict public registration so users cannot self-select privileged roles.
- Check refresh token type before issuing a new access token.
- Prefer HttpOnly secure cookies or in-memory access tokens instead of browser `localStorage`.
- Remove demo credentials from the frontend login page.
- Add automated integration tests for the RBAC matrix.

## Project Structure

```text
src/main/java/com/healthcare
  config/       Spring, JWT, OpenAPI, seed data
  controller/   REST API controllers
  service/      Business logic and IAM evaluation
  security/     JWT filter, RBAC, auth handlers
  entity/       JPA entities
  repository/   Data repositories
  util/         AES encryption and masking strategies

frontend/src
  pages/        Login, patient/clinical dashboard, IAM dashboard, audit logs
  services/     Backend API client
  routes/       Protected routes
  context/      Auth state
```
