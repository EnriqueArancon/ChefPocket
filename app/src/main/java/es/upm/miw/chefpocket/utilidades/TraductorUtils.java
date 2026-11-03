package es.upm.miw.chefpocket.utilidades;

import android.os.Handler;
import android.os.Looper;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import es.upm.miw.chefpocket.datos.api.modelos.MealDto;

/**
 * Utilidad de traducción y adaptación idiomática para las respuestas de la API TheMealDB.
 * Permite internacionalizar categorías y países según la configuración de idioma del dispositivo (Locale).
 */
public final class TraductorUtils {

    private static final Map<String, String> CATEGORIAS_ES = new HashMap<>();
    private static final Map<String, String> ORIGENES_ES = new HashMap<>();
    private static final Map<String, String> INGREDIENTES_ES = new HashMap<>();
    private static final Map<String, String> RECETAS_DESTACADAS_ES = new HashMap<>();

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
        ORIGENES_ES.put("uruguayan", "Cocina Uruguaya");
        ORIGENES_ES.put("vietnamese", "Cocina Vietnamita");

        // Diccionario gastronómico O(1) de ingredientes más comunes
        INGREDIENTES_ES.put("flour", "Harina de trigo");
        INGREDIENTES_ES.put("plain flour", "Harina común");
        INGREDIENTES_ES.put("self-raising flour", "Harina con levadura");
        INGREDIENTES_ES.put("sugar", "Azúcar");
        INGREDIENTES_ES.put("caster sugar", "Azúcar glas");
        INGREDIENTES_ES.put("brown sugar", "Azúcar moreno");
        INGREDIENTES_ES.put("muscovado sugar", "Azúcar mascabado");
        INGREDIENTES_ES.put("milk", "Leche");
        INGREDIENTES_ES.put("whole milk", "Leche entera");
        INGREDIENTES_ES.put("vegetable milk", "Bebida vegetal");
        INGREDIENTES_ES.put("vegetable millk", "Bebida vegetal");
        INGREDIENTES_ES.put("dessert rice", "Arroz para postres");
        INGREDIENTES_ES.put("egg", "Huevo");
        INGREDIENTES_ES.put("eggs", "Huevos");
        INGREDIENTES_ES.put("egg yolks", "Yemas de huevo");
        INGREDIENTES_ES.put("egg whites", "Claras de huevo");
        INGREDIENTES_ES.put("butter", "Mantequilla");
        INGREDIENTES_ES.put("unsalted butter", "Mantequilla sin sal");
        INGREDIENTES_ES.put("olive oil", "Aceite de oliva");
        INGREDIENTES_ES.put("vegetable oil", "Aceite vegetal");
        INGREDIENTES_ES.put("coconut oil", "Aceite de coco");
        INGREDIENTES_ES.put("salt", "Sal");
        INGREDIENTES_ES.put("water", "Agua");
        INGREDIENTES_ES.put("garlic", "Ajo");
        INGREDIENTES_ES.put("garlic cloves", "Dientes de ajo");
        INGREDIENTES_ES.put("onion", "Cebolla");
        INGREDIENTES_ES.put("onions", "Cebollas");
        INGREDIENTES_ES.put("red onion", "Cebolla morada");
        INGREDIENTES_ES.put("spring onions", "Cebolletas");
        INGREDIENTES_ES.put("chicken", "Pollo");
        INGREDIENTES_ES.put("chicken breast", "Pechuga de pollo");
        INGREDIENTES_ES.put("chicken breasts", "Pechugas de pollo");
        INGREDIENTES_ES.put("chicken stock", "Caldo de pollo");
        INGREDIENTES_ES.put("beef", "Carne de ternera");
        INGREDIENTES_ES.put("minced beef", "Carne picada de ternera");
        INGREDIENTES_ES.put("ground beef", "Carne picada de vacuno");
        INGREDIENTES_ES.put("cornstarch", "Almidón de maíz / Maicena");
        INGREDIENTES_ES.put("nutmeg", "Nuez moscada");
        INGREDIENTES_ES.put("beef stock", "Caldo de carne");
        INGREDIENTES_ES.put("pork", "Carne de cerdo");
        INGREDIENTES_ES.put("bacon", "Panceta / Beicon");
        INGREDIENTES_ES.put("ham", "Jamón");
        INGREDIENTES_ES.put("rice", "Arroz");
        INGREDIENTES_ES.put("basmati rice", "Arroz basmati");
        INGREDIENTES_ES.put("tomato", "Tomate");
        INGREDIENTES_ES.put("tomatoes", "Tomates");
        INGREDIENTES_ES.put("tomato puree", "Puré de tomate");
        INGREDIENTES_ES.put("chopped tomatoes", "Tomate triturado");
        INGREDIENTES_ES.put("potato", "Patata");
        INGREDIENTES_ES.put("potatoes", "Patatas");
        INGREDIENTES_ES.put("cheese", "Queso");
        INGREDIENTES_ES.put("cheddar cheese", "Queso cheddar");
        INGREDIENTES_ES.put("parmesan", "Queso parmesano");
        INGREDIENTES_ES.put("mozzarella", "Queso mozzarella");
        INGREDIENTES_ES.put("cream", "Nata / Crema");
        INGREDIENTES_ES.put("single cream", "Nata líquida");
        INGREDIENTES_ES.put("heavy cream", "Nata para montar");
        INGREDIENTES_ES.put("sour cream", "Crema agria");
        INGREDIENTES_ES.put("lemon", "Limón");
        INGREDIENTES_ES.put("lemon juice", "Zumo de limón");
        INGREDIENTES_ES.put("lime", "Lima");
        INGREDIENTES_ES.put("pepper", "Pimienta");
        INGREDIENTES_ES.put("black pepper", "Pimienta negra");
        INGREDIENTES_ES.put("green pepper", "Pimiento verde");
        INGREDIENTES_ES.put("red pepper", "Pimiento rojo");
        INGREDIENTES_ES.put("yeast", "Levadura");
        INGREDIENTES_ES.put("baking powder", "Polvo de hornear / Levadura");
        INGREDIENTES_ES.put("vanilla", "Vainilla");
        INGREDIENTES_ES.put("vanilla extract", "Extracto de vainilla");
        INGREDIENTES_ES.put("vanilla pod", "Vaina de vainilla");
        INGREDIENTES_ES.put("cardamom", "Cardamomo");
        INGREDIENTES_ES.put("star anise", "Anís estrellado");
        INGREDIENTES_ES.put("cinnamon", "Canela molida");
        INGREDIENTES_ES.put("cinnamon stick", "Rama de canela");
        INGREDIENTES_ES.put("paprika", "Pimentón");
        INGREDIENTES_ES.put("oregano", "Orégano");
        INGREDIENTES_ES.put("basil", "Albahaca fresca");
        INGREDIENTES_ES.put("parsley", "Perejil fresco");
        INGREDIENTES_ES.put("coriander", "Cilantro");
        INGREDIENTES_ES.put("carrots", "Zanahorias");
        INGREDIENTES_ES.put("carrot", "Zanahoria");
        INGREDIENTES_ES.put("celery", "Apio");
        INGREDIENTES_ES.put("mushrooms", "Champiñones");
        INGREDIENTES_ES.put("spinach", "Espinacas");
        INGREDIENTES_ES.put("peas", "Guisantes");
        INGREDIENTES_ES.put("pasta", "Pasta");
        INGREDIENTES_ES.put("spaghetti", "Espaguetis");
        INGREDIENTES_ES.put("honey", "Miel");
        INGREDIENTES_ES.put("mustard", "Mostaza");
        INGREDIENTES_ES.put("mayonnaise", "Mayonesa");
        INGREDIENTES_ES.put("soy sauce", "Salsa de soja");
        INGREDIENTES_ES.put("vinegar", "Vinagre");
        INGREDIENTES_ES.put("breadcrumbs", "Pan rallado");
        INGREDIENTES_ES.put("bread", "Pan de molde / Pan");
        INGREDIENTES_ES.put("strawberries", "Fresas");
        INGREDIENTES_ES.put("raspberries", "Frambuesas");
        INGREDIENTES_ES.put("chocolate", "Chocolate");
        INGREDIENTES_ES.put("cocoa", "Cacao en polvo");
        INGREDIENTES_ES.put("dulce de leche", "Dulce de leche");
        INGREDIENTES_ES.put("walnuts", "Nueces");
        INGREDIENTES_ES.put("raisins", "Uvas pasas");
        INGREDIENTES_ES.put("almonds", "Almendras");
        INGREDIENTES_ES.put("shortcrust pastry", "Masa quebrada");
        INGREDIENTES_ES.put("turmeric", "Cúrcuma");
        INGREDIENTES_ES.put("sweetcorn", "Maíz dulce");
        INGREDIENTES_ES.put("kabse spice", "Especias Kabsa");
        INGREDIENTES_ES.put("maggi cube", "Pastilla de caldo (Maggi)");
        INGREDIENTES_ES.put("squid", "Calamares");
        INGREDIENTES_ES.put("calamari", "Calamares");
        INGREDIENTES_ES.put("capers", "Alcaparras");
        INGREDIENTES_ES.put("flour", "Harina");
        INGREDIENTES_ES.put("plain flour", "Harina común / de trigo");
        INGREDIENTES_ES.put("all-purpose flour", "Harina de trigo");
        INGREDIENTES_ES.put("romano pepper", "Pimiento romano");
        INGREDIENTES_ES.put("green chilli", "Chile verde");
        INGREDIENTES_ES.put("red chilli", "Chile rojo");
        INGREDIENTES_ES.put("chilli", "Chile / Guindilla");
        INGREDIENTES_ES.put("chillies", "Chiles / Guindillas");
        INGREDIENTES_ES.put("red pepper paste", "Pasta de pimiento rojo");
        INGREDIENTES_ES.put("pomegranate molasses", "Melaza de granada");
        INGREDIENTES_ES.put("pul biber", "Copos de chile (Pul Biber)");
        INGREDIENTES_ES.put("sumac", "Zumaque");
        INGREDIENTES_ES.put("dried mint", "Menta seca");
        INGREDIENTES_ES.put("mint", "Menta fresca");
        INGREDIENTES_ES.put("extra virgin olive oil", "Aceite de oliva virgen extra");
        INGREDIENTES_ES.put("cucumber", "Pepino");
        INGREDIENTES_ES.put("yogurt", "Yogur");
        INGREDIENTES_ES.put("greek yogurt", "Yogur griego");
        INGREDIENTES_ES.put("ginger", "Jengibre");
        INGREDIENTES_ES.put("fresh ginger", "Jengibre fresco");
        INGREDIENTES_ES.put("peanut butter", "Mantequilla de cacahuete");
        INGREDIENTES_ES.put("peanuts", "Cacahuetes");
        INGREDIENTES_ES.put("sesame seeds", "Semillas de sésamo");
        INGREDIENTES_ES.put("sesame oil", "Aceite de sésamo");
        INGREDIENTES_ES.put("shallots", "Chalotas");
        INGREDIENTES_ES.put("shallot", "Chalota");
        INGREDIENTES_ES.put("coconut milk", "Leche de coco");
        INGREDIENTES_ES.put("cumin", "Comino");
        INGREDIENTES_ES.put("curry powder", "Curry en polvo");
        INGREDIENTES_ES.put("coriander", "Cilantro");
        INGREDIENTES_ES.put("cilantro", "Cilantro");
        INGREDIENTES_ES.put("kidney beans", "Alubias rojas");
        INGREDIENTES_ES.put("garam masala", "Garam Masala");
        INGREDIENTES_ES.put("sticky rice flour", "Harina de arroz glutinoso");
        INGREDIENTES_ES.put("rice flour", "Harina de arroz");
        INGREDIENTES_ES.put("pandan leaves", "Hojas de pandán");
        INGREDIENTES_ES.put("pandan leaf", "Hoja de pandán");
        INGREDIENTES_ES.put("beetroot", "Remolacha");
        INGREDIENTES_ES.put("pumpkin", "Calabaza");
        INGREDIENTES_ES.put("taro", "Taro");
        INGREDIENTES_ES.put("longan", "Longan");
        INGREDIENTES_ES.put("coconut", "Coco rallado");
        INGREDIENTES_ES.put("coconut cream", "Crema de coco");
        INGREDIENTES_ES.put("white cabbage", "Repollo blanco");
        INGREDIENTES_ES.put("red cabbage", "Lombarda / Col morada");
        INGREDIENTES_ES.put("cabbage", "Repollo / Col");
        INGREDIENTES_ES.put("hispi cabbage", "Col puntiaguda");
        INGREDIENTES_ES.put("sweetheart cabbage", "Col dulce");
        INGREDIENTES_ES.put("sauerkraut", "Chucrut");
        INGREDIENTES_ES.put("lard", "Manteca de cerdo");
        INGREDIENTES_ES.put("german sausages", "Salchichas alemanas");
        INGREDIENTES_ES.put("sausages", "Salchichas");
        INGREDIENTES_ES.put("sausage", "Salchicha");
        INGREDIENTES_ES.put("bay leaf", "Hoja de laurel");
        INGREDIENTES_ES.put("bay leaves", "Hojas de laurel");
        INGREDIENTES_ES.put("cloves", "Clavos de olor");
        INGREDIENTES_ES.put("clove", "Clavo de olor");
        INGREDIENTES_ES.put("peppercorns", "Granos de pimienta");
        INGREDIENTES_ES.put("juniper berries", "Bayas de enebro");
        INGREDIENTES_ES.put("allspice berries", "Pimienta de Jamaica");
        INGREDIENTES_ES.put("allspice", "Pimienta de Jamaica");
        INGREDIENTES_ES.put("prunes", "Ciruelas pasas");
        INGREDIENTES_ES.put("red wine", "Vino tinto");
        INGREDIENTES_ES.put("white wine", "Vino blanco");
        INGREDIENTES_ES.put("cannellini beans", "Alubias blancas (Cannellini)");
        INGREDIENTES_ES.put("beans", "Alubias / Judías");
        INGREDIENTES_ES.put("chorizo", "Chorizo");
        INGREDIENTES_ES.put("tarragon leaves", "Hojas de estragón");
        INGREDIENTES_ES.put("tarragon", "Estragón");
        INGREDIENTES_ES.put("chives", "Cebollino");
        INGREDIENTES_ES.put("gruyere", "Queso Gruyère");
        INGREDIENTES_ES.put("gruyère", "Queso Gruyère");
        INGREDIENTES_ES.put("sunflower oil", "Aceite de girasol");
        INGREDIENTES_ES.put("sweet potato", "Batata / Boniato");
        INGREDIENTES_ES.put("aubergine", "Berenjena");
        INGREDIENTES_ES.put("eggplant", "Berenjena");
        INGREDIENTES_ES.put("zucchini", "Calabacín");
        INGREDIENTES_ES.put("courgette", "Calabacín");
        INGREDIENTES_ES.put("corn flour", "Harina de maíz");
        INGREDIENTES_ES.put("thyme", "Tomillo");
        INGREDIENTES_ES.put("rosemary", "Romero");
        INGREDIENTES_ES.put("salmon", "Salmón");
        INGREDIENTES_ES.put("tuna", "Atún");
        INGREDIENTES_ES.put("cod", "Bacalao");
        INGREDIENTES_ES.put("prawns", "Gambas / Langostinos");
        INGREDIENTES_ES.put("shrimp", "Camarones / Gambas");
        INGREDIENTES_ES.put("mussels", "Mejillones");
        INGREDIENTES_ES.put("lamb", "Carne de cordero");
        INGREDIENTES_ES.put("minced lamb", "Carne picada de cordero");
        INGREDIENTES_ES.put("lentils", "Lentejas");
        INGREDIENTES_ES.put("chickpeas", "Garbanzos");
        INGREDIENTES_ES.put("green beans", "Judías verdes");
        INGREDIENTES_ES.put("lettuce", "Lechuga");
        INGREDIENTES_ES.put("ground mahleb", "Mahleb molido");
        INGREDIENTES_ES.put("mahleb", "Mahleb");
        INGREDIENTES_ES.put("black treacle", "Melaza negra");
        INGREDIENTES_ES.put("treacle", "Melaza");
        INGREDIENTES_ES.put("butter beans", "Alubias blancas / Frijoles de mantequilla");
        INGREDIENTES_ES.put("sev dzandig", "Semillas negras de nigella");
        INGREDIENTES_ES.put("nigella seeds", "Semillas de nigella");
        INGREDIENTES_ES.put("sesame soy sauce", "Salsa de soja y sésamo");
        INGREDIENTES_ES.put("english mustard", "Mostaza inglesa");

