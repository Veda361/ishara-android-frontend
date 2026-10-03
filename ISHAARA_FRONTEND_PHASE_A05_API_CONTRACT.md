# ISHAARA FRONTEND PHASE A05 — API CONTRACT SPECIFICATION
**Phase:** A05 — Agency Membership & Fleet Association  
**Target:** DRIVER_CONDUCTOR Role  
**Authoritative Backend:** `ishara-backend/src/modules/agencies/` and `ishara-backend/src/modules/drivers/`

---

## 1. Overview & Forensic Audit Summary

This document specifies the exact REST API contract implemented for Phase A05 in the ISHAARA Android frontend.
Every endpoint documented here was forensically verified against backend controllers, routes, Zod schemas, and services.
No unverified or speculative endpoints were implemented.

---

## 2. Verified API Endpoints

### 2.1 Public Agency Discovery
- **Method:** `GET`
- **Path:** `/api/v1/agencies`
- **Authentication:** Public
- **Role Requirement:** None (Accessible to all callers)
- **Backend Source Location:** `agency.routes.ts:31`, `agency.controller.ts:listAgencies`, `agency.service.ts:listAgencies`
- **Frontend Source Location:** `AgencyRemoteDataSource.kt:listAgencies`
- **Query Parameters:**
  - `search`: string (optional, searches name, businessName, city)
  - `city`: string (optional)
  - `page`: integer (min 1, default 1)
  - `limit`: integer (min 1, max 100, default 20)
