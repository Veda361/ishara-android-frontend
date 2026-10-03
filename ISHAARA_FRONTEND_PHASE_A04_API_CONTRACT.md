# ISHAARA FRONTEND PHASE A04 — API CONTRACT SPECIFICATION
**Phase:** A04 — Driver Onboarding & Verification  
**Target:** DRIVER_CONDUCTOR Role  
**Authoritative Backend:** `ishara-backend/src/modules/drivers/` (`driver.routes.ts`, `driver.schema.ts`, `driver.service.ts`, `driver.model.ts`)

---

## 1. Overview & Verification Audit

This document describes the exact REST API contract implemented for Phase A04 in the ISHAARA Android frontend.
Every endpoint listed here was forensically verified against the actual backend implementation in `ishara-backend`.
No unverified or speculative endpoints were created.

---

## 2. API Endpoints Contract

### 2.1 Get Driver Profile
- **Method:** `GET`
- **Path:** `/api/v1/drivers/me`
- **Authentication:** Bearer JWT in `Authorization` header (`Bearer <token>`)
- **Role Requirement:** `DRIVER_CONDUCTOR`
- **Source of Truth:** `driver.routes.ts:32`, `driver.service.ts:getDriverProfile`
- **Request Body:** None
- **Response Schema (`200 OK`):**
```json
{
  "success": true,
  "data": {
    "id": "drv_uuid",
    "userId": "usr_uuid",
    "licenseNumber": "DL-1234567890",
    "verificationStatus": "PENDING",
    "profileStatus": "INCOMPLETE",
    "operationalStatus": "OFF_DUTY",
    "agencyId": null,
    "emergencyContact": {
      "name": "Jane Doe",
      "phoneNumber": "+919876543210",
      "relationship": "Spouse"
    },
    "rejectionReason": null,
    "verifiedAt": null,
    "createdAt": "2026-03-30T10:00:00.000Z",
    "updatedAt": "2026-03-30T10:00:00.000Z"
  }
}
```
- **Error Responses:**
  - `401 Unauthorized`: Missing or invalid session JWT (`AuthenticationRequiredError`).
  - `403 Forbidden`: Authenticated user lacks `DRIVER_CONDUCTOR` role (`ForbiddenError`).
  - `404 Not Found`: No driver profile exists for this user (`DriverNotFoundError`). Frontend maps this cleanly to `DriverOnboardingState.NeedsOnboarding`.
  - `429 Too Many Requests`: Rate limit exceeded.
  - `500 Internal Server Error`: Server failure.
- **Frontend Model Mapping:** Mapped via `DriverMapper.toCleanDomain(...)` to domain entity `DriverProfile`.

---

### 2.2 Create Driver Profile (Driver Onboarding)
- **Method:** `POST`
- **Path:** `/api/v1/drivers/me`
- **Authentication:** Bearer JWT in `Authorization` header (`Bearer <token>`)
- **Role Requirement:** `DRIVER_CONDUCTOR`
- **Source of Truth:** `driver.routes.ts:38`, `driver.schema.ts:createDriverProfileSchema`, `driver.service.ts:createDriverProfile`
- **Request Headers:** `Content-Type: application/json`
- **Request Schema:**
```json
{
  "licenseNumber": "DL-987654321",
  "emergencyContact": {
    "name": "Jane Doe",
    "phoneNumber": "+919876543210",
    "relationship": "Spouse"
  }
}
```
  - `licenseNumber`: `string` (min 3, max 50 characters) — required.
  - `emergencyContact`: optional object:
    - `name`: `string` (min 2, max 100) — required if contact provided.
    - `phoneNumber`: `string` (min 7, max 20) — required if contact provided.
    - `relationship`: `string` (min 2, max 50) — required if contact provided.
- **Response Schema (`201 Created`):**
```json
{
  "success": true,
  "data": {
    "id": "drv_uuid",
    "userId": "usr_uuid",
    "licenseNumber": "DL-987654321",
    "verificationStatus": "PENDING",
    "profileStatus": "COMPLETE",
    "operationalStatus": "OFF_DUTY",
    "agencyId": null,
    "emergencyContact": {
      "name": "Jane Doe",
      "phoneNumber": "+919876543210",
      "relationship": "Spouse"
    },
    "rejectionReason": null,
    "verifiedAt": null,
    "createdAt": "2026-03-30T10:00:00.000Z",
    "updatedAt": "2026-03-30T10:00:00.000Z"
  }
}
```
- **Error Responses:**
  - `400 Bad Request / 422 Unprocessable`: Validation failed (e.g. invalid license length or phone format).
  - `401 Unauthorized`: Session expired or invalid.
  - `403 Forbidden`: User does not possess `DRIVER_CONDUCTOR` role.
  - `409 Conflict`: Driver profile already exists for this user account (`DriverAlreadyExistsError`). Frontend reconciles this by fetching the existing authoritative profile from `/api/v1/drivers/me` without crashing or looping.
  - `500 Server Error`: Internal failure.
- **Frontend Model Mapping:** Mapped to domain entity `DriverProfile` and emits updated `DriverOnboardingState`.

---

