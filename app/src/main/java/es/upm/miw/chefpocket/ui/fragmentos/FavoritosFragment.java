package es.upm.miw.chefpocket.ui.fragmentos;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.ValueEventListener;

import java.util.List;

import es.upm.miw.chefpocket.R;
import es.upm.miw.chefpocket.datos.api.modelos.MealDto;
import es.upm.miw.chefpocket.datos.firebase.FirebaseDbHelper;
import es.upm.miw.chefpocket.ui.actividades.RecipeDetailActivity;
import es.upm.miw.chefpocket.ui.adaptadores.MealAdapter;

/**
 * Fragmento de la pestaña "Favoritos".
 * Muestra el recetario guardado por el usuario en Firebase Realtime Database.
 * Se actualiza de manera reactiva en tiempo real ante cualquier adición o borrado,
 * funcionando de forma transparente en modo offline gracias a la persistencia en disco (Tema 3).
 */
public class FavoritosFragment extends Fragment {

    private static final String LOG_TAG = "FavoritosFragment";

    private ProgressBar progressFavoritos;
    private RecyclerView rvFavoritos;
    private LinearLayout layoutFavoritosVacio;
    private TextView tvSubtituloFavoritos;

    private MealAdapter adapter;
    private ValueEventListener favoritosListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_favoritos, container, false);

        progressFavoritos = root.findViewById(R.id.progress_favoritos);
        rvFavoritos = root.findViewById(R.id.rv_favoritos);
        layoutFavoritosVacio = root.findViewById(R.id.layout_favoritos_vacio);
        tvSubtituloFavoritos = root.findViewById(R.id.tv_subtitulo_favoritos);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new MealAdapter(this::abrirDetalleReceta);
        rvFavoritos.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFavoritos.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        suscribirAFavoritosFirebase();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (favoritosListener != null) {
            FirebaseDbHelper.getInstance().desuscribirDeFavoritos(favoritosListener);
            favoritosListener = null;
        }
    }

    private void suscribirAFavoritosFirebase() {
        if (progressFavoritos != null && (adapter.getItemCount() == 0)) {
            progressFavoritos.setVisibility(View.VISIBLE);
        }

        favoritosListener = FirebaseDbHelper.getInstance().suscribirAFavoritos(new FirebaseDbHelper.FavoritosListCallback() {
            @Override
            public void onFavoritosActualizados(List<MealDto> favoritos) {
                if (!isAdded()) return;

                if (progressFavoritos != null) {
                    progressFavoritos.setVisibility(View.GONE);
                }

                if (favoritos == null || favoritos.isEmpty()) {
                    rvFavoritos.setVisibility(View.GONE);
                    layoutFavoritosVacio.setVisibility(View.VISIBLE);
                    tvSubtituloFavoritos.setText(R.string.subtitulo_seccion_favoritos);
                } else {
                    layoutFavoritosVacio.setVisibility(View.GONE);
                    rvFavoritos.setVisibility(View.VISIBLE);
                    adapter.setRecetas(favoritos);

                    int count = favoritos.size();
                    String textoContador = (count == 1)
                            ? getString(R.string.contador_favoritos_singular)
                            : getString(R.string.contador_favoritos_plural, count);
                    tvSubtituloFavoritos.setText(textoContador);
                }
            }

            @Override
            public void onError(String mensaje) {
                if (!isAdded()) return;
                Log.e(LOG_TAG, "Error al sincronizar favoritos con Firebase: " + mensaje);
                if (progressFavoritos != null) {
                    progressFavoritos.setVisibility(View.GONE);
                }
            }
        });
    }

    private void abrirDetalleReceta(MealDto meal) {
        Intent intent = new Intent(requireContext(), RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECETA, meal);
        startActivity(intent);
    }
}
