package es.upm.miw.chefpocket.ui.fragmentos;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.upm.miw.chefpocket.R;
import es.upm.miw.chefpocket.datos.api.modelos.MealDto;
import es.upm.miw.chefpocket.datos.firebase.FirebaseDbHelper;
import es.upm.miw.chefpocket.utilidades.TraductorUtils;

/**
 * Fragmento de la pestaña "Estadísticas".
 * Destaca de forma prioritaria el Rango Culinario del usuario (Chef Level)
 * basado en los platos que ha preparado con éxito, garantizando que su experiencia
 * culinaria ganada permanezca intacta independientemente de su lista de favoritos.
 */
public class EstadisticasFragment extends Fragment {

    private static final String LOG_TAG = "EstadisticasFragment";

    private ProgressBar progressStats;

    // Tarjeta Hero: Rango de Chef
    private TextView tvStatRangoChef;
    private TextView tvStatRangoSub;

    // Métricas de actividad
    private TextView tvStatCocinadosNumero;
    private TextView tvStatFavoritosNumero;

    // Preferencias gastronómicas
    private TextView tvStatCategoriaTop;
    private TextView tvStatCategoriaTopSub;
    private TextView tvStatOrigenTop;
    private TextView tvStatOrigenTopSub;

    // Listas observables en tiempo real
    private final List<MealDto> listaFavoritos = new ArrayList<>();
    private final List<MealDto> listaCocinadas = new ArrayList<>();

