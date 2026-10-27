package es.upm.miw.chefpocket.datos.api.modelos;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Contenedor de la respuesta de la API TheMealDB que envuelve la lista de recetas devueltas.
 */
public class MealResponse implements Serializable {

    @SerializedName("meals")
    private List<MealDto> meals;

    public MealResponse() {
        this.meals = new ArrayList<>();
    }

    public List<MealDto> getMeals() {
        return meals != null ? meals : new ArrayList<>();
    }

    public void setMeals(List<MealDto> meals) {
        this.meals = meals;
    }

    /**
     * Comprueba si la respuesta contiene al menos una receta.
     */
    public boolean tieneRecetas() {
        return meals != null && !meals.isEmpty();
    }

    /**
     * Retorna la primera receta de la lista (muy útil para endpoints como random.php).
     */
    public MealDto getPrimeraReceta() {
        return tieneRecetas() ? meals.get(0) : null;
    }
}
