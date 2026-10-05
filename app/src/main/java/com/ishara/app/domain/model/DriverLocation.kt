package com.ishara.app.domain.model

import com.ishara.app.core.location.GeoJsonCoordinate

data class DriverLocation(

    val coordinate: GeoJsonCoordinate,

    val heading: Double,

    val speed: Double,

    val accuracy: Double,

    val recordedAt: String

)