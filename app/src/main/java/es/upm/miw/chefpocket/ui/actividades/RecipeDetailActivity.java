package es.upm.miw.chefpocket.ui.actividades;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import es.upm.miw.chefpocket.R;
import es.upm.miw.chefpocket.datos.api.RetrofitClient;
import es.upm.miw.chefpocket.datos.api.modelos.MealDto;
import es.upm.miw.chefpocket.datos.api.modelos.MealResponse;
import es.upm.miw.chefpocket.datos.firebase.FirebaseDbHelper;
import es.upm.miw.chefpocket.utilidades.NetworkUtils;
import es.upm.miw.chefpocket.utilidades.TraductorUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Actividad dedicada para visualizar en profundidad el detalle de una receta.
 * Muestra fotografía en alta resolución con CollapsingToolbarLayout, ingredientes desglosados,
 * instrucciones paso a paso con adaptación al español, aviso de idioma si procede,
 * enlace directo a vídeo de YouTube con comprobación de red y acción de guardar en favoritos.
 */
public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECETA = "extra_receta";
    private static final String LOG_TAG = "ChefPocket";

    // Vistas principales
    private CollapsingToolbarLayout collapsingToolbar;
    private ImageView ivPortada;
    private TextView tvCategoria;
    private TextView tvOrigen;
    private MaterialCardView cardAvisoIdioma;
    private LinearLayout llIngredientesLista;
    private LinearLayout llPasosLista;
    private MaterialButton btnVerVideo;
    private MaterialButton btnMarcarCocinada;
    private FloatingActionButton fabFavorito;
    private View layoutCargando;

    private static final Map<String, MealDto> CACHE_DETALLES_RECETAS = new ConcurrentHashMap<>();

    private MealDto recetaActual;
    private boolean esFavorito = false;
    private boolean esCocinada = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        inicializarVistas();
        configurarToolbar();

        // Recuperar la receta enviada mediante el Intent
        recetaActual = (MealDto) getIntent().getSerializableExtra(EXTRA_RECETA);
        if (recetaActual == null) {
            Log.e(LOG_TAG, "RecipeDetailActivity: No se recibió ningún objeto MealDto en el Intent.");
            finish();
            return;
        }

        Log.i(LOG_TAG, "RecipeDetailActivity: Mostrando receta -> " + recetaActual.getStrMeal() + " (ID: " + recetaActual.getIdMeal() + ")");

        // Poblamos de inmediato los datos que ya tengamos para una UI instantánea y fluida
        popularVistas(recetaActual);

        // Si la receta viene de un filtro rápido (filter.php) o carece de ingredientes/instrucciones:
        if (recetaActual.getStrInstructions() == null
                || recetaActual.getStrInstructions().trim().isEmpty()
                || recetaActual.getIngredientesDesglosados().isEmpty()) {
            if (CACHE_DETALLES_RECETAS.containsKey(recetaActual.getIdMeal())) {
                MealDto cached = CACHE_DETALLES_RECETAS.get(recetaActual.getIdMeal());
                Log.d(LOG_TAG, "RecipeDetailActivity: Sirviendo detalles completos desde caché (0 ms).");
                popularVistas(cached);
            } else {
                cargarDetallesCompletosDeRed(recetaActual.getIdMeal());
            }
        }

        configurarListeners();
    }

    private void inicializarVistas() {
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        ivPortada = findViewById(R.id.iv_detalle_portada);
        tvCategoria = findViewById(R.id.tv_detalle_categoria);
        tvOrigen = findViewById(R.id.tv_detalle_origen);
        cardAvisoIdioma = findViewById(R.id.card_aviso_idioma);
        llIngredientesLista = findViewById(R.id.ll_ingredientes_lista);
        llPasosLista = findViewById(R.id.ll_pasos_lista);
        btnVerVideo = findViewById(R.id.btn_ver_video_detalle);
        btnMarcarCocinada = findViewById(R.id.btn_marcar_cocinado);
        fabFavorito = findViewById(R.id.fab_favorito_detalle);
        layoutCargando = findViewById(R.id.layout_cargando_detalle);
    }

    private void configurarToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_detalle);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void popularVistas(MealDto meal) {
        this.recetaActual = meal;

        // Título en la barra colapsable (con soporte de traducción automática)
        collapsingToolbar.setTitle(TraductorUtils.traducirTitulo(meal.getStrMeal()));
        if (TraductorUtils.esEspanol() && meal.getStrMeal() != null) {
            TraductorUtils.traducirTextoAsync(meal.getStrMeal(), (tituloTrad, fueExitosa) -> {
                if (!isFinishing() && !isDestroyed() && tituloTrad != null) {
                    collapsingToolbar.setTitle(tituloTrad);
                }
            });
        }

        // Fotografía del plato con Glide
        Glide.with(this)
                .load(meal.getStrMealThumb())
                .placeholder(R.drawable.ic_recipe_placeholder)
                .error(R.drawable.ic_recipe_placeholder)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivPortada);

        // Categoría y origen traducidos adaptativamente
        String cat = meal.getStrCategory() != null ? meal.getStrCategory() : "";
        String orig = meal.getStrArea() != null ? meal.getStrArea() : "";

        tvCategoria.setText(TraductorUtils.traducirCategoria(cat));

        if (!orig.trim().isEmpty()) {
            tvOrigen.setVisibility(View.VISIBLE);
            tvOrigen.setText(TraductorUtils.traducirOrigen(orig));
        } else {
            tvOrigen.setVisibility(View.GONE);
        }

        // El aviso de idioma permanece oculto por defecto; solo se mostrará si no hay conexión o falla la traducción
        cardAvisoIdioma.setVisibility(View.GONE);

        // Desglose de ingredientes con medidas
        popularIngredientes(meal.getIngredientesDesglosados());

        // Si faltan ingredientes por traducir, resolverlos por lotes en segundo plano
        if (TraductorUtils.esEspanol() && meal.getIngredientesDesglosados() != null) {
            TraductorUtils.traducirIngredientesFaltantesAsync(meal.getIngredientesDesglosados(), () -> {
                if (!isFinishing() && !isDestroyed()) {
                    popularIngredientes(meal.getIngredientesDesglosados());
                }
            });
        }

        // Instrucciones de preparación paso a paso formateadas de inmediato
        if (!TraductorUtils.esEspanol() || TraductorUtils.tieneTraduccionCompleta(meal)) {
            cardAvisoIdioma.setVisibility(View.GONE);
            popularInstrucciones(TraductorUtils.obtenerInstrucciones(meal));
        } else if (meal.getStrInstructions() != null && !meal.getStrInstructions().trim().isEmpty()) {
            // Mostramos de inmediato las instrucciones para que el usuario no espere con pantalla vacía
            popularInstrucciones(meal.getStrInstructions());

            // Traducimos en segundo plano y actualizamos con fluidez sin bloquear la lectura
            TraductorUtils.traducirInstruccionesAsync(meal.getStrInstructions(), (textoTraducido, fueExitosa) -> {
                if (!isFinishing() && !isDestroyed()) {
                    if (fueExitosa && textoTraducido != null && !textoTraducido.trim().isEmpty()) {
                        cardAvisoIdioma.setVisibility(View.GONE);
                        popularInstrucciones(textoTraducido);
                    } else {
                        cardAvisoIdioma.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        // Botón de vídeo tutorial en YouTube
        String urlYoutube = meal.getStrYoutube();
        if (urlYoutube != null && !urlYoutube.trim().isEmpty()) {
            btnVerVideo.setVisibility(View.VISIBLE);
            btnVerVideo.setOnClickListener(v -> abrirVideoTutorial(urlYoutube));
        } else {
            btnVerVideo.setVisibility(View.GONE);
        }
    }

    private void popularIngredientes(List<MealDto.IngredienteItem> ingredientes) {
        llIngredientesLista.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        if (ingredientes == null || ingredientes.isEmpty()) {
            TextView tvAviso = new TextView(this);
            tvAviso.setText(R.string.ingredientes_en_instrucciones);
            tvAviso.setTextAppearance(this, com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
            tvAviso.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.chef_text_secondary));
            tvAviso.setPadding(8, 16, 8, 16);
            llIngredientesLista.addView(tvAviso);
            return;
        }

        for (MealDto.IngredienteItem item : ingredientes) {
            View viewItem = inflater.inflate(R.layout.item_ingrediente, llIngredientesLista, false);
            TextView tvNombre = viewItem.findViewById(R.id.tv_nombre_ingrediente);
            TextView tvMedida = viewItem.findViewById(R.id.tv_medida_ingrediente);

            String nombreTraducido = TraductorUtils.traducirIngrediente(item.getIngrediente());
            String medidaTraducida = TraductorUtils.traducirMedida(item.getMedida());

            tvNombre.setText(nombreTraducido);
            tvMedida.setText(medidaTraducida);

            llIngredientesLista.addView(viewItem);
        }
    }

    private void mostrarPlaceholderTraduccion() {
        llPasosLista.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText(R.string.traduciendo_instrucciones);
        tv.setTextAppearance(this, com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        tv.setTextColor(ContextCompat.getColor(this, R.color.chef_text_secondary));
        tv.setPadding(8, 24, 8, 24);
        llPasosLista.addView(tv);
    }

    private void popularInstrucciones(String textoInstrucciones) {
        llPasosLista.removeAllViews();
        List<String> pasos = TraductorUtils.desglosarPasosInstrucciones(textoInstrucciones);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < pasos.size(); i++) {
            View viewPaso = inflater.inflate(R.layout.item_paso_instruccion, llPasosLista, false);
            TextView tvNumero = viewPaso.findViewById(R.id.tv_numero_paso);
            TextView tvTexto = viewPaso.findViewById(R.id.tv_texto_paso);

            tvNumero.setText(String.valueOf(i + 1));
            tvTexto.setText(pasos.get(i));

            llPasosLista.addView(viewPaso);
        }
    }

    private void cargarDetallesCompletosDeRed(String idMeal) {
        Log.i(LOG_TAG, "RecipeDetailActivity: Solicitando detalles completos a TheMealDB (lookup.php?i=" + idMeal + ")...");
        layoutCargando.setVisibility(View.VISIBLE);

        RetrofitClient.getInstance().getApiService().obtenerRecetaPorId(idMeal).enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(@NonNull Call<MealResponse> call, @NonNull Response<MealResponse> response) {
                layoutCargando.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getMeals() != null && !response.body().getMeals().isEmpty()) {
                    MealDto detalleCompleto = response.body().getMeals().get(0);
                    Log.i(LOG_TAG, "RecipeDetailActivity: Detalles completos recibidos para " + detalleCompleto.getStrMeal());
                    CACHE_DETALLES_RECETAS.put(detalleCompleto.getIdMeal(), detalleCompleto);
                    popularVistas(detalleCompleto);
                } else {
                    Log.w(LOG_TAG, "RecipeDetailActivity: Respuesta vacía de lookup.");
                    popularVistas(recetaActual);
                }
            }

            @Override
            public void onFailure(@NonNull Call<MealResponse> call, @NonNull Throwable t) {
                layoutCargando.setVisibility(View.GONE);
                Log.e(LOG_TAG, "RecipeDetailActivity: Error al cargar lookup -> " + t.getMessage());
                popularVistas(recetaActual);
            }
        });
    }

    private void configurarListeners() {
        comprobarEstadoFavorito();
        comprobarEstadoCocinada();

        fabFavorito.setOnClickListener(v -> {
            if (recetaActual == null) return;

            fabFavorito.setEnabled(false);
            FirebaseDbHelper.getInstance().conmutarFavorito(recetaActual, new FirebaseDbHelper.ConmutarCallback() {
                @Override
                public void onCompletado(boolean ahoraEsFavorito) {
                    esFavorito = ahoraEsFavorito;
                    actualizarIconoFavorito();
                    fabFavorito.animate().scaleX(1.25f).scaleY(1.25f).setDuration(120).withEndAction(() -> fabFavorito.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()).start();
                    fabFavorito.setEnabled(true);
                    int msgRes = esFavorito ? R.string.favorito_anadido : R.string.favorito_eliminado;
                    Snackbar.make(findViewById(android.R.id.content), msgRes, Snackbar.LENGTH_SHORT).show();
                    Log.i(LOG_TAG, "RecipeDetailActivity: Estado de favorito actualizado en Firebase -> " + (esFavorito ? "Añadido" : "Eliminado"));
                }

                @Override
                public void onError(String mensaje) {
                    fabFavorito.setEnabled(true);
                    Log.e(LOG_TAG, "RecipeDetailActivity: Error al conmutar favorito: " + mensaje);
                    Snackbar.make(findViewById(android.R.id.content), mensaje != null ? mensaje : getString(R.string.error_firebase), Snackbar.LENGTH_SHORT).show();
                }
            });
        });

        btnMarcarCocinada.setOnClickListener(v -> {
            if (recetaActual == null) return;

            btnMarcarCocinada.setEnabled(false);
            FirebaseDbHelper.getInstance().conmutarCocinada(recetaActual, new FirebaseDbHelper.ConmutarCallback() {
                @Override
                public void onCompletado(boolean ahoraEsCocinada) {
                    esCocinada = ahoraEsCocinada;
                    actualizarBotonCocinada();
                    btnMarcarCocinada.setEnabled(true);
                    int msgRes = esCocinada ? R.string.snack_plato_cocinado_anadido : R.string.snack_plato_cocinado_eliminado;
                    Snackbar.make(findViewById(android.R.id.content), msgRes, Snackbar.LENGTH_SHORT).show();
                    Log.i(LOG_TAG, "RecipeDetailActivity: Plato cocinado actualizado -> " + esCocinada);
                }

                @Override
                public void onError(String mensaje) {
                    btnMarcarCocinada.setEnabled(true);
                    Snackbar.make(findViewById(android.R.id.content), mensaje != null ? mensaje : getString(R.string.error_firebase), Snackbar.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void comprobarEstadoFavorito() {
        if (recetaActual == null || recetaActual.getIdMeal() == null) return;
        FirebaseDbHelper.getInstance().comprobarEsFavorito(recetaActual.getIdMeal(), esFav -> {
            esFavorito = esFav;
            actualizarIconoFavorito();
        });
    }

    private void comprobarEstadoCocinada() {
        if (recetaActual == null || recetaActual.getIdMeal() == null) return;
        FirebaseDbHelper.getInstance().comprobarEsCocinada(recetaActual.getIdMeal(), yaCocinada -> {
            esCocinada = yaCocinada;
            actualizarBotonCocinada();
        });
    }

    private void actualizarBotonCocinada() {
        if (esCocinada) {
            btnMarcarCocinada.setText(R.string.btn_marcar_cocinado_activo);
            btnMarcarCocinada.setBackgroundColor(getResources().getColor(R.color.chef_primary_dark, getTheme()));
        } else {
            btnMarcarCocinada.setText(R.string.btn_marcar_cocinado_inactivo);
            btnMarcarCocinada.setBackgroundColor(getResources().getColor(R.color.chef_primary, getTheme()));
        }
    }

    private void actualizarIconoFavorito() {
        if (esFavorito) {
            fabFavorito.setImageResource(R.drawable.ic_favorite_filled);
            fabFavorito.setContentDescription(getString(R.string.fab_eliminar_favorito));
        } else {
            fabFavorito.setImageResource(R.drawable.ic_favorite_border);
            fabFavorito.setContentDescription(getString(R.string.fab_anadir_favorito));
        }
    }

    private void abrirVideoTutorial(String url) {
        Log.i(LOG_TAG, "RecipeDetailActivity: Abriendo vídeo tutorial -> " + url);
        if (!NetworkUtils.hayConexion(this)) {
            Snackbar.make(findViewById(android.R.id.content), R.string.video_sin_conexion, Snackbar.LENGTH_LONG).show();
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Log.e(LOG_TAG, "RecipeDetailActivity: Error al abrir vídeo -> " + e.getMessage());
            Snackbar.make(findViewById(android.R.id.content), R.string.error_abrir_video, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
