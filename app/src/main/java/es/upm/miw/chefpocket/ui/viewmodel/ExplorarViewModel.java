package es.upm.miw.chefpocket.ui.viewmodel;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import es.upm.miw.chefpocket.datos.api.RetrofitClient;
import es.upm.miw.chefpocket.datos.api.modelos.MealDto;
import es.upm.miw.chefpocket.datos.api.modelos.MealResponse;
import es.upm.miw.chefpocket.utilidades.NetworkUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel para gestionar el estado observable de la pantalla Explorar.
 * Conserva tanto la receta sugerida como el catálogo filtrado/buscado en memoria,
 * evitando llamadas redundantes de red ante cambios de configuración o navegación de pestañas.
 */
public class ExplorarViewModel extends ViewModel {

    private static final String LOG_TAG = "ChefPocket";

    // Estado de la receta sugerida
    private final MutableLiveData<MealDto> recetaSugerida = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cargandoSugerencia = new MutableLiveData<>(false);

    // Estado del catálogo de recetas y filtros
    private final MutableLiveData<List<MealDto>> recetasCatalogo = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> cargandoCatalogo = new MutableLiveData<>(false);
    private final MutableLiveData<String> categoriaActiva = new MutableLiveData<>("Todas");

    // Estados generales
    private final MutableLiveData<Boolean> modoOffline = new MutableLiveData<>(false);
    private final MutableLiveData<String> mensajeError = new MutableLiveData<>(null);

    public LiveData<MealDto> getRecetaSugerida() {
        return recetaSugerida;
    }

    public LiveData<Boolean> getCargandoSugerencia() {
        return cargandoSugerencia;
    }

    public LiveData<List<MealDto>> getRecetasCatalogo() {
        return recetasCatalogo;
    }

    public LiveData<Boolean> getCargandoCatalogo() {
        return cargandoCatalogo;
    }

    public LiveData<String> getCategoriaActiva() {
        return categoriaActiva;
    }

    public LiveData<Boolean> getModoOffline() {
        return modoOffline;
    }

    public LiveData<String> getMensajeError() {
        return mensajeError;
    }

    /**
     * Carga una receta sugerida desde la API TheMealDB.
     */
    public void cargarRecetaSugerida(Context context, boolean forzarNueva) {
        if (!forzarNueva && recetaSugerida.getValue() != null) {
            Log.d(LOG_TAG, "ExplorarViewModel: Reutilizando receta en memoria (" +
                    recetaSugerida.getValue().getStrMeal() + "). Sin llamada de red.");
            return;
        }

        if (!NetworkUtils.hayConexion(context)) {
            Log.w(LOG_TAG, "ExplorarViewModel: Sin conexión para sugerencia.");
            cargandoSugerencia.setValue(false);
            modoOffline.setValue(true);
            return;
        }

        cargandoSugerencia.setValue(true);
        modoOffline.setValue(false);
        mensajeError.setValue(null);

        RetrofitClient.getInstance()
                .getApiService()
                .obtenerRecetaAleatoria()
                .enqueue(new Callback<MealResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<MealResponse> call, @NonNull Response<MealResponse> response) {
                        cargandoSugerencia.postValue(false);
                        if (response.isSuccessful() && response.body() != null && response.body().tieneRecetas()) {
                            MealDto meal = response.body().getPrimeraReceta();
                            Log.i(LOG_TAG, "ExplorarViewModel: Sugerencia recibida: " + meal.getStrMeal());
                            recetaSugerida.postValue(meal);
                        } else {
                            mensajeError.postValue("No se pudo obtener la sugerencia");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<MealResponse> call, @NonNull Throwable t) {
                        cargandoSugerencia.postValue(false);
                        mensajeError.postValue(t.getMessage());
                    }
                });
    }

    /**
     * Carga el catálogo filtrando por categoría o mostrando todas las recetas por defecto.
     *
     * @param context Contexto de la app para comprobar conectividad.
     * @param categoriaEnglish Categoría en inglés de TheMealDB (o "Todas").
     */
    public void cargarCatalogoPorCategoria(Context context, String categoriaEnglish) {
        if (!NetworkUtils.hayConexion(context)) {
            modoOffline.setValue(true);
            return;
        }

        String cat = (categoriaEnglish == null || categoriaEnglish.trim().isEmpty()) ? "Todas" : categoriaEnglish;
        categoriaActiva.setValue(cat);
        cargandoCatalogo.setValue(true);
        modoOffline.setValue(false);

        Call<MealResponse> call;
        if (cat.equalsIgnoreCase("Todas")) {
            call = RetrofitClient.getInstance().getApiService().buscarRecetasPorNombre("");
        } else {
            call = RetrofitClient.getInstance().getApiService().filtrarPorCategoria(cat);
        }

        call.enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(@NonNull Call<MealResponse> call, @NonNull Response<MealResponse> response) {
                cargandoCatalogo.postValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().getMeals() != null) {
                    List<MealDto> lista = response.body().getMeals();
                    if (!cat.equalsIgnoreCase("Todas")) {
                        for (MealDto m : lista) {
                            if (m.getStrCategory() == null || m.getStrCategory().isEmpty()) {
                                m.setStrCategory(cat);
                            }
                        }
                    }
                    Log.i(LOG_TAG, "ExplorarViewModel: Catálogo cargado con " + lista.size() + " recetas para categoría: " + cat);
                    recetasCatalogo.postValue(lista);
                } else {
                    recetasCatalogo.postValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(@NonNull Call<MealResponse> call, @NonNull Throwable t) {
                cargandoCatalogo.postValue(false);
                Log.e(LOG_TAG, "ExplorarViewModel: Error al cargar catálogo de recetas: " + t.getMessage());
            }
        });
    }

    /**
     * Busca recetas en TheMealDB que coincidan con el texto introducido en el buscador.
     */
    public void buscarRecetas(Context context, String query) {
        if (!NetworkUtils.hayConexion(context)) {
            modoOffline.setValue(true);
            return;
        }

        String termino = query != null ? query.trim() : "";
        categoriaActiva.setValue(termino.isEmpty() ? "Todas" : "SEARCH:" + termino);
        cargandoCatalogo.setValue(true);
        modoOffline.setValue(false);

        RetrofitClient.getInstance()
                .getApiService()
                .buscarRecetasPorNombre(query != null ? query.trim() : "")
                .enqueue(new Callback<MealResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<MealResponse> call, @NonNull Response<MealResponse> response) {
                        cargandoCatalogo.postValue(false);
                        if (response.isSuccessful() && response.body() != null && response.body().getMeals() != null) {
                            List<MealDto> lista = response.body().getMeals();
                            Log.i(LOG_TAG, "ExplorarViewModel: Búsqueda '" + query + "' devolvió " + lista.size() + " resultados.");
                            recetasCatalogo.postValue(lista);
                        } else {
                            recetasCatalogo.postValue(new ArrayList<>());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<MealResponse> call, @NonNull Throwable t) {
                        cargandoCatalogo.postValue(false);
                        Log.e(LOG_TAG, "ExplorarViewModel: Error en búsqueda: " + t.getMessage());
                    }
                });
    }
}