        // Recetas destacadas con traducción al español integrada
        RECETAS_DESTACADAS_ES.put("flan",
                "1. Para el caramelo: coloca 100 g de azúcar en una sartén a fuego medio sin grasa y deja que se funda suavemente hasta obtener un tono ámbar dorado.\n" +
                "2. Vierte el caramelo con cuidado en el fondo de los moldes individuales.\n" +
                "3. Precalienta el horno a 150°C y prepara una bandeja honda para baño maría.\n" +
                "4. En un bol grande, bate los huevos, las yemas y el azúcar restante hasta que se integren.\n" +
                "5. Calienta la leche con la vaina de vainilla abierta y viértela templada sobre la mezcla de huevos sin dejar de remover suavemente.\n" +
                "6. Pasa la mezcla por un colador fino y llena los moldes caramelizados.\n" +
                "7. Hornea al baño maría durante unos 45-50 minutos hasta que el flan esté firme pero con ligero temblor central.\n" +
                "8. Deja enfriar a temperatura ambiente y refrigera un mínimo de 4 horas antes de desmoldar acompañado con dulce de leche.");

        RECETAS_DESTACADAS_ES.put("summer pudding",
                "1. Lava suavemente los frutos rojos (frambuesas y fresas) y sécalos con papel absorbente.\n" +
                "2. Coloca el azúcar y 3 cucharadas de agua en una cazuela grande a fuego bajo hasta disolver el azúcar por completo.\n" +
                "3. Añade los frutos rojos y cocina a fuego suave durante 3 minutos hasta que desprendan su jugo rojizo.\n" +
                "4. Cuela la fruta reservando todo el jugo concentrado en un bol aparte.\n" +
                "5. Forra un molde hondo con film transparente y corta las cortezas del pan.\n" +
                "6. Moja las rebanadas de pan brevemente en el jugo de frutas y cubre el fondo y los laterales del molde sin dejar huecos.\n" +
                "7. Rellena el interior con la fruta escurrida y sella la parte superior con más rebanadas de pan mojadas.\n" +
                "8. Coloca un plato pequeño encima con un peso y refrigera durante toda la noche.\n" +
                "9. Desmolda con cuidado, baña con el jugo sobrante y sirve bien frío.");

