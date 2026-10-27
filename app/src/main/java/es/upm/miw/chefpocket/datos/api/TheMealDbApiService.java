package es.upm.miw.chefpocket.datos.api;

import es.upm.miw.chefpocket.datos.api.modelos.MealResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interfaz de definición del servicio REST con Retrofit para la API TheMealDB.
 * Basado en las pautas oficiales del Tema 4 (Diapositivas 8 a 16).
 */
public interface TheMealDbApiService {

    /**
     * Obtiene una receta aleatoria ("Plato del día").
     */
    @GET("random.php")
    Call<MealResponse> obtenerRecetaAleatoria();

    /**
     * Busca recetas cuyo nombre o ingrediente contenga el término proporcionado.
     *
     * @param nombre Criterio de búsqueda (ej. "Arrabiata", "Chicken", "Pasta")
     */
    @GET("search.php")
    Call<MealResponse> buscarRecetasPorNombre(@Query("s") String nombre);

    /**
     * Recupera el detalle completo de una receta a partir de su ID único.
     *
     * @param idMeal Identificador numérico de la receta en TheMealDB
     */
    @GET("lookup.php")
    Call<MealResponse> obtenerRecetaPorId(@Query("i") String idMeal);

    /**
     * Filtra recetas por categoría gastronómica (ej. "Seafood", "Beef", "Dessert").
     *
     * @param categoria Nombre de la categoría
     */
    @GET("filter.php")
    Call<MealResponse> filtrarPorCategoria(@Query("c") String categoria);
}
