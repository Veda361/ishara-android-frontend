package com.ishara.app.domain.model

import com.ishara.app.core.location.GeoJsonCoordinate

data class RoutePolyline(

    val coordinates: List<GeoJsonCoordinate>

)