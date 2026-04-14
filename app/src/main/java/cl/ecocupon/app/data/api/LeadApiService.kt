package cl.ecocupon.app.data.api

import cl.ecocupon.app.data.model.LeadRequest
import cl.ecocupon.app.data.model.LeadResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface LeadApiService {

    @POST("leads")
    suspend fun createLead(@Body lead: LeadRequest): Response<LeadResponse>

    companion object {
        // Local dev: usa adb reverse tcp:8000 tcp:8000
        // Producción: cambia a tu URL de Render (ej: https://smarter-os-core.onrender.com)
        private const val BASE_URL = "http://10.0.2.2:8000/api/v1/"
        // private const val BASE_URL = "https://render.ecocupon.cl/api/v1/"

        fun create(): LeadApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(LeadApiService::class.java)
        }
    }
}
