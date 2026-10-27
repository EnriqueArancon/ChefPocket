package es.upm.miw.chefpocket.utilidades;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

/**
 * Utilidad para la verificación del estado de la red antes de realizar llamadas HTTP.
 * Basado en las pautas oficiales del Tema 4 (Diapositiva 3 - Conectividad).
 */
public class NetworkUtils {

    private static final String LOG_TAG = "ChefPocket";

    /**
     * Comprueba si el dispositivo dispone de una conexión activa a Internet (Wi-Fi, datos móviles o ethernet).
     *
     * @param context Contexto de la aplicación o actividad
     * @return true si hay conexión activa a Internet, false en caso contrario
     */
    public static boolean hayConexion(Context context) {
        if (context == null) {
            Log.w(LOG_TAG, "NetworkUtils: Contexto nulo al comprobar conectividad.");
            return false;
        }

        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            Log.w(LOG_TAG, "NetworkUtils: ConnectivityManager no disponible.");
            return false;
        }

        boolean conectado = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network redActiva = connectivityManager.getActiveNetwork();
            if (redActiva != null) {
                NetworkCapabilities capacidades = connectivityManager.getNetworkCapabilities(redActiva);
                conectado = capacidades != null && (
                        capacidades.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capacidades.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capacidades.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                );
            }
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            conectado = networkInfo != null && networkInfo.isConnected();
        }

        if (conectado) {
            Log.i(LOG_TAG, "NetworkUtils: Estado de red -> CONECTADO A INTERNET.");
        } else {
            Log.w(LOG_TAG, "NetworkUtils: Estado de red -> SIN CONEXIÓN (Modo offline).");
        }

        return conectado;
    }
}