- **Response Schema (`200 OK`):**
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "65a1234567890123456789ab",
        "name": "Metro City Transits",
        "businessName": "Metro City Transits Ltd",
        "city": "Ibadan",
        "state": "Oyo",
        "contactPhoneMasked": "+234 803 *** 1234",
        "contactEmail": "contact@metrotransits.ng",
        "status": "ACTIVE",
        "createdAt": "2026-01-15T08:00:00.000Z"
      }
    ],
    "pagination": {
      "page": 1,
      "limit": 20,
      "total": 1,
      "totalPages": 1
    }
  }
}
```
- **Error Codes:**
  - `400 Bad Request`: Invalid pagination parameters.
  - `500 Internal Server Error`: Backend database failure.
- **Side Effects:** None (read-only query).
- **Frontend Model:** Mapped via `AgencyMapper.toDomain` to `List<Agency>`.

---

### 2.2 Get Public Agency Details
- **Method:** `GET`
- **Path:** `/api/v1/agencies/:id`
- **Authentication:** Public
- **Role Requirement:** None
- **Backend Source Location:** `agency.routes.ts:50`, `agency.controller.ts:getAgency`, `agency.service.ts:getAgencyById`
- **Frontend Source Location:** `AgencyRemoteDataSource.kt:getAgencyById`
- **Response Schema (`200 OK`):**
```json
{
  "success": true,
  "data": {
    "id": "65a1234567890123456789ab",
    "name": "Metro City Transits",
    "businessName": "Metro City Transits Ltd",
    "city": "Ibadan",
    "state": "Oyo",
    "contactPhoneMasked": "+234 803 *** 1234",
    "contactEmail": "contact@metrotransits.ng",
    "status": "ACTIVE",
    "createdAt": "2026-01-15T08:00:00.000Z"
  }
}
```
- **Error Codes:**
  - `400 Bad Request`: Malformed MongoDB ObjectId.
  - `404 Not Found`: Agency not found.
  - `500 Internal Server Error`: Backend server error.
- **Side Effects:** None.
- **Frontend Model:** Mapped to `Agency`.

---

### 2.3 Get Current Driver Agency Membership
- **Method:** `GET`
- **Path:** `/api/v1/drivers/me/memberships/current` (Alias: `/api/v1/drivers/me/agencies/current`)
- **Authentication:** Bearer JWT in `Authorization` header
- **Role Requirement:** `DRIVER_CONDUCTOR`
- **Backend Source Location:** `driver.routes.ts:234`, `agency-membership.controller.ts:getCurrentDriverMembership`, `agency-membership.service.ts:getCurrentDriverMembership`
- **Frontend Source Location:** `AgencyRemoteDataSource.kt:getCurrentDriverMembership`
- **Response Schema (`200 OK`):**
```json
{
  "success": true,
  "data": {
    "id": "65b9876543210987654321cd",
    "agencyId": "65a1234567890123456789ab",
    "agencyName": "Metro City Transits",
    "agencyCity": "Ibadan",
    "agencyState": "Oyo",
    "agencyContactEmail": "contact@metrotransits.ng",
    "status": "APPROVED",
    "requestedAt": "2026-03-30T10:00:00.000Z",
    "respondedAt": "2026-03-30T12:00:00.000Z",
    "rejectionReason": null,
    "notes": "Morning corridor preference",
    "createdAt": "2026-03-30T10:00:00.000Z",
    "updatedAt": "2026-03-30T12:00:00.000Z"
  }
}
```
*Note:* If driver has no approved or pending membership, `data` is `null`.
- **Supported `status` Values:** `PENDING`, `APPROVED`, `REJECTED`.
- **Error Codes:**
  - `401 Unauthorized`: Session invalid or expired.
  - `403 Forbidden`: Authenticated user lacks `DRIVER_CONDUCTOR` role.
  - `404 Not Found`: Driver profile has not yet been registered.
  - `500 Internal Server Error`: Backend server error.
- **Side Effects:** None (read-only query).
- **Frontend Model:** Mapped via `AgencyMapper.toDomain` to `DriverAgencyMembership?`.

---

### 2.4 List Driver Agency Memberships
- **Method:** `GET`
- **Path:** `/api/v1/drivers/me/memberships` (Alias: `/api/v1/drivers/me/agencies`)
- **Authentication:** Bearer JWT in `Authorization` header
- **Role Requirement:** `DRIVER_CONDUCTOR`
- **Backend Source Location:** `driver.routes.ts:224`, `agency-membership.controller.ts:listDriverMemberships`, `agency-membership.service.ts:listDriverMemberships`
- **Frontend Source Location:** `AgencyRemoteDataSource.kt:listDriverMemberships`
- **Query Parameters:**
  - `status`: `PENDING` | `APPROVED` | `REJECTED` (optional)
  - `page`: integer (min 1, default 1)
  - `limit`: integer (min 1, max 100, default 20)
- **Response Schema (`200 OK`):**
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "65b9876543210987654321cd",
        "agencyId": "65a1234567890123456789ab",
        "agencyName": "Metro City Transits",
        "agencyCity": "Ibadan",
        "agencyState": "Oyo",
        "agencyContactEmail": "contact@metrotransits.ng",
        "status": "PENDING",
        "requestedAt": "2026-03-30T10:00:00.000Z",
        "respondedAt": null,
        "rejectionReason": null,
        "notes": "Morning corridor preference",
        "createdAt": "2026-03-30T10:00:00.000Z",
        "updatedAt": "2026-03-30T10:00:00.000Z"
      }
    ],
    "pagination": { "page": 1, "limit": 20, "total": 1, "totalPages": 1 }
  }
}
```
- **Error Codes:** `401`, `403`, `404`, `500`.

---

### 2.5 Request Agency Membership
- **Method:** `POST`
- **Path:** `/api/v1/drivers/me/memberships`
- **Authentication:** Bearer JWT in `Authorization` header
- **Role Requirement:** `DRIVER_CONDUCTOR`
- **Backend Source Location:** `driver.routes.ts:214`, `agency-membership.schema.ts:createAgencyMembershipBodySchema`, `agency-membership.service.ts:requestMembership`
- **Frontend Source Location:** `AgencyRemoteDataSource.kt:requestMembership`
- **Request Headers:** `Content-Type: application/json`
- **Request Schema:**
```json
{
  "agencyId": "65a1234567890123456789ab",
  "notes": "Driver application notes (optional, max 500 chars)"
}
```
  - `agencyId`: string (24-character hexadecimal MongoDB ObjectId regex) — required.
  - `notes`: string (max 500 characters) — optional.
  - *Strict Validation:* Schema enforces `.strict()`, rejecting unverified fields.
