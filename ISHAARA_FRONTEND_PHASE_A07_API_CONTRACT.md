# ISHAARA FRONTEND — PHASE A07: API CONTRACT

This document provides the definitive specification of all vehicle and assignment API endpoints consumed by the Ishaara Android frontend under Phase A07.

---

### Endpoint 1: Get Driver Current Assigned Vehicle

- **Method**: `GET`
- **Path**: `/api/v1/vehicles/me/assigned`
- **Alias**: `/api/v1/drivers/me/vehicle`
- **Auth**: `requireAuth` (`Bearer <token>`)
- **Role**: `DRIVER_CONDUCTOR`
- **Request Parameters**: None
- **Request Body**: None
- **Response `200 OK`**:
```json
{
  "success": true,
  "data": {
    "vehicle": {
      "id": "673f1a2b8e33bd0db1dfea4c",
      "agencyId": "673f1a2b8e33bd0db1dfea5d",
      "operatorId": null,
      "assignedDriverId": "673f1a2b8e33bd0db1dfea6e",
      "registrationNumber": "UP32AB1234",
      "vehicleType": "BUS",
      "make": "Tata",
      "model": "Starbus",
      "capacity": 32,
      "ownershipType": "AGENCY",
      "isVerified": true,
      "isActive": true,
      "createdAt": "2026-09-01T08:00:00.000Z",
      "updatedAt": "2026-09-01T08:00:00.000Z"
    },
    "assignment": {
      "id": "673f1a2b8e33bd0db1dfea7f",
      "driverId": "673f1a2b8e33bd0db1dfea6e",
      "vehicleId": "673f1a2b8e33bd0db1dfea4c",
      "agencyId": "673f1a2b8e33bd0db1dfea5d",
      "status": "ACTIVE",
      "assignedAt": "2026-09-01T09:00:00.000Z",
      "unassignedAt": null,
      "assignedBy": "673f1a2b8e33bd0db1dfea1a",
      "assignedByRole": "AGENCY_OWNER",
      "unassignedBy": null,
      "unassignedByRole": null,
      "reason": null,
      "vehicle": { ... },
      "createdAt": "2026-09-01T09:00:00.000Z",
      "updatedAt": "2026-09-01T09:00:00.000Z"
    }
  }
}
```
*Note: If no vehicle is currently assigned, `vehicle` and `assignment` are both `null`.*
- **Errors**:
  - `401 UNAUTHORIZED`: Authentication required
  - `403 FORBIDDEN`: Non-driver role
  - `404 DRIVER_PROFILE_NOT_FOUND`: Driver profile not created
  - `500 SERVER_ERROR`: Backend error
- **Side Effects**: None (read-only query).
- **Frontend Mapping**: `VehicleRemoteDataSource.getMyAssignedVehicle()`, mapped to `AssignedVehicleState` via `VehicleMapper.toDomain()`.
- **Backend Source**: `vehicle.controller.ts:getMyAssignedVehicle` calling `vehicleAssignmentService.getActiveAssignmentForDriver`.

---

### Endpoint 2: List Driver Owned Vehicles

- **Method**: `GET`
- **Path**: `/api/v1/vehicles`
- **Auth**: `requireAuth` (`Bearer <token>`)
- **Role**: `DRIVER_CONDUCTOR`
- **Request Body**: None
- **Response `200 OK`**:
```json
{
  "success": true,
  "data": [
    {
      "id": "673f1a2b8e33bd0db1dfea4c",
      "agencyId": null,
      "operatorId": null,
      "assignedDriverId": "673f1a2b8e33bd0db1dfea6e",
      "registrationNumber": "UP32IND5678",
      "vehicleType": "CAB",
      "make": "Maruti",
      "model": "Dzire",
      "capacity": 4,
      "ownershipType": "INDIVIDUAL",
      "isVerified": false,
      "isActive": true,
      "createdAt": "2026-09-01T08:00:00.000Z",
      "updatedAt": "2026-09-01T08:00:00.000Z"
    }
  ]
}
```
- **Errors**: `401 UNAUTHORIZED`, `403 FORBIDDEN`, `404 DRIVER_PROFILE_NOT_FOUND`.
- **Side Effects**: None.
- **Frontend Mapping**: `VehicleRemoteDataSource.listMyVehicles()`, mapped to `List<Vehicle>`.
- **Backend Source**: `vehicle.controller.ts:list` calling `vehicleService.listMyVehicles`.

---

### Endpoint 3: Register Individual Driver Vehicle

- **Method**: `POST`
- **Path**: `/api/v1/vehicles`
- **Auth**: `requireAuth` (`Bearer <token>`)
- **Role**: `DRIVER_CONDUCTOR`
- **Request Body**:
```json
{
  "registrationNumber": "UP32IND1234",
  "vehicleType": "CAB",
  "make": "Hyundai",
  "model": "Aura",
  "capacity": 4
}
```
- **Response `201 Created`**:
```json
{
  "success": true,
  "statusCode": 201,
  "data": {
    "id": "673f1a2b8e33bd0db1dfea4c",
    "registrationNumber": "UP32IND1234",
    "vehicleType": "CAB",
    "make": "Hyundai",
    "model": "Aura",
    "capacity": 4,
    "ownershipType": "INDIVIDUAL",
    "isVerified": false,
    "isActive": true,
    "createdAt": "2026-09-30T19:00:00.000Z",
    "updatedAt": "2026-09-30T19:00:00.000Z"
  },
  "message": "Vehicle registered successfully."
}
```
- **Errors**:
  - `400 VALIDATION_ERROR`: Missing make, model, registrationNumber, or invalid vehicleType
  - `409 VEHICLE_REGISTRATION_ALREADY_EXISTS`: Plate number already registered in platform