    private ValueEventListener favoritosListener;
    private ValueEventListener cocinadasListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_estadisticas, container, false);

        progressStats = root.findViewById(R.id.progress_stats);

        tvStatRangoChef = root.findViewById(R.id.tv_stat_rango_chef);
        tvStatRangoSub = root.findViewById(R.id.tv_stat_rango_sub);

        tvStatCocinadosNumero = root.findViewById(R.id.tv_stat_cocinados_numero);
        tvStatFavoritosNumero = root.findViewById(R.id.tv_stat_favoritos_numero);

        tvStatCategoriaTop = root.findViewById(R.id.tv_stat_categoria_top);
        tvStatCategoriaTopSub = root.findViewById(R.id.tv_stat_categoria_top_sub);
        tvStatOrigenTop = root.findViewById(R.id.tv_stat_origen_top);
        tvStatOrigenTopSub = root.findViewById(R.id.tv_stat_origen_top_sub);

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        suscribirAEscuchadoresFirebase();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (favoritosListener != null) {
            FirebaseDbHelper.getInstance().desuscribirDeFavoritos(favoritosListener);
            favoritosListener = null;
        }
        if (cocinadasListener != null) {
            FirebaseDbHelper.getInstance().desuscribirDeCocinadas(cocinadasListener);
            cocinadasListener = null;
        }
    }

    private void suscribirAEscuchadoresFirebase() {
        if (progressStats != null) {
            progressStats.setVisibility(View.VISIBLE);
        }

        // Suscripción a platos cocinados (Experiencia / Rango de Chef)
        cocinadasListener = FirebaseDbHelper.getInstance().suscribirAPlatosCocinados(new FirebaseDbHelper.FavoritosListCallback() {
            @Override
            public void onFavoritosActualizados(List<MealDto> cocinadas) {
                if (!isAdded()) return;
                listaCocinadas.clear();
                if (cocinadas != null) {
                    listaCocinadas.addAll(cocinadas);
                }
                actualizarVistaEstadisticas();
            }

            @Override
            public void onError(String mensaje) {
                Log.e(LOG_TAG, "Error al sincronizar platos cocinados: " + mensaje);
            }
        });

        // Suscripción a recetas en favoritos (Deseos de cocina)
        favoritosListener = FirebaseDbHelper.getInstance().suscribirAFavoritos(new FirebaseDbHelper.FavoritosListCallback() {
            @Override
            public void onFavoritosActualizados(List<MealDto> favoritos) {
                if (!isAdded()) return;
                listaFavoritos.clear();
                if (favoritos != null) {
                    listaFavoritos.addAll(favoritos);
                }
                actualizarVistaEstadisticas();
            }

            @Override
            public void onError(String mensaje) {
                Log.e(LOG_TAG, "Error al sincronizar favoritos: " + mensaje);
            }
        });
    }

    private void actualizarVistaEstadisticas() {
        if (progressStats != null) {
            progressStats.setVisibility(View.GONE);
        }

        int totalCocinados = listaCocinadas.size();
        int totalFavoritos = listaFavoritos.size();

        tvStatCocinadosNumero.setText(String.valueOf(totalCocinados));
        tvStatFavoritosNumero.setText(String.valueOf(totalFavoritos));

        // 1. RANGO DE CHEF: Basado exclusivamente en platos preparados con éxito (experiencia ganada)
        if (totalCocinados == 0) {
            tvStatRangoChef.setText(R.string.rango_pinche);
            tvStatRangoSub.setText(R.string.stat_rango_sub_cero);
        } else if (totalCocinados < 3) {
            tvStatRangoChef.setText(R.string.rango_novato);
            int falta = 3 - totalCocinados;
            tvStatRangoSub.setText(getString(R.string.stat_rango_sub_novato, totalCocinados, falta));
        } else if (totalCocinados < 6) {
            tvStatRangoChef.setText(R.string.rango_aficionado);
            int falta = 6 - totalCocinados;
            tvStatRangoSub.setText(getString(R.string.stat_rango_sub_aficionado, totalCocinados, falta));
        } else {
            tvStatRangoChef.setText(R.string.rango_master);
            tvStatRangoSub.setText(getString(R.string.stat_rango_sub_master, totalCocinados));
        }

        // 2. PREFERENCIAS GASTRONÓMICAS: Se analizan priorizando platos cocinados y completando con favoritos
        List<MealDto> conjuntoAnalisis = new ArrayList<>(listaCocinadas);
        if (conjuntoAnalisis.isEmpty()) {
            conjuntoAnalisis.addAll(listaFavoritos);
        }

        if (conjuntoAnalisis.isEmpty()) {
            tvStatCategoriaTop.setText("-");
            tvStatCategoriaTopSub.setText(R.string.stat_descubre_gustos);
            tvStatOrigenTop.setText("-");
            tvStatOrigenTopSub.setText(R.string.stat_explora_gastronomias);
            return;
        }

        Map<String, Integer> freqCategorias = new HashMap<>();
        Map<String, Integer> freqOrigenes = new HashMap<>();

        for (MealDto meal : conjuntoAnalisis) {
            String cat = meal.getStrCategory();
            if (cat != null && !cat.trim().isEmpty()) {
                freqCategorias.put(cat, freqCategorias.getOrDefault(cat, 0) + 1);
            }

            String origen = meal.getStrArea();
            if (origen != null && !origen.trim().isEmpty()) {
                freqOrigenes.put(origen, freqOrigenes.getOrDefault(origen, 0) + 1);
            }
        }

        // Categoría top
        String topCat = "-";
        int topCatCount = 0;
        for (Map.Entry<String, Integer> entry : freqCategorias.entrySet()) {
            if (entry.getValue() > topCatCount) {
                topCat = entry.getKey();
                topCatCount = entry.getValue();
            }
        }

        if (!"-".equals(topCat)) {
            tvStatCategoriaTop.setText(TraductorUtils.traducirCategoria(topCat));
            tvStatCategoriaTopSub.setText(topCatCount == 1
                    ? getString(R.string.stat_recetas_contadas_singular)
                    : getString(R.string.stat_recetas_contadas_plural, topCatCount));
        } else {
            tvStatCategoriaTop.setText(R.string.stat_categoria_variada);
            tvStatCategoriaTopSub.setText(R.string.stat_categoria_variada_sub);
        }

        // Origen top
        String topOrigen = "-";
        int topOrigenCount = 0;
        for (Map.Entry<String, Integer> entry : freqOrigenes.entrySet()) {
            if (entry.getValue() > topOrigenCount) {
                topOrigen = entry.getKey();
                topOrigenCount = entry.getValue();
            }
        }

        if (!"-".equals(topOrigen)) {
            tvStatOrigenTop.setText(TraductorUtils.traducirOrigen(topOrigen));
            tvStatOrigenTopSub.setText(topOrigenCount == 1
                    ? getString(R.string.stat_origen_contado_singular)
                    : getString(R.string.stat_origen_contado_plural, topOrigenCount));
        } else {
            tvStatOrigenTop.setText(R.string.stat_origen_internacional);
            tvStatOrigenTopSub.setText(R.string.stat_origen_internacional_sub);
        }
    }
}
