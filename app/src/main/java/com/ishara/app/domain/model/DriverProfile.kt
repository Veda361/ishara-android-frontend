package com.ishara.app.domain.model

data class EmergencyContact(
    val id: String? = null,
    val name: String,
    val phoneNumber: String,
    val relationship: String = "FAMILY"
)

data class DriverProfile(
    val id: String,
    val userId: String,
    val licenseNumberMasked: String,
    val yearsOfExperience: Int,
    val isOnline: Boolean,
    val ratingAverage: Double,
    val totalRatingsCount: Int,
    val emergencyContact: EmergencyContact? = null
)
