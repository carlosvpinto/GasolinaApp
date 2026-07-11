package com.carlosvpinto.gasolinaapp // Verifica tu paquete

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

// ==========================================
// 1. MOLDES PARA LEER EL JSON (Basado en Postman)
// ==========================================
data class PricesResponse(
    val status: String,
    val zip: String,
    val gas_prices: List<GasPriceItem>? // Ahora se llama gas_prices
)

data class GasPriceItem(
    // El primer elemento de la lista trae esto:
    val average: String?,
    val lowest: String?,

    // Los demás elementos traen esto:
    val station_id: String?,
    val price: String?,
    val station: String?,
    val address: String?
)

data class StationDataResponse(val status: String, val data: StationDetails?)

data class StationDetails(
    val station_id: String,
    val name: String,
    val coordinates: Coordinates?
)

data class Coordinates(val lat: String, val lng: String)


// ==========================================
// 2. LOS ENDPOINTS DE ZYLA LABS
// ==========================================
interface ZylaApiService {
    // ¡Actualizado a 'precios'!
    @GET("4649/precios")
    suspend fun getPricesByZip(
        @Header("Authorization") token: String,
        @Query("zip") zipCode: String,
        @Query("type") type: String = "regular"
    ): Response<PricesResponse>

    // Asumimos que este sigue igual, usando la nueva URL base
    @GET("24541/station+data")
    suspend fun getStationData(
        @Header("Authorization") token: String,
        @Query("station_id") stationId: String
    ): Response<StationDataResponse>
}

// ==========================================
// 3. EL MOTOR DE CONEXIÓN
// ==========================================
object RetrofitClient {
    // ¡NUEVA URL BASE según tu Postman!
    private const val BASE_URL = "https://zylalabs.com/api/3927/precios+de+la+gasolina+api/"

    val apiService: ZylaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ZylaApiService::class.java)
    }
}