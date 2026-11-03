package es.upm.miw.chefpocket.datos.api;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Cliente Singleton de Retrofit configurado con la URL base de TheMealDB,
 * convertidor Gson y cliente OkHttpClient con caché HTTP de 20 MB en disco.
 * Patrón oficial visto en el Tema 4 (Diapositiva 11).
 */
public class RetrofitClient {

    private static final String LOG_TAG = "ChefPocket";
    private static final String BASE_URL = "https://www.themealdb.com/api/json/v1/1/";

    private static RetrofitClient instancia;
    private final TheMealDbApiService apiService;

    private RetrofitClient(Context context) {
        Log.d(LOG_TAG, "RetrofitClient: Inicializando cliente Retrofit con URL: " + BASE_URL);

        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder()
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(6, TimeUnit.SECONDS);

        if (context != null) {
            try {
                File cacheDir = new File(context.getCacheDir(), "http_cache");
                Cache cache = new Cache(cacheDir, 20 * 1024 * 1024); // 20 MB de caché HTTP
                okHttpBuilder.cache(cache);
                Log.d(LOG_TAG, "RetrofitClient: Caché OkHttp de 20MB configurada en: " + cacheDir.getAbsolutePath());
            } catch (Exception e) {
                Log.w(LOG_TAG, "RetrofitClient: No se pudo configurar caché de disco: " + e.getMessage());
            }
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpBuilder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.apiService = retrofit.create(TheMealDbApiService.class);
        Log.i(LOG_TAG, "RetrofitClient: Servicio TheMealDbApiService creado correctamente.");
    }

    public static synchronized void inicializar(Context context) {
        if (instancia == null && context != null) {
            instancia = new RetrofitClient(context.getApplicationContext());
        }
    }

    public static synchronized RetrofitClient getInstance() {
        if (instancia == null) {
            instancia = new RetrofitClient(null);
        }
        return instancia;
    }

    public TheMealDbApiService getApiService() {
        return apiService;
    }
}
