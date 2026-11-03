package es.upm.miw.chefpocket;

import android.app.Application;
import android.util.Log;

import com.google.firebase.database.FirebaseDatabase;

import es.upm.miw.chefpocket.datos.api.RetrofitClient;
import es.upm.miw.chefpocket.utilidades.TraductorUtils;

/**
 * Clase Application principal para la inicialización global de servicios en ChefPocket.
 * Configura la persistencia en disco de Firebase Realtime Database para que la aplicación
 * soporte almacenamiento local automático y funcionamiento offline (Tema 3 / Firebase),
 * así como la caché HTTP de Retrofit/OkHttp y la caché en disco de traducciones.
 */
public class ChefPocketApp extends Application {

    private static final String LOG_TAG = "ChefPocketApp";

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Habilitamos la persistencia en disco de Firebase Realtime Database
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            Log.i(LOG_TAG, "Persistencia en disco de Firebase Realtime Database activada con éxito.");
        } catch (Exception e) {
            Log.w(LOG_TAG, "Persistencia de FirebaseDatabase ya inicializada o no disponible: " + e.getMessage());
        }

        // 2. Inicializamos el cliente Retrofit con caché HTTP en disco (20 MB)
        RetrofitClient.inicializar(this);

        // 3. Inicializamos la caché en disco de traducciones gastronómicas
        TraductorUtils.inicializarCacheDisco(this);
    }
}