### 2.3 Update Driver Profile
- **Method:** `PUT`
- **Path:** `/api/v1/drivers/me`
- **Authentication:** Bearer JWT in `Authorization` header (`Bearer <token>`)
- **Role Requirement:** `DRIVER_CONDUCTOR`
- **Source of Truth:** `driver.routes.ts:44`, `driver.schema.ts:updateDriverProfileSchema`, `driver.service.ts:updateDriverProfile`
- **Request Headers:** `Content-Type: application/json`
- **Request Schema:**
```json
{
  "licenseNumber": "DL-987654321-RENEWED",
  "emergencyContact": {
    "name": "Jane Smith",
    "phoneNumber": "+919876543210",
    "relationship": "Sister"
  }
}
```
- **Response Schema (`200 OK`):**
```json
{
  "success": true,
  "data": {
    "id": "drv_uuid",
    "userId": "usr_uuid",
    "licenseNumber": "DL-987654321-RENEWED",
    "verificationStatus": "PENDING",
    "profileStatus": "COMPLETE",
    "operationalStatus": "OFF_DUTY",
    "agencyId": null,
    "emergencyContact": { ... },
    "rejectionReason": null,
    "verifiedAt": null,
    "createdAt": "...",
    "updatedAt": "..."
  }
}
```
- **Error Responses:**
  - `400 / 422`: Validation error.
  - `401`: Unauthorized.
  - `403`: Forbidden.
  - `404`: Driver profile not found.
  - `500`: Internal error.

---

### 2.4 Get Driver Verification Status
- **Method:** `GET`
- **Path:** `/api/v1/drivers/me/verification`
- **Authentication:** Bearer JWT in `Authorization` header (`Bearer <token>`)
- **Role Requirement:** `DRIVER_CONDUCTOR`
- **Source of Truth:** `driver.routes.ts:50`, `driver.service.ts:getDriverVerificationStatus`
- **Request Body:** None
- **Response Schema (`200 OK`):**
```json
{
  "success": true,
  "data": {
    "driverId": "drv_uuid",
    "verificationStatus": "PENDING",
    "rejectionReason": null,
    "verifiedAt": null
  }
}
```
- **Supported `verificationStatus` Values in Backend:**
  - `PENDING`
  - `VERIFIED`
  - `REJECTED`
  - `SUSPENDED`
- **Error Responses:**
  - `401 Unauthorized`: Session expired or invalid.
  - `403 Forbidden`: User lacks `DRIVER_CONDUCTOR` role.
  - `404 Not Found`: Driver profile does not exist.
  - `500 Internal Server Error`: Remote server failure.
- **Frontend Model Mapping:** Mapped to domain model `DriverVerificationDetails`.

---

### 2.5 Submit Driver Verification
- **Method:** `POST`
- **Path:** `/api/v1/drivers/me/verification`
- **Authentication:** Bearer JWT in `Authorization` header (`Bearer <token>`)
- **Role Requirement:** `DRIVER_CONDUCTOR`
- **Source of Truth:** `driver.routes.ts:56`, `driver.schema.ts:submitDriverVerificationSchema`, `driver.service.ts:submitDriverVerification`
- **Request Headers:** `Content-Type: application/json`
- **Request Schema:**
```json
{
  "notes": "Optional notes from driver regarding license or credentials"
}
```
  - `notes`: `string` (max 500 characters) — optional.
  - *Critical Backend Fact:* The Zod schema `submitDriverVerificationSchema` enforces `.strict()`. Sending unverified fields (like `documentUrls`) causes `400 Bad Request`.
- **Backend Business Semantics:**
  - If driver is already `VERIFIED`, returns `409 Conflict` (`"Driver is already verified."`).
  - If verification is already `PENDING`, returns `409 Conflict` (`"Driver verification is already pending."`).
  - Otherwise updates driver status to `PENDING` and returns updated verification record.
- **Response Schema (`200 OK`):**
```json
{
  "success": true,
  "data": {
    "driverId": "drv_uuid",
    "verificationStatus": "PENDING",
    "rejectionReason": null,
    "verifiedAt": null
  }
}
```
- **Error Responses:**
  - `400 Bad Request`: Validation error (e.g. notes exceed 500 characters or unknown fields present).
  - `401 Unauthorized`: Missing or invalid session JWT.
  - `403 Forbidden`: Authenticated user lacks `DRIVER_CONDUCTOR` role.
  - `404 Not Found`: Driver profile does not exist.
  - `409 Conflict`: Driver already verified or already pending review.
  - `500 Server Error`: Backend database failure.

---

## 3. Discrepancy Analysis

| Item | Documentation (`api_final_flow.md` §5.1) | Actual Backend (`driver.schema.ts`) | Resolution in Frontend |
|---|---|---|---|
| Verification Submission Body | Listed `documentUrls: string[]` | `submitDriverVerificationSchema` has `notes: z.string().max(500).optional()` with `.strict()` | Frontend sends strictly verified `{ notes: string? }` and documents document upload deferral for future backend storage phase. |
| Automatic Profile Creation | Stated in early comments that profile is auto-created on registration | `GET /api/v1/drivers/me` returns `404` until driver submits `POST /api/v1/drivers/me` | Frontend explicitly prompts for onboarding upon receiving 404, preventing silent auto-fabrication of fake licenses. |
