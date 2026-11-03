package es.upm.miw.chefpocket.ui.viewmodel;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import es.upm.miw.chefpocket.datos.api.RetrofitClient;
import es.upm.miw.chefpocket.datos.api.modelos.MealDto;
import es.upm.miw.chefpocket.datos.api.modelos.MealResponse;
import es.upm.miw.chefpocket.utilidades.NetworkUtils;
import es.upm.miw.chefpocket.utilidades.TraductorUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel para gestionar el estado observable de la pantalla Explorar.
 * Implementa una arquitectura de alto rendimiento con:
 * 1. Caché persistente en disco (arranque instantáneo en 0 ms).
 * 2. Buffer de precarga en segundo plano de la siguiente sugerencia (cambio inmediato al pulsar "Probar otra").
 * 3. Caché en memoria por categorías (conmutación instantánea de pestañas).
 */
public class ExplorarViewModel extends ViewModel {

    private static final String LOG_TAG = "ChefPocket";
    private static final String PREFS_CACHE = "chefpocket_explorar_cache";
    private static final String KEY_SUGERIDA = "cache_receta_sugerida";
    private static final String KEY_CATALOGO_TODAS = "cache_catalogo_todas";

    // Letras del abecedario con mayor catálogo en TheMealDB para rotar las recetas destacadas
    private static final char[] LETRAS_DESTACADAS = {'b', 'c', 'm', 'p', 's', 't', 'a'};

    private final Gson gson = new Gson();

    // Estado de la receta sugerida
    private final MutableLiveData<MealDto> recetaSugerida = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cargandoSugerencia = new MutableLiveData<>(false);

    // Buffer de precarga para cambio inmediato de propuesta culinaria
    private MealDto bufferProximaSugerencia = null;
    private boolean precargandoSiguiente = false;

    // Estado del catálogo de recetas y filtros
    private final MutableLiveData<List<MealDto>> recetasCatalogo = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> cargandoCatalogo = new MutableLiveData<>(false);
    private final MutableLiveData<String> categoriaActiva = new MutableLiveData<>("Todas");