- **Side Effects**: Creates `VehicleModel` record under driver profile ID.
- **Frontend Mapping**: `VehicleRemoteDataSource.createVehicle()`, mapped to `Vehicle`.
- **Backend Source**: `vehicle.controller.ts:create` calling `vehicleService.createVehicle`.

---

### Endpoint 4: Assign Driver to Owned Vehicle

- **Method**: `POST`
- **Path**: `/api/v1/vehicles/:vehicleId/assignments`
- **Auth**: `requireAuth` (`Bearer <token>`)
- **Role**: `DRIVER_CONDUCTOR`
- **Request Body**:
```json
{
  "driverId": "673f1a2b8e33bd0db1dfea6e"
}
```
- **Response `201 Created`**:
```json
{
  "success": true,
  "statusCode": 201,
  "data": {
    "id": "673f1a2b8e33bd0db1dfea7f",
    "driverId": "673f1a2b8e33bd0db1dfea6e",
    "vehicleId": "673f1a2b8e33bd0db1dfea4c",
    "agencyId": null,
    "status": "ACTIVE",
    "assignedAt": "2026-09-30T19:05:00.000Z",
    "unassignedAt": null,
    "assignedBy": "673f1a2b8e33bd0db1dfea6e",
    "assignedByRole": "DRIVER",
    "unassignedBy": null,
    "unassignedByRole": null,
    "reason": null,
    "createdAt": "2026-09-30T19:05:00.000Z",
    "updatedAt": "2026-09-30T19:05:00.000Z"
  },
  "message": "Driver assigned to vehicle successfully."
}
```
- **Errors**:
  - `400 DRIVER_NOT_VERIFIED`: Unverified driver cannot be assigned
  - `403 DRIVER_OPERATIONAL_SUSPENDED`: Suspended driver cannot be assigned
  - `409 DRIVER_ALREADY_ASSIGNED`: Driver currently has another active assignment
  - `409 VEHICLE_ALREADY_ASSIGNED`: Vehicle currently assigned to another driver
- **Side Effects**: Creates active `DriverVehicleAssignment` record, updates denormalized `vehicle.driverId`.
- **Frontend Mapping**: `VehicleRemoteDataSource.assignVehicle()`, mapped to `DriverVehicleAssignment`.
- **Backend Source**: `vehicle.controller.ts:assignDriver` calling `vehicleAssignmentService.assignVehicle`.

---

### Endpoint 5: Unassign Driver from Vehicle

- **Method**: `POST`
- **Path**: `/api/v1/vehicles/:vehicleId/unassign`
- **Auth**: `requireAuth` (`Bearer <token>`)
- **Role**: `DRIVER_CONDUCTOR`
- **Request Body**:
```json
{
  "reason": "Shift rotation completed"
}
```
- **Response `200 OK`**:
```json
{
  "success": true,
  "statusCode": 200,
  "data": {
    "id": "673f1a2b8e33bd0db1dfea7f",
    "driverId": "673f1a2b8e33bd0db1dfea6e",
    "vehicleId": "673f1a2b8e33bd0db1dfea4c",
    "status": "ENDED",
    "assignedAt": "2026-09-30T19:05:00.000Z",
    "unassignedAt": "2026-09-30T19:30:00.000Z",
    "assignedBy": "673f1a2b8e33bd0db1dfea6e",
    "assignedByRole": "DRIVER",
    "reason": "Shift rotation completed"
  },
  "message": "Vehicle unassigned successfully."
}
```
- **Errors**:
  - `400 DRIVER_HAS_ACTIVE_TRIP`: Cannot unassign while driver has active trip or in-flight rides
- **Side Effects**: Marks active assignment `ENDED`, records `unassignedAt`, sets driver `OFFLINE` if currently online.
- **Frontend Mapping**: `VehicleRemoteDataSource.unassignVehicle()`.
- **Backend Source**: `vehicle.controller.ts:unassignDriver` calling `vehicleAssignmentService.unassignVehicle`.

---

### Endpoint 6: View Vehicle Assignment History

- **Method**: `GET`
- **Path**: `/api/v1/vehicles/:vehicleId/assignments`
- **Auth**: `requireAuth` (`Bearer <token>`)
- **Role**: `DRIVER_CONDUCTOR`
- **Response `200 OK`**:
```json
{
  "success": true,
  "statusCode": 200,
  "data": [
    {
      "id": "673f1a2b8e33bd0db1dfea7f",
      "driverId": "673f1a2b8e33bd0db1dfea6e",
      "vehicleId": "673f1a2b8e33bd0db1dfea4c",
      "status": "ENDED",
      "assignedAt": "2026-09-30T19:05:00.000Z",
      "unassignedAt": "2026-09-30T19:30:00.000Z",
      "assignedByRole": "DRIVER",
      "reason": "Shift rotation completed"
    }
  ]
}
```
- **Frontend Mapping**: `VehicleRemoteDataSource.getVehicleAssignmentHistory()`.
- **Backend Source**: `vehicle.controller.ts:getAssignmentHistory`.
