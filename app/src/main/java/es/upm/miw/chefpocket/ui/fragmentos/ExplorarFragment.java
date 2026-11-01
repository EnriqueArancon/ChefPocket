package es.upm.miw.chefpocket.ui.fragmentos;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import es.upm.miw.chefpocket.R;
import es.upm.miw.chefpocket.datos.api.modelos.MealDto;
import es.upm.miw.chefpocket.ui.actividades.RecipeDetailActivity;
import es.upm.miw.chefpocket.ui.adaptadores.MealAdapter;
import es.upm.miw.chefpocket.ui.viewmodel.ExplorarViewModel;
import es.upm.miw.chefpocket.utilidades.TraductorUtils;

/**
 * Fragmento de la pestaña "Explorar".
 * Muestra el saludo de bienvenida, la tarjeta destacada "¿No sabes qué cocinar?",
 * la barra de búsqueda interactiva, los filtros de categorías con Chips y el
 * catálogo completo de recetas gestionado mediante RecyclerView y ViewModel.
 */
public class ExplorarFragment extends Fragment {

    private static final String LOG_TAG = "ChefPocket";

    // ViewModel para estado persistente en memoria
    private ExplorarViewModel viewModel;

    // Vistas de estado y sugerencia
    private MaterialCardView cardOffline;
    private LinearLayout layoutCargandoReceta;
    private MaterialCardView cardRecetaDia;
    private ImageView ivRecetaDia;
    private TextView tvNombreReceta;
    private TextView tvCategoriaOrigen;
    private TextView tvInstruccionesResumen;
    private MaterialButton btnRefrescarReceta;
    private MaterialButton btnReintentarConexion;

    // Buscador y filtros
    private TextInputEditText etBuscarReceta;
    private ChipGroup chipGroupCategorias;

    // Catálogo RecyclerView
    private ProgressBar progressCatalogo;
    private TextView tvSinResultados;
    private RecyclerView rvCatalogo;
    private MealAdapter adapter;

