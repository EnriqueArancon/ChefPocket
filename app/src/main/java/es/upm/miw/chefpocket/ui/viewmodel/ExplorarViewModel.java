package es.upm.miw.chefpocket.ui.viewmodel;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import es.upm.miw.chefpocket.datos.api.RetrofitClient;
import es.upm.miw.chefpocket.datos.api.modelos.MealDto;
import es.upm.miw.chefpocket.datos.api.modelos.MealResponse;
import es.upm.miw.chefpocket.utilidades.NetworkUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel para gestionar el estado observable de la pantalla Explorar.
 * Conserva la receta sugerida en memoria para evitar llamadas redundantes a la API
 * ante cambios de configuración (rotación de pantalla) o navegación entre pestañas.
 */
public class ExplorarViewModel extends ViewModel {

    private static final String LOG_TAG = "ChefPocket";

    private final MutableLiveData<MealDto> recetaSugerida = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> modoOffline = new MutableLiveData<>(false);
    private final MutableLiveData<String> mensajeError = new MutableLiveData<>(null);

    public LiveData<MealDto> getRecetaSugerida() {
        return recetaSugerida;
    }

    public LiveData<Boolean> getCargando() {
        return cargando;
    }

    public LiveData<Boolean> getModoOffline() {
        return modoOffline;
    }

    public LiveData<String> getMensajeError() {
        return mensajeError;
    }

    /**
     * Carga una receta sugerida desde la API TheMealDB.
     *
     * @param context Contexto para verificar conectividad mediante NetworkUtils.
     * @param forzarNueva Si es false y ya hay una receta en memoria, se reutiliza.
     *                    Si es true, se solicita una nueva sugerencia a la API.
     */
    public void cargarRecetaSugerida(Context context, boolean forzarNueva) {
        // Si ya tenemos una receta en memoria y no se fuerza una nueva, la conservamos
        if (!forzarNueva && recetaSugerida.getValue() != null) {
            Log.d(LOG_TAG, "ExplorarViewModel: Reutilizando receta en memoria (" +
                    recetaSugerida.getValue().getStrMeal() + "). Sin llamada de red.");
            return;
        }

        // 1. Verificación previa de conexión (Tema 4, Diapositiva 3)
        if (!NetworkUtils.hayConexion(context)) {
            Log.w(LOG_TAG, "ExplorarViewModel: Sin conexión a Internet. Activando estado offline.");
            cargando.setValue(false);
            modoOffline.setValue(true);
            return;
        }

        // 2. Activar indicador de carga y limpiar estados previos
        cargando.setValue(true);
        modoOffline.setValue(false);
        mensajeError.setValue(null);

        Log.i(LOG_TAG, "ExplorarViewModel: Solicitando nueva receta aleatoria a TheMealDB vía Retrofit...");

        // 3. Petición asíncrona mediante Retrofit (Tema 4, Diapositivas 18-19)
        RetrofitClient.getInstance()
                .getApiService()
                .obtenerRecetaAleatoria()
                .enqueue(new Callback<MealResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<MealResponse> call, @NonNull Response<MealResponse> response) {
                        cargando.postValue(false);
                        if (response.isSuccessful() && response.body() != null && response.body().tieneRecetas()) {
                            MealDto meal = response.body().getPrimeraReceta();
                            Log.i(LOG_TAG, "ExplorarViewModel: Receta sugerida recibida con éxito -> ID: " +
                                    meal.getIdMeal() + ", Título: " + meal.getStrMeal());
                            recetaSugerida.postValue(meal);
                        } else {
                            Log.e(LOG_TAG, "ExplorarViewModel: Respuesta no exitosa de TheMealDB. Código HTTP: " + response.code());
                            mensajeError.postValue("Error en el servidor de recetas (" + response.code() + ")");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<MealResponse> call, @NonNull Throwable t) {
                        cargando.postValue(false);
                        Log.e(LOG_TAG, "ExplorarViewModel: Error de red en la llamada Retrofit: " + t.getMessage(), t);
                        mensajeError.postValue(t.getMessage());
                    }
                });
    }
}

