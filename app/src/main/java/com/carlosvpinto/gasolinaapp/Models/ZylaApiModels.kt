package com.carlosvpinto.gasolinaapp.Models

// Molde para la Respuesta 1 (Precios)
data class PricesResponse(
    val success: Boolean,
    val result: List<PriceResult>
)

data class PriceResult(
    val station_id: String?, // Suponiendo que el API real devuelve el ID aquí
    val name: String,
    val gasoline: String? // Precio regular
)

// Molde para la Respuesta 2 (Datos de Estación y Coordenadas)
data class StationDataResponse(
    val status: String,
    val data: StationDetails
)

data class StationDetails(
    val station_id: String,
    val name: String,
    val coordinates: Coordinates
)

data class Coordinates(
    val lat: String,
    val lng: String
)