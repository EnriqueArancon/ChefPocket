package es.upm.miw.chefpocket;

import android.app.Application;
import android.util.Log;

import com.google.firebase.database.FirebaseDatabase;

/**
 * Clase Application principal para la inicialización global de servicios en ChefPocket.
 * Configura la persistencia en disco de Firebase Realtime Database para que la aplicación
 * soporte almacenamiento local automático y funcionamiento offline (Tema 3 / Firebase).
 */
public class ChefPocketApp extends Application {

    private static final String LOG_TAG = "ChefPocketApp";

    @Override
    public void onCreate() {
        super.onCreate();

        // Habilitamos la persistencia en disco de Firebase Realtime Database.
        // Debe invocarse antes de cualquier otra referencia a FirebaseDatabase.
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            Log.i(LOG_TAG, "Persistencia en disco de Firebase Realtime Database activada con éxito.");
        } catch (Exception e) {
            Log.w(LOG_TAG, "Persistencia de FirebaseDatabase ya inicializada o no disponible: " + e.getMessage());
        }
    }
}

