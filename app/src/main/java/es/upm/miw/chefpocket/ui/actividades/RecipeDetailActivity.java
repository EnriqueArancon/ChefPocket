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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import es.upm.miw.chefpocket.R;
import es.upm.miw.chefpocket.datos.api.RetrofitClient;
import es.upm.miw.chefpocket.datos.api.modelos.MealDto;
import es.upm.miw.chefpocket.datos.api.modelos.MealResponse;
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
    private FloatingActionButton fabFavorito;
    private View layoutCargando;

    private MealDto recetaActual;
    private boolean esFavorito = false;

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

        // Si la receta viene de un filtro rápido (filter.php), solo incluye ID, nombre y foto.
        // En ese caso, consultamos lookup.php para obtener ingredientes e instrucciones completas.
        if (recetaActual.getStrInstructions() == null || recetaActual.getStrInstructions().trim().isEmpty()) {
            cargarDetallesCompletosDeRed(recetaActual.getIdMeal());
        } else {
            popularVistas(recetaActual);
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

        // Título en la barra colapsable
        collapsingToolbar.setTitle(meal.getStrMeal());

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

        // Aviso explicativo si las instrucciones están en inglés original
        if (TraductorUtils.requiereAvisoIdioma(meal)) {
            cardAvisoIdioma.setVisibility(View.VISIBLE);
        } else {
            cardAvisoIdioma.setVisibility(View.GONE);
        }

        // Desglose de ingredientes con medidas
        popularIngredientes(meal.getIngredientesDesglosados());

        // Instrucciones de preparación paso a paso formateadas
        popularInstrucciones(TraductorUtils.obtenerInstrucciones(meal));

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
        fabFavorito.setOnClickListener(v -> {
            esFavorito = !esFavorito;
            actualizarIconoFavorito();
            int msgRes = esFavorito ? R.string.favorito_anadido : R.string.favorito_eliminado;
            Snackbar.make(findViewById(android.R.id.content), msgRes, Snackbar.LENGTH_SHORT).show();
            Log.i(LOG_TAG, "RecipeDetailActivity: Favorito alternado -> " + (esFavorito ? "Añadido" : "Eliminado"));
        });
    }

    private void actualizarIconoFavorito() {
        if (esFavorito) {
            fabFavorito.setImageResource(R.drawable.ic_nav_favorites);
            fabFavorito.setColorFilter(getResources().getColor(R.color.white, getTheme()));
        } else {
            fabFavorito.setImageResource(R.drawable.ic_nav_favorites);
            fabFavorito.setColorFilter(getResources().getColor(R.color.white, getTheme()));
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
            Snackbar.make(findViewById(android.R.id.content), "No se pudo abrir el enlace del vídeo", Snackbar.LENGTH_SHORT).show();
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