- **Backend Business Semantics:**
  - Driver must have registered profile with `operatingType === "AGENCY"`.
  - Target agency must be `ACTIVE`.
  - Driver cannot already have an `APPROVED` membership (returns `409 Conflict`).
  - Driver cannot have a concurrent `PENDING` request across any agency (returns `409 Conflict`).
  - If a prior request was `REJECTED`, submitting a new request resets status to `PENDING`.
- **Response Schema (`201 Created`):**
```json
{
  "success": true,
  "data": {
    "id": "65b9876543210987654321cd",
    "agencyId": "65a1234567890123456789ab",
    "agencyName": "Metro City Transits",
    "agencyCity": "Ibadan",
    "agencyState": "Oyo",
    "agencyContactEmail": "contact@metrotransits.ng",
    "status": "PENDING",
    "requestedAt": "2026-03-30T10:00:00.000Z",
    "respondedAt": null,
    "rejectionReason": null,
    "notes": "Driver application notes",
    "createdAt": "2026-03-30T10:00:00.000Z",
    "updatedAt": "2026-03-30T10:00:00.000Z"
  },
  "message": "Agency membership request submitted successfully."
}
```
- **Error Codes:**
  - `400 Bad Request`: Invalid ID format or driver profile has `operatingType === "INDIVIDUAL"`.
  - `401 Unauthorized`: Session invalid.
  - `403 Forbidden`: Caller lacks `DRIVER_CONDUCTOR` role.
  - `404 Not Found`: Target agency or driver profile not found.
  - `409 Conflict`: Active approved membership exists or duplicate pending request exists.
  - `500 Internal Server Error`: Backend database failure.
- **Side Effects:** Creates a new pending `AgencyMembership` document in database.
- **Frontend Model:** Mapped to `DriverAgencyMembership`, updating `DriverMembershipState.Pending`.

---

### 2.6 Cancel Pending Membership Request
- **Method:** `DELETE`
- **Path:** `/api/v1/drivers/me/memberships/:membershipId`
- **Authentication:** Bearer JWT in `Authorization` header
- **Role Requirement:** `DRIVER_CONDUCTOR`
- **Backend Source Location:** `driver.routes.ts:252`, `agency-membership.controller.ts:cancelDriverMembership`, `agency-membership.service.ts:cancelDriverMembershipRequest`
- **Frontend Source Location:** `AgencyRemoteDataSource.kt:cancelMembership`
- **Backend Business Semantics:**
  - IDOR protected: driver can only cancel their own membership request.
  - Invariant: only `PENDING` requests may be cancelled.
  - Deletes the membership record from the database.
- **Response Schema (`200 OK`):**
```json
{
  "success": true,
  "data": {
    "message": "Membership request cancelled successfully.",
    "cancelledMembershipId": "65b9876543210987654321cd"
  },
  "message": "Membership request cancelled successfully."
}
```
- **Error Codes:**
  - `400 Bad Request`: Invalid ID format or membership is not `PENDING`.
  - `401 Unauthorized`: Missing or invalid session.
  - `403 Forbidden`: IDOR violation (membership belongs to another driver).
  - `404 Not Found`: Pending membership request not found.
  - `500 Internal Server Error`: Remote server failure.
- **Side Effects:** Deletes `AgencyMembership` document from database.
- **Frontend Model:** Triggers re-fetch and transition to `DriverMembershipState.NoMembership`.

---

## 3. Discrepancy & Gap Analysis

| Item | Documentation (`api_final_flow.md` §8) | Actual Backend Implementation | Resolution in Frontend |
|---|---|---|---|
| Agency Invitation Endpoints | Referenced conceptual invitation flow | No invitation schema, route, or model exists anywhere in the backend codebase (`grep -rni "invit"` returned 0 matches) | Documented clearly that invitations are not supported by the backend contract. Driver-initiated application (`POST /api/v1/drivers/me/memberships`) is the verified mechanism. |
| Agency Driver Self-Approval | N/A | `agency-membership.service.ts:approveMembership` strictly prevents drivers from approving or rejecting their own requests | Frontend maintains strict separation: no admin approval/rejection endpoints are exposed or callable by the driver application. |
