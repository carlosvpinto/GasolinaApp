package com.carlosvpinto.gasolinaapp


import com.carlosvpinto.gasolinaapp.Models.PricesResponse
import com.carlosvpinto.gasolinaapp.Models.StationDataResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

// 1. Definimos los Endpoints tal cual la documentación
interface GasStationApiService {

    // Endpoint 1: Obtener Precios por ZIP
    @GET("4649/prices")
    suspend fun getPricesByZip(
        @Header("Authorization") token: String,
        @Query("zip") zipCode: String,
        @Query("type") fuelType: String = "regular"
    ): Response<PricesResponse>

    // Endpoint 2: Obtener Coordenadas por ID de estación
    @GET("24541/station+data")
    suspend fun getStationData(
        @Header("Authorization") token: String,
        @Query("station_id") stationId: String
    ): Response<StationDataResponse>
}

// 2. Configuramos la URL Base y creamos el objeto Retrofit
object RetrofitClient {
    private const val BASE_URL = "https://zylalabs.com/api/3927/gasoline+prices+api/"

    val apiService: GasStationApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Convierte el JSON a nuestras Data Classes
            .build()
            .create(GasStationApiService::class.java)
    }
}