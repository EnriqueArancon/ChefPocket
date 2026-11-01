package es.upm.miw.chefpocket.ui.actividades;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.firebase.ui.auth.AuthUI;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import es.upm.miw.chefpocket.R;
import es.upm.miw.chefpocket.ui.dialogos.CerrarSesionDialogFragment;
import es.upm.miw.chefpocket.ui.fragmentos.EstadisticasFragment;
import es.upm.miw.chefpocket.ui.fragmentos.ExplorarFragment;
import es.upm.miw.chefpocket.ui.fragmentos.FavoritosFragment;
import es.upm.miw.chefpocket.ui.fragmentos.PerfilFragment;

public class MainActivity extends AppCompatActivity implements CerrarSesionDialogFragment.CerrarSesionListener {

    private static final String TAG_EXPLORAR = "fragment_explorar";
    private static final String TAG_FAVORITOS = "fragment_favoritos";
    private static final String TAG_ESTADISTICAS = "fragment_estadisticas";
    private static final String TAG_PERFIL = "fragment_perfil";

    private Fragment fragmentExplorar;
    private Fragment fragmentFavoritos;
    private Fragment fragmentEstadisticas;
    private Fragment fragmentPerfil;
    private Fragment fragmentActivo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verificamos si el usuario está autenticado
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            redirigirALogin();
            return;
        }

        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        FragmentManager fm = getSupportFragmentManager();

        if (savedInstanceState == null) {
            fragmentExplorar = new ExplorarFragment();
            fragmentFavoritos = new FavoritosFragment();
            fragmentEstadisticas = new EstadisticasFragment();
            fragmentPerfil = new PerfilFragment();

            fragmentActivo = fragmentExplorar;

            fm.beginTransaction()
                    .add(R.id.contenedor_fragmentos, fragmentPerfil, TAG_PERFIL).hide(fragmentPerfil)
                    .add(R.id.contenedor_fragmentos, fragmentEstadisticas, TAG_ESTADISTICAS).hide(fragmentEstadisticas)
                    .add(R.id.contenedor_fragmentos, fragmentFavoritos, TAG_FAVORITOS).hide(fragmentFavoritos)
                    .add(R.id.contenedor_fragmentos, fragmentExplorar, TAG_EXPLORAR)
                    .commit();
        } else {
            fragmentExplorar = fm.findFragmentByTag(TAG_EXPLORAR);
            fragmentFavoritos = fm.findFragmentByTag(TAG_FAVORITOS);
            fragmentEstadisticas = fm.findFragmentByTag(TAG_ESTADISTICAS);
            fragmentPerfil = fm.findFragmentByTag(TAG_PERFIL);

            if (fragmentExplorar == null) fragmentExplorar = new ExplorarFragment();
            if (fragmentFavoritos == null) fragmentFavoritos = new FavoritosFragment();
            if (fragmentEstadisticas == null) fragmentEstadisticas = new EstadisticasFragment();
            if (fragmentPerfil == null) fragmentPerfil = new PerfilFragment();

            fragmentActivo = obtenerFragmentoPorId(bottomNav.getSelectedItemId());
            if (fragmentActivo == null) {
                fragmentActivo = fragmentExplorar;
            }
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragmentDestino = obtenerFragmentoPorId(item.getItemId());
            if (fragmentDestino != null && fragmentDestino != fragmentActivo) {
                fm.beginTransaction()
                        .hide(fragmentActivo)
                        .show(fragmentDestino)
                        .commit();
                fragmentActivo = fragmentDestino;
                return true;
            }
            return fragmentDestino != null;
        });
    }

    private Fragment obtenerFragmentoPorId(int itemId) {
        if (itemId == R.id.nav_explorar) {
            return fragmentExplorar;
        } else if (itemId == R.id.nav_favoritos) {
            return fragmentFavoritos;
        } else if (itemId == R.id.nav_estadisticas) {
            return fragmentEstadisticas;
        } else if (itemId == R.id.nav_perfil) {
            return fragmentPerfil;
        }
        return null;
    }

    private void redirigirALogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onConfirmarCierreSesion() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(task -> {
                    Toast.makeText(this, R.string.sesion_cerrada, Toast.LENGTH_SHORT).show();
                    redirigirALogin();
                });
    }
}

