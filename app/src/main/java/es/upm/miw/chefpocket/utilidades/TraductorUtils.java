package es.upm.miw.chefpocket.utilidades;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import es.upm.miw.chefpocket.datos.api.modelos.MealDto;

/**
 * Utilidad de traducción y adaptación idiomática para las respuestas de la API TheMealDB.
 * Permite internacionalizar categorías y países según la configuración de idioma del dispositivo (Locale).
 */
public final class TraductorUtils {

    private static final Map<String, String> CATEGORIAS_ES = new HashMap<>();
    private static final Map<String, String> ORIGENES_ES = new HashMap<>();

    static {
        // Categorías
        CATEGORIAS_ES.put("beef", "Ternera");
        CATEGORIAS_ES.put("chicken", "Pollo");
        CATEGORIAS_ES.put("dessert", "Postres");
        CATEGORIAS_ES.put("lamb", "Cordero");
        CATEGORIAS_ES.put("miscellaneous", "Especialidades");
        CATEGORIAS_ES.put("pasta", "Pastas");
        CATEGORIAS_ES.put("pork", "Cerdo");
        CATEGORIAS_ES.put("seafood", "Pescados y Mariscos");
        CATEGORIAS_ES.put("side", "Guarniciones");
        CATEGORIAS_ES.put("starter", "Entrantes");
        CATEGORIAS_ES.put("vegan", "Vegano");
        CATEGORIAS_ES.put("vegetarian", "Vegetariano");
        CATEGORIAS_ES.put("breakfast", "Desayunos");
        CATEGORIAS_ES.put("goat", "Cabrito");

        // Orígenes / Cocinas
        ORIGENES_ES.put("american", "Cocina Americana");
        ORIGENES_ES.put("british", "Cocina Británica");
        ORIGENES_ES.put("canadian", "Cocina Canadiense");
        ORIGENES_ES.put("chinese", "Cocina China");
        ORIGENES_ES.put("croatian", "Cocina Croata");
        ORIGENES_ES.put("dutch", "Cocina Holandesa");
        ORIGENES_ES.put("egyptian", "Cocina Egipcia");
        ORIGENES_ES.put("filipino", "Cocina Filipina");
        ORIGENES_ES.put("french", "Cocina Francesa");
        ORIGENES_ES.put("greek", "Cocina Griega");
        ORIGENES_ES.put("indian", "Cocina India");
        ORIGENES_ES.put("irish", "Cocina Irlandesa");
        ORIGENES_ES.put("italian", "Cocina Italiana");
        ORIGENES_ES.put("jamaican", "Cocina Jamaicana");
        ORIGENES_ES.put("japanese", "Cocina Japonesa");
        ORIGENES_ES.put("kenyan", "Cocina Keniana");
        ORIGENES_ES.put("malaysian", "Cocina Malaya");
        ORIGENES_ES.put("mexican", "Cocina Mexicana");
        ORIGENES_ES.put("moroccan", "Cocina Marroquí");
        ORIGENES_ES.put("polish", "Cocina Polaca");
        ORIGENES_ES.put("portuguese", "Cocina Portuguesa");
        ORIGENES_ES.put("russian", "Cocina Rusa");
        ORIGENES_ES.put("spanish", "Cocina Española");
        ORIGENES_ES.put("thai", "Cocina Tailandesa");
        ORIGENES_ES.put("tunisian", "Cocina Tunecina");
        ORIGENES_ES.put("turkish", "Cocina Turca");
        ORIGENES_ES.put("ukrainian", "Cocina Ucraniana");
        ORIGENES_ES.put("vietnamese", "Cocina Vietnamita");
    }

    private TraductorUtils() {
        // Constructor privado para clase utilitaria
    }

    /**
     * Comprueba si el idioma preferido en el dispositivo es el español.
     */
    public static boolean esEspanol() {
        return Locale.getDefault().getLanguage().toLowerCase().startsWith("es");
    }

    /**
     * Traduce una categoría gastronómica al español si el dispositivo está en español;
     * de lo contrario, conserva la denominación en inglés.
     */
    public static String traducirCategoria(String categoria) {
        if (categoria == null || categoria.trim().isEmpty()) {
            return esEspanol() ? "Gourmet" : "Gourmet";
        }
        if (!esEspanol()) {
            return categoria;
        }
        String traduccion = CATEGORIAS_ES.get(categoria.trim().toLowerCase());
        return traduccion != null ? traduccion : categoria;
    }

    /**
     * Traduce un país o región gastronómica según el idioma activo en el dispositivo.
     */
    public static String traducirOrigen(String area) {
        if (area == null || area.trim().isEmpty()) {
            return esEspanol() ? "Internacional" : "International";
        }
        if (!esEspanol()) {
            return area + " Cuisine";
        }
        String traduccion = ORIGENES_ES.get(area.trim().toLowerCase());
        return traduccion != null ? traduccion : area;
    }

    /**
     * Obtiene el texto de instrucciones priorizando el idioma del usuario.
     */
    public static String obtenerInstrucciones(MealDto meal) {
        if (meal == null) return "";
        if (esEspanol() && meal.tieneTraduccionEspanol()) {
            return meal.getStrInstructionsES();
        }
        return meal.getStrInstructions();
    }

    /**
     * Determina si se debe mostrar una nota informativa aclarando que la receta
     * está en su versión original en inglés por falta de traducción en TheMealDB.
     */
    public static boolean requiereAvisoIdioma(MealDto meal) {
        return esEspanol() && meal != null && !meal.tieneTraduccionEspanol();
    }
}

