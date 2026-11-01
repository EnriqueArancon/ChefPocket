package es.upm.miw.chefpocket.ui.fragmentos;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import es.upm.miw.chefpocket.R;
import es.upm.miw.chefpocket.datos.api.modelos.MealDto;
import es.upm.miw.chefpocket.ui.viewmodel.ExplorarViewModel;

/**
 * Fragmento de la pestaña "Explorar".
 * Muestra el saludo de bienvenida, coordina la interacción con ExplorarViewModel
 * para conservar la propuesta gastronómica en memoria y renderiza la sugerencia con Glide.
 */
public class ExplorarFragment extends Fragment {

    private static final String LOG_TAG = "ChefPocket";

    // ViewModel para estado persistente en memoria (Tema 4, Diapositiva 19)
    private ExplorarViewModel viewModel;

    // Vistas de estado
    private MaterialCardView cardOffline;
    private LinearLayout layoutCargando;
    private MaterialCardView cardRecetaDia;

    // Vistas de la receta
    private ImageView ivRecetaDia;
    private TextView tvNombreReceta;
    private TextView tvCategoriaOrigen;
    private TextView tvInstruccionesResumen;
    private MaterialButton btnRefrescarReceta;
    private MaterialButton btnReintentarConexion;

    // Receta actual cargada en memoria
    private MealDto recetaActual;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_explorar, container, false);

        // Inicializar saludo de bienvenida
        TextView tvSaludo = root.findViewById(R.id.tv_saludo_usuario);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String nombre = (user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                    ? user.getDisplayName()
                    : (user.getEmail() != null ? user.getEmail() : getString(R.string.usuario_desconocido));
            tvSaludo.setText(getString(R.string.saludo_usuario, nombre));
            Log.d(LOG_TAG, "ExplorarFragment: Usuario activo -> " + nombre);
        }

        // Inicializar componentes de la interfaz
        cardOffline = root.findViewById(R.id.card_offline);
        layoutCargando = root.findViewById(R.id.layout_cargando_receta);
        cardRecetaDia = root.findViewById(R.id.card_receta_dia);

        ivRecetaDia = root.findViewById(R.id.iv_receta_dia);
        tvNombreReceta = root.findViewById(R.id.tv_nombre_receta);
        tvCategoriaOrigen = root.findViewById(R.id.tv_categoria_origen);
        tvInstruccionesResumen = root.findViewById(R.id.tv_instrucciones_resumen);
        btnRefrescarReceta = root.findViewById(R.id.btn_refrescar_receta);
        btnReintentarConexion = root.findViewById(R.id.btn_reintentar_conexion);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar el ViewModel asociado al ciclo de vida del fragmento
        viewModel = new ViewModelProvider(this).get(ExplorarViewModel.class);

        // Observar la receta sugerida (si ya existe en memoria, se muestra sin peticiones de red)
        viewModel.getRecetaSugerida().observe(getViewLifecycleOwner(), meal -> {
            if (meal != null) {
                recetaActual = meal;
                mostrarReceta(meal);
            }
        });

        // Observar estado de carga
        viewModel.getCargando().observe(getViewLifecycleOwner(), estaCargando -> {
            if (Boolean.TRUE.equals(estaCargando)) {
                mostrarEstadoCargando();
            }
        });

        // Observar modo offline
        viewModel.getModoOffline().observe(getViewLifecycleOwner(), offline -> {
            if (Boolean.TRUE.equals(offline)) {
                mostrarEstadoOffline();
                if (getView() != null) {
                    Snackbar.make(getView(), R.string.sin_conexion_internet, Snackbar.LENGTH_LONG)
                            .setAction(R.string.btn_reintentar, v -> viewModel.cargarRecetaSugerida(requireContext(), true))
                            .show();
                }
            }
        });

        // Observar mensajes de error
        viewModel.getMensajeError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                mostrarEstadoError(error);
            }
        });

        // Configuración de listeners
        btnRefrescarReceta.setOnClickListener(v -> {
            Log.i(LOG_TAG, "ExplorarFragment: Usuario pulsó 'Probar otra' para forzar nueva sugerencia.");
            viewModel.cargarRecetaSugerida(requireContext(), true);
        });

        btnReintentarConexion.setOnClickListener(v -> {
            Log.i(LOG_TAG, "ExplorarFragment: Usuario pulsó 'Reintentar conexión'.");
            viewModel.cargarRecetaSugerida(requireContext(), true);
        });

        // Carga inicial: reutiliza la receta en memoria si ya fue obtenida previamente (evita parpadeo al rotar)
        viewModel.cargarRecetaSugerida(requireContext(), false);
    }

    /**
     * Muestra en pantalla los datos de la receta obtenida y carga su imagen con Glide.
     */
    private void mostrarReceta(MealDto meal) {
        layoutCargando.setVisibility(View.GONE);
        cardOffline.setVisibility(View.GONE);
        cardRecetaDia.setVisibility(View.VISIBLE);

        tvNombreReceta.setText(meal.getStrMeal());

        String area = meal.getStrArea() != null && !meal.getStrArea().isEmpty() ? meal.getStrArea() : "Internacional";
        String categoria = meal.getStrCategory() != null && !meal.getStrCategory().isEmpty() ? meal.getStrCategory() : "Gourmet";
        tvCategoriaOrigen.setText(getString(R.string.receta_origen_categoria, area, categoria));

        tvInstruccionesResumen.setText(meal.getStrInstructions());

        // Carga de imagen remota con Glide (Tema 4, Diapositiva 15)
        if (meal.getStrMealThumb() != null && !meal.getStrMealThumb().isEmpty()) {
            Log.d(LOG_TAG, "ExplorarFragment: Cargando imagen con Glide desde -> " + meal.getStrMealThumb());
            Glide.with(this)
                    .load(meal.getStrMealThumb())
                    .placeholder(R.drawable.ic_recipe_placeholder)
                    .error(R.drawable.ic_recipe_placeholder)
                    .centerCrop()
                    .into(ivRecetaDia);
        } else {
            ivRecetaDia.setImageResource(R.drawable.ic_recipe_placeholder);
        }
    }

    private void mostrarEstadoCargando() {
        layoutCargando.setVisibility(View.VISIBLE);
        cardOffline.setVisibility(View.GONE);
        cardRecetaDia.setVisibility(View.GONE);
    }

    private void mostrarEstadoOffline() {
        layoutCargando.setVisibility(View.GONE);
        cardRecetaDia.setVisibility(View.GONE);
        cardOffline.setVisibility(View.VISIBLE);
    }

    private void mostrarEstadoError(String mensaje) {
        layoutCargando.setVisibility(View.GONE);
        cardRecetaDia.setVisibility(View.GONE);
        cardOffline.setVisibility(View.VISIBLE);
        if (getView() != null) {
            Snackbar.make(getView(), getString(R.string.error_cargar_receta), Snackbar.LENGTH_LONG)
                    .setAction(R.string.btn_reintentar, v -> viewModel.cargarRecetaSugerida(requireContext(), true))
                    .show();
        }
    }
}
