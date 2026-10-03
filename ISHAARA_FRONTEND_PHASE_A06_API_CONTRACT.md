# ISHAARA FRONTEND — PHASE A06 API CONTRACT
## DRIVER OPERATIONAL READINESS & AVAILABILITY APIs

This document records the exact backend API contract consumed by the Android frontend for Phase A06.

---

### 1. Authoritative Operational Readiness Evaluation

- **Method**: `GET`
- **Path**: `/api/v1/drivers/me/readiness`
- **Alias**: `/api/v1/drivers/me/operational-readiness`
- **Auth**: `requireAuth` (`Bearer <token>`)
- **Role**: `requireDriverConductor` (`role == "DRIVER_CONDUCTOR"`)
- **Request Headers**:
  - `Authorization: Bearer <session_jwt>`
- **Request Body**: None
- **Response `200 OK`**:
```json
{
  "success": true,
  "statusCode": 200,
  "data": {
    "driverId": "65f1234567890123456789ab",
    "userId": "65f1234567890123456789aa",
    "authorized": true,
    "status": "READY",
    "reasons": [],
    "requirements": {
      "platformVerification": true,
      "agencyMembership": true,
      "profileComplete": true,
      "notSuspended": true,
      "vehicleAssigned": true
    },
    "operatingType": "INDIVIDUAL",
    "agency": {
      "membershipStatus": "APPROVED",
      "agencyId": "65a1234567890123456789ab",
      "agencyName": "Pune Campus Transit"
    },
    "activeVehicle": {
      "id": "65b1234567890123456789ac",
      "registrationNumber": "DL01AB1234",
      "make": "Tata",
      "model": "Tigor EV"
    }
  },
  "message": "Driver operational readiness evaluated successfully."
}
```
- **Error Codes**:
  - `401 Unauthorized`: Missing or invalid session token.
  - `403 Forbidden` (`FORBIDDEN`): Authenticated user does not possess `DRIVER_CONDUCTOR` role.
  - `404 Not Found` (`DRIVER_PROFILE_NOT_FOUND`): Driver profile does not exist yet.
- **Side Effects**: None (strictly read-only idempotent query).

---

### 2. Transition Driver Status to ONLINE

- **Method**: `POST`
- **Path**: `/api/v1/drivers/me/status/online`
- **Alias**: `/api/v1/drivers/me/online`
- **Auth**: `requireAuth` (`Bearer <token>`)
- **Role**: `requireDriverConductor` (`role == "DRIVER_CONDUCTOR"`)
- **Request Headers**:
  - `Authorization: Bearer <session_jwt>`
- **Request Body**: `{}`
- **Response `200 OK`**:
```json
{
  "success": true,
  "statusCode": 200,
  "data": {
    "id": "65f1234567890123456789ab",
    "userId": "65f1234567890123456789aa",
    "verificationStatus": "VERIFIED",
    "status": "ONLINE",
    "currentLocation": null,
    "licenseNumberMasked": "DL***1111",
    "licenseVerifiedAt": "2026-09-26T10:00:00.000Z",
    "operatingType": "INDIVIDUAL",
    "isSuspended": false,
    "createdAt": "2026-09-26T09:00:00.000Z",
    "updatedAt": "2026-09-26T10:05:00.000Z"
  },
  "message": "Driver status set to ONLINE."
}
```
- **Error Codes**:
  - `401 Unauthorized`: Authentication required.
  - `403 Forbidden` (`DRIVER_NOT_VERIFIED`): Verification status is not `VERIFIED`.
  - `403 Forbidden` (`DRIVER_OPERATIONAL_SUSPENDED`): Account is suspended.
  - `403 Forbidden` (`DRIVER_NOT_OPERATIONAL_READY`): Missing approved agency membership or other prerequisite.
- **Side Effects**: Sets `DriverProfile.status = ONLINE`.

---

### 3. Transition Driver Status to OFFLINE

- **Method**: `POST`
- **Path**: `/api/v1/drivers/me/status/offline`
- **Alias**: `/api/v1/drivers/me/offline`
- **Auth**: `requireAuth` (`Bearer <token>`)
- **Role**: `requireDriverConductor` (`role == "DRIVER_CONDUCTOR"`)
- **Request Headers**:
  - `Authorization: Bearer <session_jwt>`
- **Request Body**: `{}`
- **Response `200 OK`**:
```json
{
  "success": true,
  "statusCode": 200,
  "data": {
    "id": "65f1234567890123456789ab",
    "userId": "65f1234567890123456789aa",
    "verificationStatus": "VERIFIED",
    "status": "OFFLINE",
    "currentLocation": null,
    "licenseNumberMasked": "DL***1111",
    "licenseVerifiedAt": "2026-09-26T10:00:00.000Z",
    "operatingType": "INDIVIDUAL",
    "isSuspended": false,
    "createdAt": "2026-09-26T09:00:00.000Z",
    "updatedAt": "2026-09-26T10:10:00.000Z"
  },
  "message": "Driver status set to OFFLINE."
}
```
- **Error Codes**:
  - `400 Bad Request` (`INVALID_DRIVER_STATUS_TRANSITION`): Cannot transition to OFFLINE while actively on a ride (`ON_RIDE`).
  - `401 Unauthorized`: Authentication required.
- **Side Effects**: Sets `DriverProfile.status = OFFLINE`.

---

### 4. Operational Context Snapshot

- **Method**: `GET`
- **Path**: `/api/v1/drivers/me/operations/context`
- **Alias**: `/api/v1/drivers/me/operational-context`
- **Auth**: `requireAuth` (`Bearer <token>`)
- **Role**: `requireDriverConductor` (`role == "DRIVER_CONDUCTOR"`)
- **Query Params**: `timezone` (default: `"Asia/Kolkata"`)
- **Response `200 OK`**: Comprehensive driver snapshot including driver identity, active vehicle, active trip, active rides count, today's summary stats, and embedded readiness payload.