        RECETAS_DESTACADAS_ES.put("canadian butter tarts",
                "1. Precalienta el horno a 190°C con calor arriba y abajo.\n" +
                "2. Estira la masa quebrada sobre una superficie ligeramente enharinada hasta que tenga unos 3 mm de grosor.\n" +
                "3. Corta discos de masa con un cortapastas de unos 8 cm y forra con ellos los moldes individuales de tartaleta previamente engrasados.\n" +
                "4. En un bol grande, bate los huevos junto con el azúcar mascabado, la mantequilla derretida, la nata líquida y el extracto de vainilla.\n" +
                "5. Añade las pasas y las nueces picadas a la mezcla.\n" +
                "6. Reparte el relleno en las tartaletas sin llegar al borde superior.\n" +
                "7. Hornea durante unos 18 a 22 minutos hasta que la masa esté dorada y el relleno caramelizado y burbujeante.\n" +
                "8. Deja enfriar en el molde antes de desmoldar para disfrutar de su textura cremosa.");

        RECETAS_DESTACADAS_ES.put("spanish tortilla",
                "1. Pela y corta las patatas en láminas finas y pica la cebolla.\n" +
                "2. Calienta abundante aceite de oliva virgen extra en una sartén grande a fuego medio.\n" +
                "3. Añade las patatas y la cebolla, cocinándolas a fuego lento hasta que queden tiernas y ligeramente doradas.\n" +
                "4. Bate los huevos en un bol grande con una pizca generosa de sal.\n" +
                "5. Escurre las patatas y la cebolla del exceso de aceite e incorpóralas al bol de los huevos batidos, dejando reposar 10 minutos.\n" +
                "6. Calienta una sartén antiadherente con unas gotas de aceite, vierte la mezcla y cocina a fuego medio durante 3-4 minutos.\n" +
                "7. Da la vuelta a la tortilla con ayuda de un plato grande y cocina por el otro lado 2 minutos más.\n" +
                "8. Sirve templada o fría.");