    // Caché en memoria de categorías ya cargadas para conmutación instantánea (0 ms)
    private final Map<String, List<MealDto>> cacheCategorias = new ConcurrentHashMap<>();

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
     * Carga de forma ultrarrápida (0 ms) los datos cacheados en disco para que la pantalla inicial
     * aparezca pintada de inmediato sin skeletons ni pantallas blancas.
     */
    public void cargarCacheInicial(Context context) {
        if (recetaSugerida.getValue() != null && recetasCatalogo.getValue() != null && !recetasCatalogo.getValue().isEmpty()) {
            return;
        }

        try {
            SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_CACHE, Context.MODE_PRIVATE);
            String jsonSugerida = prefs.getString(KEY_SUGERIDA, null);
            if (jsonSugerida != null && recetaSugerida.getValue() == null) {
                MealDto cachedMeal = gson.fromJson(jsonSugerida, MealDto.class);
                if (cachedMeal != null) {
                    recetaSugerida.setValue(cachedMeal);
                    Log.d(LOG_TAG, "ExplorarViewModel: Sugerencia inicial servida desde caché de disco: " + cachedMeal.getStrMeal());
                }
            }

            String jsonCatalogo = prefs.getString(KEY_CATALOGO_TODAS, null);
            if (jsonCatalogo != null && (recetasCatalogo.getValue() == null || recetasCatalogo.getValue().isEmpty())) {
                Type tipoLista = new TypeToken<List<MealDto>>() {}.getType();
                List<MealDto> cachedList = gson.fromJson(jsonCatalogo, tipoLista);
                if (cachedList != null && !cachedList.isEmpty()) {
                    recetasCatalogo.setValue(cachedList);
                    cacheCategorias.put("Todas", cachedList);
                    Log.d(LOG_TAG, "ExplorarViewModel: Catálogo inicial servido desde caché de disco (" + cachedList.size() + " recetas).");
                }
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "ExplorarViewModel: Error al leer caché inicial: " + e.getMessage());
        }
    }

    /**
     * Carga una receta sugerida. Si se solicita forzar una nueva y ya disponemos de una precargada
     * en el buffer en memoria, la entrega al instante sin latencia de red.
     */
    public void cargarRecetaSugerida(Context context, boolean forzarNueva) {
        // Si no se fuerza nueva y ya tenemos una en memoria, la reutilizamos
        if (!forzarNueva && recetaSugerida.getValue() != null) {
            Log.d(LOG_TAG, "ExplorarViewModel: Reutilizando receta en memoria (" +
                    recetaSugerida.getValue().getStrMeal() + ").");
            precargarSiguienteSugerencia(context);
            return;
        }

        // Si se fuerza nueva ("Probar otra") y disponemos del buffer listo, entrega instantánea (0 ms)
        if (forzarNueva && bufferProximaSugerencia != null) {
            MealDto instantanea = bufferProximaSugerencia;
            bufferProximaSugerencia = null;
            cargandoSugerencia.setValue(false);
            recetaSugerida.setValue(instantanea);
            guardarSugerenciaEnDisco(context, instantanea);
            Log.i(LOG_TAG, "ExplorarViewModel: Entrega instantánea desde buffer -> " + instantanea.getStrMeal());
            precargarSiguienteSugerencia(context);
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
                            guardarSugerenciaEnDisco(context, meal);
                            precargarSiguienteSugerencia(context);
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
     * Precarga en segundo plano la siguiente sugerencia para que la próxima pulsación de "Probar otra"
     * no sufra retardo de red.
     */
    private void precargarSiguienteSugerencia(Context context) {
        if (precargandoSiguiente || !NetworkUtils.hayConexion(context)) return;
        precargandoSiguiente = true;

        RetrofitClient.getInstance()
                .getApiService()
                .obtenerRecetaAleatoria()
                .enqueue(new Callback<MealResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<MealResponse> call, @NonNull Response<MealResponse> response) {
                        precargandoSiguiente = false;
                        if (response.isSuccessful() && response.body() != null && response.body().tieneRecetas()) {
                            bufferProximaSugerencia = response.body().getPrimeraReceta();
                            Log.d(LOG_TAG, "ExplorarViewModel: Próxima sugerencia precargada en buffer: " + bufferProximaSugerencia.getStrMeal());
                            // Precalentar traducciones en memoria para máxima velocidad
                            if (TraductorUtils.esEspanol() && bufferProximaSugerencia.getStrMeal() != null) {
                                TraductorUtils.traducirTextoAsync(bufferProximaSugerencia.getStrMeal(), (t, ok) -> {});
                            }
                            if (TraductorUtils.esEspanol() && bufferProximaSugerencia.getStrInstructions() != null) {
                                TraductorUtils.traducirInstruccionesAsync(bufferProximaSugerencia.getStrInstructions(), (t, ok) -> {});
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<MealResponse> call, @NonNull Throwable t) {
                        precargandoSiguiente = false;
                    }
                });
    }

    /**
     * Carga el catálogo filtrando por categoría o mostrando la selección destacada.
     * Utiliza caché en memoria para que volver a pulsar cualquier categoría sea instantáneo (0 ms).
     */
    public void cargarCatalogoPorCategoria(Context context, String categoriaEnglish) {
        String cat = (categoriaEnglish == null || categoriaEnglish.trim().isEmpty()) ? "Todas" : categoriaEnglish;
        categoriaActiva.setValue(cat);

        // Si ya disponemos de esta categoría en memoria, respuesta instantánea en 0 ms
        if (cacheCategorias.containsKey(cat)) {
            Log.d(LOG_TAG, "ExplorarViewModel: Sirviendo categoría '" + cat + "' desde caché en memoria.");
            recetasCatalogo.setValue(cacheCategorias.get(cat));
            cargandoCatalogo.setValue(false);
            return;
        }

        if (!NetworkUtils.hayConexion(context)) {
            modoOffline.setValue(true);
            return;
        }

        cargandoCatalogo.setValue(true);
        modoOffline.setValue(false);

        Call<MealResponse> call;
        if (cat.equalsIgnoreCase("Todas")) {
            char letraAleatoria = LETRAS_DESTACADAS[new Random().nextInt(LETRAS_DESTACADAS.length)];
            Log.i(LOG_TAG, "ExplorarViewModel: Cargando selección destacada rotativa (Letra: " + letraAleatoria + ")...");
            call = RetrofitClient.getInstance().getApiService().buscarPorPrimeraLetra(String.valueOf(letraAleatoria));
        } else {
            call = RetrofitClient.getInstance().getApiService().filtrarPorCategoria(cat);
        }

        call.enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(@NonNull Call<MealResponse> call, @NonNull Response<MealResponse> response) {
                cargandoCatalogo.postValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().getMeals() != null) {
                    List<MealDto> lista = new ArrayList<>(response.body().getMeals());
                    if (cat.equalsIgnoreCase("Todas")) {
                        Collections.shuffle(lista);
                        if (lista.size() > 25) {
                            lista = new ArrayList<>(lista.subList(0, 25));
                        }
                        guardarCatalogoEnDisco(context, lista);
                    } else {
                        for (MealDto m : lista) {
                            if (m.getStrCategory() == null || m.getStrCategory().isEmpty()) {
                                m.setStrCategory(cat);
                            }
                        }
                    }
                    cacheCategorias.put(cat, lista);
                    Log.i(LOG_TAG, "ExplorarViewModel: Catálogo cargado con " + lista.size() + " recetas para: " + cat);
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

    private void guardarSugerenciaEnDisco(Context context, MealDto meal) {
        if (meal == null || context == null) return;
        try {
            SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_CACHE, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_SUGERIDA, gson.toJson(meal)).apply();
        } catch (Exception ignored) {}
    }

    private void guardarCatalogoEnDisco(Context context, List<MealDto> lista) {
        if (lista == null || context == null) return;
        try {
            SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_CACHE, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_CATALOGO_TODAS, gson.toJson(lista)).apply();
        } catch (Exception ignored) {}
    }
}
