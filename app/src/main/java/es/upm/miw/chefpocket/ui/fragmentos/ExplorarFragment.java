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

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import es.upm.miw.chefpocket.R;
import es.upm.miw.chefpocket.datos.api.RetrofitClient;
import es.upm.miw.chefpocket.datos.api.modelos.MealDto;
import es.upm.miw.chefpocket.datos.api.modelos.MealResponse;
import es.upm.miw.chefpocket.utilidades.NetworkUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragmento de la pestaña "Explorar".
 * Muestra la bienvenida al usuario, gestiona el estado online/offline y consume
 * la API REST de TheMealDB mediante Retrofit para mostrar la receta del día con Glide.
 */
public class ExplorarFragment extends Fragment {

    private static final String LOG_TAG = "ChefPocket";

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

    // Receta actual mostrada
    private MealDto recetaActual;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_explorar, container, false);

        // Inicializar vistas de bienvenida
        TextView tvSaludo = root.findViewById(R.id.tv_saludo_usuario);
        TextView tvEmail = root.findViewById(R.id.tv_email_usuario);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String nombre = (user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                    ? user.getDisplayName()
                    : (user.getEmail() != null ? user.getEmail() : getString(R.string.usuario_desconocido));
            tvSaludo.setText(getString(R.string.saludo_usuario, nombre));
            tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");
            Log.d(LOG_TAG, "ExplorarFragment: Usuario activo -> " + nombre + " (" + user.getEmail() + ")");
        }

        // Inicializar vistas de control y receta
        cardOffline = root.findViewById(R.id.card_offline);
        layoutCargando = root.findViewById(R.id.layout_cargando_receta);
        cardRecetaDia = root.findViewById(R.id.card_receta_dia);

        ivRecetaDia = root.findViewById(R.id.iv_receta_dia);
        tvNombreReceta = root.findViewById(R.id.tv_nombre_receta);
        tvCategoriaOrigen = root.findViewById(R.id.tv_categoria_origen);
        tvInstruccionesResumen = root.findViewById(R.id.tv_instrucciones_resumen);
        btnRefrescarReceta = root.findViewById(R.id.btn_refrescar_receta);
        btnReintentarConexion = root.findViewById(R.id.btn_reintentar_conexion);

        // Listeners de actualización y reintento
        btnRefrescarReceta.setOnClickListener(v -> {
            Log.i(LOG_TAG, "ExplorarFragment: Usuario solicitó otra receta aleatoria.");
            cargarRecetaAleatoria();
        });

        btnReintentarConexion.setOnClickListener(v -> {
            Log.i(LOG_TAG, "ExplorarFragment: Usuario pulsó 'Reintentar conexión'.");
            cargarRecetaAleatoria();
        });

        // Cargar receta inicial
        cargarRecetaAleatoria();

        return root;
    }

    /**
     * Comprueba la conectividad de red y realiza la llamada a TheMealDB mediante Retrofit.
     * En caso de estar offline, conmuta la UI para mostrar la tarjeta de aviso sin bloquear la app.
     */
    private void cargarRecetaAleatoria() {
        if (!isAdded() || getContext() == null) {
            return;
        }

        // 1. Verificación previa de conexión (Tema 4, Diapositiva 3)
        if (!NetworkUtils.hayConexion(requireContext())) {
            Log.w(LOG_TAG, "ExplorarFragment: Sin conexión a Internet. Mostrando vista offline.");
            mostrarEstadoOffline();
            if (getView() != null) {
                Snackbar.make(getView(), R.string.sin_conexion_internet, Snackbar.LENGTH_LONG)
                        .setAction(R.string.btn_reintentar, v -> cargarRecetaAleatoria())
                        .show();
            }
            return;
        }

        // 2. Estado de carga activo
        mostrarEstadoCargando();
        Log.i(LOG_TAG, "ExplorarFragment: Conectado a Internet. Solicitando receta aleatoria a TheMealDB...");

        // 3. Petición asíncrona con Retrofit (Tema 4, Diapositiva 12)
        RetrofitClient.getInstance()
                .getApiService()
                .obtenerRecetaAleatoria()
                .enqueue(new Callback<MealResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<MealResponse> call, @NonNull Response<MealResponse> response) {
                        if (!isAdded()) return;

                        if (response.isSuccessful() && response.body() != null && response.body().tieneRecetas()) {
                            recetaActual = response.body().getPrimeraReceta();
                            Log.i(LOG_TAG, "ExplorarFragment: Receta recibida con éxito -> ID: " +
                                    recetaActual.getIdMeal() + ", Título: " + recetaActual.getStrMeal());
                            mostrarReceta(recetaActual);
                        } else {
                            Log.e(LOG_TAG, "ExplorarFragment: Respuesta no exitosa de TheMealDB. Código HTTP: " + response.code());
                            mostrarEstadoError(getString(R.string.error_cargar_receta));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<MealResponse> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        Log.e(LOG_TAG, "ExplorarFragment: Error en la llamada Retrofit: " + t.getMessage(), t);
                        mostrarEstadoError(t.getMessage());
                    }
                });
    }

    /**
     * Muestra en pantalla los datos de la receta obtenida y carga su imagen con Glide.
     */
    private void mostrarReceta(MealDto meal) {
        layoutCargando.setVisibility(View.GONE);
        cardOffline.setVisibility(View.GONE);
        cardRecetaDia.setVisibility(View.VISIBLE);

        tvNombreReceta.setText(meal.getStrMeal());

        String area = meal.getStrArea().isEmpty() ? "Internacional" : meal.getStrArea();
        String categoria = meal.getStrCategory().isEmpty() ? "Gourmet" : meal.getStrCategory();
        tvCategoriaOrigen.setText(getString(R.string.receta_origen_categoria, area, categoria));

        tvInstruccionesResumen.setText(meal.getStrInstructions());

        // Carga de imagen remota con Glide (Tema 4)
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
                    .setAction(R.string.btn_reintentar, v -> cargarRecetaAleatoria())
                    .show();
        }
    }
}