        RECETAS_DESTACADAS_ES.put("pancakes",
                "1. En un bol amplio, tamiza la harina junto con el azúcar, el polvo de hornear y una pizca de sal.\n" +
                "2. En otro recipiente, bate la leche con los huevos y la mantequilla derretida.\n" +
                "3. Vierte los ingredientes líquidos sobre los secos y mezcla suavemente con unas varillas solo hasta integrar.\n" +
                "4. Calienta una sartén antiadherente a fuego medio y engrásala con una nuez de mantequilla.\n" +
                "5. Vierte un cucharón de masa para cada tortita.\n" +
                "6. Cuando aparezcan burbujas en la superficie (unos 2 minutos), dale la vuelta con una espátula.\n" +
                "7. Cocina durante 1 o 2 minutos más por el otro lado hasta que esté dorada.\n" +
                "8. Sirve calientes acompañadas de sirope, miel o fruta fresca.");

        RECETAS_DESTACADAS_ES.put("kabse",
                "1. Dora el pollo troceado en una cazuela grande con un buen chorro de aceite de oliva, añade la pastilla de caldo y cocina a fuego lento durante unos 30 minutos.\n" +
                "2. En otra cazuela amplia, saltea las verduras (champiñones, maíz dulce y zanahorias) a fuego medio hasta que queden tiernas y aromáticas.\n" +
                "3. Cuando la verdura esté pochada, añade el arroz, las uvas pasas y las especias Kabsa. Vierte dos tazas y media de agua o caldo caliente para que el arroz cocine despacio.\n" +
                "4. Cuando el arroz haya absorbido casi todo el caldo, coloca el pollo por encima e introduce todo en el horno a 180°C durante 15 minutos para que se dore antes de servir.");
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
     * Traduce el nombre de un ingrediente al español con búsqueda O(1).
     * Si no se encuentra o el dispositivo está en inglés, devuelve el nombre original.
     */
    public static String traducirIngrediente(String ingrediente) {
        if (ingrediente == null || ingrediente.trim().isEmpty() || !esEspanol()) {
            return ingrediente != null ? ingrediente.trim() : "";
        }
        String clave = ingrediente.trim().toLowerCase();
        String traduccion = INGREDIENTES_ES.get(clave);
        if (traduccion != null) {
            return traduccion;
        }
        if (CACHE_TRADUCCIONES.containsKey(clave)) {
            return CACHE_TRADUCCIONES.get(clave);
        }
        return ingrediente.trim();
    }

