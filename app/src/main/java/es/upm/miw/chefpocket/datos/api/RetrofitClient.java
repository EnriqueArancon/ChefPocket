package es.upm.miw.chefpocket.datos.api;

import android.util.Log;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Cliente Singleton de Retrofit configurado con la URL base de TheMealDB y convertidor Gson.
 * Patrón oficial visto en el Tema 4 (Diapositiva 11).
 */
public class RetrofitClient {

    private static final String LOG_TAG = "ChefPocket";
    private static final String BASE_URL = "https://www.themealdb.com/api/json/v1/1/";

    private static RetrofitClient instancia;
    private final TheMealDbApiService apiService;

    private RetrofitClient() {
        Log.d(LOG_TAG, "RetrofitClient: Inicializando cliente Retrofit con URL: " + BASE_URL);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.apiService = retrofit.create(TheMealDbApiService.class);
        Log.i(LOG_TAG, "RetrofitClient: Servicio TheMealDbApiService creado correctamente.");
    }

    public static synchronized RetrofitClient getInstance() {
        if (instancia == null) {
            instancia = new RetrofitClient();
        }
        return instancia;
    }

    public TheMealDbApiService getApiService() {
        return apiService;
    }
}