    // Receta sugerida activa
    private MealDto recetaSugeridaActual;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_explorar, container, false);

        // Saludo de bienvenida
        TextView tvSaludo = root.findViewById(R.id.tv_saludo_usuario);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String nombre = (user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                    ? user.getDisplayName()
                    : (user.getEmail() != null ? user.getEmail() : getString(R.string.usuario_desconocido));
            tvSaludo.setText(getString(R.string.saludo_usuario, nombre));
        }

        // Vistas de sugerencia
        cardOffline = root.findViewById(R.id.card_offline);
        layoutCargandoReceta = root.findViewById(R.id.layout_cargando_receta);
        cardRecetaDia = root.findViewById(R.id.card_receta_dia);
        ivRecetaDia = root.findViewById(R.id.iv_receta_dia);
        tvNombreReceta = root.findViewById(R.id.tv_nombre_receta);
        tvCategoriaOrigen = root.findViewById(R.id.tv_categoria_origen);
        tvInstruccionesResumen = root.findViewById(R.id.tv_instrucciones_resumen);
        btnRefrescarReceta = root.findViewById(R.id.btn_refrescar_receta);
        btnReintentarConexion = root.findViewById(R.id.btn_reintentar_conexion);

        // Buscador y filtros
        etBuscarReceta = root.findViewById(R.id.et_buscar_receta);
        chipGroupCategorias = root.findViewById(R.id.chip_group_categorias);

        // Catálogo
        progressCatalogo = root.findViewById(R.id.progress_catalogo);
        tvSinResultados = root.findViewById(R.id.tv_sin_resultados);
        rvCatalogo = root.findViewById(R.id.rv_catalogo_recetas);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Vinculamos el ViewModel a la Activity anfitriona para que su estado (receta sugerida y catálogo)
        // sobreviva a la navegación entre pestañas del BottomNavigationView y a rotaciones.
        viewModel = new ViewModelProvider(requireActivity()).get(ExplorarViewModel.class);

        // Configuración de RecyclerView
        adapter = new MealAdapter(this::abrirDetalleReceta);
        rvCatalogo.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCatalogo.setAdapter(adapter);

        configurarObservadores();
        configurarBuscador();
        configurarFiltrosCategorias();

        // Listeners de sugerencia
        btnRefrescarReceta.setOnClickListener(v -> {
            Log.i(LOG_TAG, "ExplorarFragment: Solicitando nueva sugerencia...");
            viewModel.cargarRecetaSugerida(requireContext(), true);
        });

        btnReintentarConexion.setOnClickListener(v -> {
            Log.i(LOG_TAG, "ExplorarFragment: Reintentando conexión...");
            viewModel.cargarRecetaSugerida(requireContext(), true);
            viewModel.cargarCatalogoPorCategoria(requireContext(), "Todas");
        });

        cardRecetaDia.setOnClickListener(v -> {
            if (recetaSugeridaActual != null) {
                abrirDetalleReceta(recetaSugeridaActual);
            }
        });

        // Carga inicial: no duplica llamadas si el ViewModel ya dispone de datos
        viewModel.cargarRecetaSugerida(requireContext(), false);
        if (viewModel.getRecetasCatalogo().getValue() == null || viewModel.getRecetasCatalogo().getValue().isEmpty()) {
            viewModel.cargarCatalogoPorCategoria(requireContext(), "Todas");
        }
    }

    private void configurarObservadores() {
        // Observador de la receta sugerida
        viewModel.getRecetaSugerida().observe(getViewLifecycleOwner(), meal -> {
            if (meal != null) {
                recetaSugeridaActual = meal;
                mostrarSugerencia(meal);
            }
        });

        // Observador de carga de sugerencia
        viewModel.getCargandoSugerencia().observe(getViewLifecycleOwner(), cargando -> {
            layoutCargandoReceta.setVisibility(Boolean.TRUE.equals(cargando) ? View.VISIBLE : View.GONE);
            if (Boolean.TRUE.equals(cargando)) {
                cardRecetaDia.setVisibility(View.GONE);
            }
        });

        // Observador del catálogo de recetas
        viewModel.getRecetasCatalogo().observe(getViewLifecycleOwner(), this::mostrarCatalogo);

        // Observador de carga del catálogo
        viewModel.getCargandoCatalogo().observe(getViewLifecycleOwner(), cargando ->
                progressCatalogo.setVisibility(Boolean.TRUE.equals(cargando) ? View.VISIBLE : View.GONE));

        // Observador del modo offline
        viewModel.getModoOffline().observe(getViewLifecycleOwner(), offline -> {
            if (Boolean.TRUE.equals(offline)) {
                cardOffline.setVisibility(View.VISIBLE);
                cardRecetaDia.setVisibility(View.GONE);
                if (getView() != null) {
                    Snackbar.make(getView(), R.string.sin_conexion_internet, Snackbar.LENGTH_LONG)
                            .setAction(R.string.btn_reintentar, v -> {
                                viewModel.cargarRecetaSugerida(requireContext(), true);
                                viewModel.cargarCatalogoPorCategoria(requireContext(), "Todas");
                            })
                            .show();
                }
            } else {
                cardOffline.setVisibility(View.GONE);
            }
        });
    }

    private void configurarBuscador() {
        etBuscarReceta.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String query = etBuscarReceta.getText() != null ? etBuscarReceta.getText().toString().trim() : "";
                ocultarTeclado();
                Log.i(LOG_TAG, "ExplorarFragment: Buscando recetas con término: " + query);
                viewModel.buscarRecetas(requireContext(), query);
                return true;
            }
            return false;
        });

        etBuscarReceta.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    // Si se vacía el buscador, recarga la categoría actualmente seleccionada
                    viewModel.cargarCatalogoPorCategoria(requireContext(), "Todas");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void configurarFiltrosCategorias() {
        chipGroupCategorias.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                // Si el usuario desmarca el chip activo, vuelve a "Todas"
                chipGroupCategorias.check(R.id.chip_cat_todas);
                return;
            }

            int checkedId = checkedIds.get(0);
            String categoriaEnglish = "Todas";

            if (checkedId == R.id.chip_cat_pasta) {
                categoriaEnglish = "Pasta";
            } else if (checkedId == R.id.chip_cat_pollo) {
                categoriaEnglish = "Chicken";
            } else if (checkedId == R.id.chip_cat_pescado) {
                categoriaEnglish = "Seafood";
            } else if (checkedId == R.id.chip_cat_postres) {
                categoriaEnglish = "Dessert";
            } else if (checkedId == R.id.chip_cat_vegetariano) {
                categoriaEnglish = "Vegetarian";
            } else if (checkedId == R.id.chip_cat_desayunos) {
                categoriaEnglish = "Breakfast";
            } else if (checkedId == R.id.chip_cat_ternera) {
                categoriaEnglish = "Beef";
            }

            Log.i(LOG_TAG, "ExplorarFragment: Categoría seleccionada -> " + categoriaEnglish);
            viewModel.cargarCatalogoPorCategoria(requireContext(), categoriaEnglish);
        });
    }

    private void mostrarSugerencia(MealDto meal) {
        layoutCargandoReceta.setVisibility(View.GONE);
        cardRecetaDia.setVisibility(View.VISIBLE);

        tvNombreReceta.setText(meal.getStrMeal());

        String categoria = TraductorUtils.traducirCategoria(meal.getStrCategory());
        String origen = TraductorUtils.traducirOrigen(meal.getStrArea());
        tvCategoriaOrigen.setText(getString(R.string.receta_origen_categoria, origen, categoria));

        String instrucciones = TraductorUtils.obtenerInstrucciones(meal);
        tvInstruccionesResumen.setText(instrucciones);

        if (meal.getStrMealThumb() != null && !meal.getStrMealThumb().isEmpty()) {
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

    private void mostrarCatalogo(List<MealDto> lista) {
        if (lista == null || lista.isEmpty()) {
            rvCatalogo.setVisibility(View.GONE);
            tvSinResultados.setVisibility(View.VISIBLE);
        } else {
            tvSinResultados.setVisibility(View.GONE);
            rvCatalogo.setVisibility(View.VISIBLE);
            adapter.setRecetas(lista);
        }
    }

    private void abrirDetalleReceta(MealDto meal) {
        if (meal == null) return;
        Log.i(LOG_TAG, "ExplorarFragment: Abriendo detalle de receta -> " + meal.getStrMeal() + " (ID: " + meal.getIdMeal() + ")");
        Intent intent = new Intent(requireContext(), RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECETA, meal);
        startActivity(intent);
    }

    private void ocultarTeclado() {
        View view = getActivity() != null ? getActivity().getCurrentFocus() : null;
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}