    /**
     * Traduce unidades de medida y descriptores culinarios de inglés a español,
     * distinguiendo con precisión gramatical entre singular, plural y técnicas de cocina.
     */
    public static String traducirMedida(String medida) {
        if (medida == null || medida.trim().isEmpty() || !esEspanol()) {
            return medida != null ? medida.trim() : "";
        }
        String m = medida.trim();

        // Normalizar fracciones tipográficas comunes
        m = m.replace("½", "1/2").replace("¼", "1/4").replace("¾", "3/4");

        // Si la medida es un número entero aislado (ej. "1", "2"), representa unidades contables ("1 ud.", "2 uds.")
        if (m.equals("1")) {
            return "1 ud.";
        }
        if (m.matches("^\\d+$")) {
            return m + " uds.";
        }
        if (m.equals("1/2")) {
            return "1/2 ud.";
        }

        // Finalidades de preparación
        m = m.replaceAll("(?i)\\bfor frying\\b", "para freír");
        m = m.replaceAll("(?i)\\b(?:for serving|to serve)\\b", "para servir");
        m = m.replaceAll("(?i)\\b(?:to garnish|for garnish|garnish)\\b", "para decorar");
        m = m.replaceAll("(?i)\\bto taste\\b", "al gusto");
        m = m.replaceAll("(?i)\\b(?:as needed|as required)\\b", "según necesidad");
        m = m.replaceAll("(?i)\\boptional\\b", "opcional");
        m = m.replaceAll("(?i)\\b(?:to dust|dusting)\\b", "para espolvorear");
        m = m.replaceAll("(?i)\\bcoating\\b", "para rebozar");

        // Técnicas y estados culinarios
        m = m.replaceAll("(?i)\\bpeeled (?:and )?crushed\\b", "pelado y machacado");
        m = m.replaceAll("(?i)\\bpeeled\\b", "pelado");
        m = m.replaceAll("(?i)\\bcrushed\\b", "machacado");
        m = m.replaceAll("(?i)\\bfinely chopped\\b", "finamente picado");
        m = m.replaceAll("(?i)\\b(?:roughly|coarsely) chopped\\b", "picado grueso");
        m = m.replaceAll("(?i)\\b(?:minced|chopped)\\b", "picado");
        m = m.replaceAll("(?i)\\bdiced\\b", "en dados");
        m = m.replaceAll("(?i)\\bgrated\\b", "rallado");
        m = m.replaceAll("(?i)\\bshredded\\b", "desmenuzado");
        m = m.replaceAll("(?i)\\bmelted\\b", "derretido");
        m = m.replaceAll("(?i)\\bbeaten\\b", "batido");
        m = m.replaceAll("(?i)\\bdrained\\b", "escurrido");
        m = m.replaceAll("(?i)\\bthinly sliced\\b", "cortado fino");
        m = m.replaceAll("(?i)\\bsliced\\b", "en rodajas");
        m = m.replaceAll("(?i)\\bhalved\\b", "por la mitad");
        m = m.replaceAll("(?i)\\bquartered\\b", "en cuartos");
        m = m.replaceAll("(?i)\\bground\\b", "molido");
        m = m.replaceAll("(?i)\\broasted\\b", "tostado");
        m = m.replaceAll("(?i)\\bcooked\\b", "cocinado");
        m = m.replaceAll("(?i)\\bboiled\\b", "hervido");
        m = m.replaceAll("(?i)\\bwarm\\b", "tibio");
        m = m.replaceAll("(?i)\\bcold\\b", "frío");
        m = m.replaceAll("(?i)\\bhot\\b", "caliente");
        m = m.replaceAll("(?i)\\bfresh\\b", "fresco");
        m = m.replaceAll("(?i)\\bdried\\b", "seco");
        m = m.replaceAll("(?i)\\bleaves\\b", "hojas");

        // Tamaños (singular y plural)
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*medium\\b", "$1 mediano");
        m = m.replaceAll("(?i)\\b(\\d+)\\s*medium\\b", "$1 medianos");
        m = m.replaceAll("(?i)\\bmedium\\b", "mediano");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*large\\b", "$1 grande");
        m = m.replaceAll("(?i)\\b(\\d+)\\s*large\\b", "$1 grandes");
        m = m.replaceAll("(?i)\\blarge\\b", "grande");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*small\\b", "$1 pequeño");
        m = m.replaceAll("(?i)\\b(\\d+)\\s*small\\b", "$1 pequeños");
        m = m.replaceAll("(?i)\\bsmall\\b", "pequeño");

        // Unidades de medida culinarias (singulares con 1 o fracciones)
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*(?:tbsps?|tbls?p?s?|tablespoons?)\\b", "$1 cucharada");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*(?:tsps?|teaspoons?)\\b", "$1 cucharadita");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*cups?\\b", "$1 taza");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*cloves?\\b", "$1 diente");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*slices?\\b", "$1 rebanada");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*pinch(?:es)?\\b", "$1 pizca");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*handfuls?\\b", "$1 puñado");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*bunch(?:es)?\\b", "$1 manojo");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*sprigs?\\b", "$1 ramita");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*stalks?\\b", "$1 tallo");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*(?:cans?|tins?)\\b", "$1 lata");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*jars?\\b", "$1 tarro");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*(?:pack|packet|package)s?\\b", "$1 paquete");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*parts?\\b", "$1 parte");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*sheets?\\b", "$1 lámina");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*pieces?\\b", "$1 ud.");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*bottles?\\b", "$1 botella");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*glasses?\\b", "$1 vaso");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*sticks?\\b", "$1 rama");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*fillets?\\b", "$1 filete");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*heads?\\b", "$1 cabeza");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*drops?\\b", "$1 gota");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*cubes?\\b", "$1 pastilla");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*(?:fl(?:uid)?\\s*oz|fluid\\s+ounces?)\\b", "$1 oz líq.");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*(?:ounces?|oz)\\b", "$1 onza");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*(?:pounds?|lbs?)\\b", "$1 libra");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*(?:pints?|pt)\\b", "$1 pinta");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*(?:quarts?|qt)\\b", "$1 cuarto de galón");
        m = m.replaceAll("(?i)\\b(1|1/2|1/4)\\s*(?:gallons?|gal)\\b", "$1 galón");

        // Plurales o unidades sin número delante
        m = m.replaceAll("(?i)\\b(?:tbsps?|tbls?p?s?|tablespoons?)\\b", "cucharadas");
        m = m.replaceAll("(?i)\\b(?:tsps?|teaspoons?)\\b", "cucharaditas");
        m = m.replaceAll("(?i)\\bcups?\\b", "tazas");
        m = m.replaceAll("(?i)\\bcloves?\\b", "dientes");
        m = m.replaceAll("(?i)\\bslices?\\b", "rebanadas");
        m = m.replaceAll("(?i)\\bpinch(?:es)?\\b", "pizca");
        m = m.replaceAll("(?i)\\bhandfuls?\\b", "puñados");
        m = m.replaceAll("(?i)\\bbunch(?:es)?\\b", "manojos");
        m = m.replaceAll("(?i)\\bsprigs?\\b", "ramitas");
        m = m.replaceAll("(?i)\\bstalks?\\b", "tallos");
        m = m.replaceAll("(?i)\\b(?:cans?|tins?)\\b", "latas");
        m = m.replaceAll("(?i)\\bjars?\\b", "tarros");
        m = m.replaceAll("(?i)\\b(?:pack|packet|package)s?\\b", "paquetes");
        m = m.replaceAll("(?i)\\bparts?\\b", "partes");
        m = m.replaceAll("(?i)\\bsheets?\\b", "láminas");
        m = m.replaceAll("(?i)\\bpieces?\\b", "uds.");
        m = m.replaceAll("(?i)\\bbottles?\\b", "botellas");
        m = m.replaceAll("(?i)\\bglasses?\\b", "vasos");
        m = m.replaceAll("(?i)\\bsticks?\\b", "ramas");
        m = m.replaceAll("(?i)\\bfillets?\\b", "filetes");
        m = m.replaceAll("(?i)\\bheads?\\b", "cabezas");
        m = m.replaceAll("(?i)\\bdrops?\\b", "gotas");
        m = m.replaceAll("(?i)\\bcubes?\\b", "pastillas");
        m = m.replaceAll("(?i)\\b(?:drizzle|splash|dash)(?:es)?\\b", "chorrito");
        m = m.replaceAll("(?i)\\b(?:fl(?:uid)?\\s*oz|fluid\\s+ounces?)\\b", "oz líq.");
        m = m.replaceAll("(?i)\\b(?:ounces?|oz)\\b", "onzas");
        m = m.replaceAll("(?i)\\b(?:pounds?|lbs?)\\b", "libras");
        m = m.replaceAll("(?i)\\b(?:pints?|pt)\\b", "pintas");
        m = m.replaceAll("(?i)\\b(?:quarts?|qt)\\b", "cuartos de galón");
        m = m.replaceAll("(?i)\\b(?:gallons?|gal)\\b", "galones");
        m = m.replaceAll("(?i)\\bgrams?\\b", "g");
        m = m.replaceAll("(?i)\\bkilograms?\\b", "kg");
        m = m.replaceAll("(?i)\\bmillilit(?:ers?|res?)\\b", "ml");
        m = m.replaceAll("(?i)\\blit(?:ers?|res?)\\b", "l");

        // Ajustes gramaticales adicionales de concordancia
        m = m.replaceAll("(?i)\\b(\\d+\\s+dientes)\\s+picado\\b", "$1 picados");
        m = m.replaceAll("(?i)\\bsprigs?\\b", "ramitas");
        m = m.replaceAll("(?i)\\bstalks?\\b", "tallos");
        m = m.replaceAll("(?i)\\b(?:cans?|tins?)\\b", "latas");
        m = m.replaceAll("(?i)\\bjars?\\b", "tarros");
        m = m.replaceAll("(?i)\\b(?:pack|package)s?\\b", "paquetes");
        m = m.replaceAll("(?i)\\bsheets?\\b", "láminas");
        m = m.replaceAll("(?i)\\bpieces?\\b", "uds.");
        m = m.replaceAll("(?i)\\bbottles?\\b", "botellas");
        m = m.replaceAll("(?i)\\bglasses?\\b", "vasos");
        m = m.replaceAll("(?i)\\bsticks?\\b", "ramas");
        m = m.replaceAll("(?i)\\bfillets?\\b", "filetes");
        m = m.replaceAll("(?i)\\bheads?\\b", "cabezas");
        m = m.replaceAll("(?i)\\bdrops?\\b", "gotas");
        m = m.replaceAll("(?i)\\bcubes?\\b", "pastillas");
        m = m.replaceAll("(?i)\\b(?:drizzle|splash|dash)(?:es)?\\b", "chorrito");

        return m;
    }

    /**
     * Interfaz de callback para recepción asíncrona de traducciones en el hilo principal de UI.
     */
    public interface TraduccionCallback {
        void onTraduccion(String textoTraducido, boolean fueExitosa);
    }

    private static final Map<String, String> CACHE_TRADUCCIONES = new ConcurrentHashMap<>();
    private static SharedPreferences prefsTraducciones = null;

    /**
     * Inicializa la persistencia en disco de la caché de traducciones gastronómicas.
     * Carga en O(1) todas las traducciones previamente almacenadas para evitar peticiones repetidas a internet.
     */
    public static void inicializarCacheDisco(Context context) {
        if (prefsTraducciones != null || context == null) return;
        try {
            prefsTraducciones = context.getApplicationContext().getSharedPreferences("chefpocket_traducciones_cache", Context.MODE_PRIVATE);
            Map<String, ?> todas = prefsTraducciones.getAll();
            for (Map.Entry<String, ?> entry : todas.entrySet()) {
                if (entry.getValue() instanceof String) {
                    CACHE_TRADUCCIONES.put(entry.getKey(), (String) entry.getValue());
                }
            }
            Log.d("TraductorUtils", "Caché de traducciones cargada desde disco: " + CACHE_TRADUCCIONES.size() + " entradas.");
        } catch (Exception e) {
            Log.w("TraductorUtils", "Error al cargar caché de traducciones de disco: " + e.getMessage());
        }
    }

    private static void persistirEnDisco(String clave, String valor) {
        if (prefsTraducciones != null && clave != null && valor != null) {
            prefsTraducciones.edit().putString(clave, valor).apply();
        }
    }

    public static boolean tieneTraduccionEnCache(String texto) {
        if (texto == null) return false;
        return CACHE_TRADUCCIONES.containsKey(texto);
    }
    private static final ExecutorService EXECUTOR_TRADUCTOR = Executors.newCachedThreadPool();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    /**
     * Obtiene el texto de instrucciones disponible de forma inmediata (síncrona).
     * Devuelve la versión en español si viene en la API, la traducción destacada o
     * el resultado previo en caché; en su defecto devuelve el texto original en inglés.
     */
    public static String obtenerInstrucciones(MealDto meal) {
        if (meal == null) return "";
        if (esEspanol()) {
            if (meal.tieneTraduccionEspanol()) {
                return meal.getStrInstructionsES();
            }
            if (meal.getStrMeal() != null) {
                String destacada = RECETAS_DESTACADAS_ES.get(meal.getStrMeal().trim().toLowerCase());
                if (destacada != null) {
                    return destacada;
                }
            }
            if (meal.getStrInstructions() != null && CACHE_TRADUCCIONES.containsKey(meal.getStrInstructions())) {
                return CACHE_TRADUCCIONES.get(meal.getStrInstructions());
            }
        }
        return meal.getStrInstructions();
    }

    /**
     * Determina si una receta ya dispone de traducción completa al español
     * (sea oficial en TheMealDB, incluida en el catálogo destacado o presente en caché).
     */
    public static boolean tieneTraduccionCompleta(MealDto meal) {
        if (meal == null) return false;
        if (meal.tieneTraduccionEspanol()) return true;
        if (meal.getStrMeal() != null && RECETAS_DESTACADAS_ES.containsKey(meal.getStrMeal().trim().toLowerCase())) {
            return true;
        }
        if (meal.getStrInstructions() != null && CACHE_TRADUCCIONES.containsKey(meal.getStrInstructions())) {
            return true;
        }
        return false;
    }

    /**
     * Traduce de forma asíncrona en segundo plano el texto de instrucciones al español
     * utilizando servicios online de traducción (Google Translate y fallback a MyMemory), sin claves ni bloqueos.
     * Incorpora caché en memoria para que accesos repetidos a la misma receta respondan en 0 ms.
     * Si el dispositivo está en inglés, cortocircuita y devuelve el texto original al momento.
     * En caso de ausencia de conexión o fallo de red, efectúa un fallback transparente al texto original.
     */
    public static void traducirInstruccionesAsync(String textoIngles, TraduccionCallback callback) {
        if (textoIngles == null || textoIngles.trim().isEmpty() || !esEspanol()) {
            callback.onTraduccion(textoIngles, true);
            return;
        }

        // Si ya está en la caché en memoria, responder al instante en el hilo principal
        if (CACHE_TRADUCCIONES.containsKey(textoIngles)) {
            callback.onTraduccion(CACHE_TRADUCCIONES.get(textoIngles), true);
            return;
        }

        EXECUTOR_TRADUCTOR.execute(() -> {
            String resultado = traducirConGoogle(textoIngles);
            if (resultado == null || resultado.isEmpty()) {
                resultado = traducirConMyMemory(textoIngles);
            }

            if (resultado != null && !resultado.trim().isEmpty()) {
                CACHE_TRADUCCIONES.put(textoIngles, resultado);
                persistirEnDisco(textoIngles, resultado);
                final String finalResultado = resultado;
                MAIN_HANDLER.post(() -> callback.onTraduccion(finalResultado, true));
            } else {
                Log.w("TraductorUtils", "Traducción online no disponible. Mostrando instrucciones originales en inglés.");
                MAIN_HANDLER.post(() -> callback.onTraduccion(textoIngles, false));
            }
        });
    }

    private static String traducirConGoogle(String textoIngles) {
        try {
            URL url = new URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=es&dt=t");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)");

            String postBody = "q=" + URLEncoder.encode(textoIngles, "UTF-8");
            byte[] bytes = postBody.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
                os.flush();
            }

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder jsonResponse = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonResponse.append(line);
                }
                reader.close();

                JSONArray root = new JSONArray(jsonResponse.toString());
                JSONArray sentences = root.getJSONArray(0);
                StringBuilder textoFinal = new StringBuilder();
                for (int i = 0; i < sentences.length(); i++) {
                    JSONArray sentence = sentences.getJSONArray(i);
                    if (sentence.length() > 0 && !sentence.isNull(0)) {
                        textoFinal.append(sentence.getString(0));
                    }
                }

                String res = textoFinal.toString().trim();
                if (!res.isEmpty()) {
                    return res;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String traducirConMyMemory(String textoIngles) {
        if (textoIngles == null || textoIngles.trim().isEmpty()) return null;
        try {
            // MyMemory admite un máximo de 500 caracteres por petición.
            // Para textos breves (títulos, nombres de ingredientes), traducir directamente.
            if (textoIngles.length() <= 400) {
                return traducirSegmentoMyMemory(textoIngles);
            }

            // Para textos largos (instrucciones completas), dividimos por párrafos o frases
            String[] fragmentos = textoIngles.split("(?<=\\r?\\n)|(?<=\\.)\\s+");
            StringBuilder traduccionTotal = new StringBuilder();
            StringBuilder buffer = new StringBuilder();

            for (String frag : fragmentos) {
                if (buffer.length() + frag.length() > 380) {
                    if (buffer.length() > 0) {
                        String trad = traducirSegmentoMyMemory(buffer.toString());
                        traduccionTotal.append(trad != null ? trad : buffer.toString()).append(" ");
                        buffer.setLength(0);
                    }
                }
                buffer.append(frag);
            }

            if (buffer.length() > 0) {
                String trad = traducirSegmentoMyMemory(buffer.toString());
                traduccionTotal.append(trad != null ? trad : buffer.toString());
            }

            String resultado = traduccionTotal.toString().trim();
            return !resultado.isEmpty() ? resultado : null;
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String traducirSegmentoMyMemory(String segmento) {
        if (segmento == null || segmento.trim().isEmpty()) return "";
        try {
            String encoded = URLEncoder.encode(segmento.trim(), "UTF-8");
            URL url = new URL("https://api.mymemory.translated.net/get?q=" + encoded + "&langpair=en|es");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)");

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder jsonResponse = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonResponse.append(line);
                }
                reader.close();

                JSONObject obj = new JSONObject(jsonResponse.toString());
                if (obj.has("responseData")) {
                    JSONObject resData = obj.getJSONObject("responseData");
                    String trad = resData.optString("translatedText", "");
                    if (!trad.trim().isEmpty() && !trad.contains("QUERY LENGTH LIMIT EXCEEDED")) {
                        return trad;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Traduce un texto genérico (como el título de una receta) de forma asíncrona.
     */
    public static void traducirTextoAsync(String textoIngles, TraduccionCallback callback) {
        traducirInstruccionesAsync(textoIngles, callback);
    }

    /**
     * Traduce el título de una receta de forma síncrona si ya está en caché o es conocido,
     * devolviendo el original en su defecto.
     */
    public static String traducirTitulo(String titulo) {
        if (titulo == null || titulo.trim().isEmpty() || !esEspanol()) {
            return titulo;
        }
        if (CACHE_TRADUCCIONES.containsKey(titulo)) {
            return CACHE_TRADUCCIONES.get(titulo);
        }
        return titulo;
    }

    /**
     * Traduce por lotes los ingredientes de una receta que no se encontraban en el diccionario estático.
     */
    public static void traducirIngredientesFaltantesAsync(List<MealDto.IngredienteItem> ingredientes, Runnable onActualizado) {
        if (!esEspanol() || ingredientes == null || ingredientes.isEmpty()) {
            return;
        }

        List<String> faltantes = new ArrayList<>();
        for (MealDto.IngredienteItem item : ingredientes) {
            String ing = item.getIngrediente();
            if (ing != null && !ing.trim().isEmpty()) {
                String clave = ing.trim().toLowerCase();
                if (!INGREDIENTES_ES.containsKey(clave) && !CACHE_TRADUCCIONES.containsKey(clave)) {
                    faltantes.add(ing.trim());
                }
            }
        }

        if (faltantes.isEmpty()) {
            return;
        }

        // Unir ingredientes separados por salto de línea para traducirlos en un único viaje HTTP
        String paquete = String.join("\n", faltantes);
        traducirTextoAsync(paquete, (textoTraducido, fueExitosa) -> {
            if (textoTraducido != null) {
                String[] lineas = textoTraducido.split("\\r?\\n");
                for (int i = 0; i < Math.min(faltantes.size(), lineas.length); i++) {
                    String orig = faltantes.get(i).toLowerCase();
                    String trad = lineas[i].trim();
                    if (!trad.isEmpty()) {
                        CACHE_TRADUCCIONES.put(orig, trad);
                        persistirEnDisco(orig, trad);
                        INGREDIENTES_ES.put(orig, trad);
                    }
                }
                if (onActualizado != null) {
                    onActualizado.run();
                }
            }
        });
    }

    /**
     * Determina si se debe mostrar una nota informativa aclarando que la receta
     * está en su versión original en inglés por falta de traducción en TheMealDB.
     */
    public static boolean requiereAvisoIdioma(MealDto meal) {
        if (!esEspanol() || meal == null) return false;
        if (meal.tieneTraduccionEspanol()) return false;
        if (meal.getStrMeal() != null && RECETAS_DESTACADAS_ES.containsKey(meal.getStrMeal().trim().toLowerCase())) {
            return false;
        }
        return true;
    }

    /**
     * Parsea el texto completo de instrucciones y lo divide en pasos limpios y legibles.
     * Soporta saltos de línea (\r\n, \n), viñetas numeradas ("1.", "Step 1:"), y en su defecto
     * división por oraciones.
     */
    public static List<String> desglosarPasosInstrucciones(String instrucciones) {
        List<String> pasos = new ArrayList<>();
        if (instrucciones == null || instrucciones.trim().isEmpty()) {
            return pasos;
        }

        // 1. Dividir primero por saltos de línea (\r\n, \n)
        String[] lineas = instrucciones.split("\\r?\\n+");

        for (String linea : lineas) {
            String pasoLimpio = limpiarPaso(linea);
            if (!pasoLimpio.isEmpty()) {
                pasos.add(pasoLimpio);
            }
        }

        // 2. Si sólo vino una única línea muy larga (sin saltos de línea), dividimos por oraciones
        if (pasos.size() <= 1 && instrucciones.length() > 150) {
            pasos.clear();
            // Dividir por punto seguido de espacio y mayúscula
            String[] frases = instrucciones.split("(?<=\\.)\\s+(?=[A-Z0-9ÁÉÍÓÚ])");
            for (String frase : frases) {
                String pasoLimpio = limpiarPaso(frase);
                if (!pasoLimpio.isEmpty()) {
                    pasos.add(pasoLimpio);
                }
            }
        }

        if (pasos.isEmpty() && !instrucciones.trim().isEmpty()) {
            pasos.add(instrucciones.trim());
        }

        return pasos;
    }

    private static String limpiarPaso(String texto) {
        if (texto == null) return "";
        String limpio = texto.trim();

        // 1. Descartar encabezados genéricos aislados ("Instructions:", "Method", "Preparación", etc.)
        if (limpio.matches("^(?i:(?:cooking\\s+)?instructions?|directions?|method|preparaci[oó]n|elaboraci[oó]n|modo\\s+de\\s+empleo)[.:-]?$")) {
            return "";
        }

        // 2. Si la línea es únicamente un número aislado ("1", "1.") o un marcador de paso ("Step 1", "Paso 1", "PASO 1:", etc.)
        if (limpio.matches("^(?i:(?:step|paso|etapa|fase)\\s*\\d+|\\d+)[.:-]?$")) {
            return "";
        }

        // 3. Descartar marcadores de sección en mayúsculas breves (ej: "STEP 2 - STIR FRY", "PASO 2 - SALTEADO")
        if (limpio.matches("^(?i:(?:step|paso)\\s*\\d+\\s*[-–—]\\s*[a-záéíóúñA-ZÁÉÍÓÚÑ\\s]{2,18})$")) {
            return "";
        }

        // 4. Eliminar prefijos de paso cuando hay contenido útil detrás (ej: "Paso 1: Mezclar...", "STEP 1 - Put...", "1.- Batir...")
        limpio = limpio.replaceFirst("^(?i:(?:step|paso|etapa|fase)\\s*\\d+\\s*[:.-]*\\s*)", "");
        limpio = limpio.replaceFirst("^\\d+\\s*[.):-]+\\s*", "");
        return limpio.trim();
    }
}
