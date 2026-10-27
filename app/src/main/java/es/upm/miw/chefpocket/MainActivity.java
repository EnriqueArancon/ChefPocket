package es.upm.miw.chefpocket;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.firebase.ui.auth.AuthUI;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import es.upm.miw.chefpocket.ui.actividades.LoginActivity;
import es.upm.miw.chefpocket.ui.dialogos.CerrarSesionDialogFragment;
import es.upm.miw.chefpocket.ui.fragmentos.EstadisticasFragment;
import es.upm.miw.chefpocket.ui.fragmentos.ExplorarFragment;
import es.upm.miw.chefpocket.ui.fragmentos.FavoritosFragment;
import es.upm.miw.chefpocket.ui.fragmentos.PerfilFragment;

public class MainActivity extends AppCompatActivity implements CerrarSesionDialogFragment.CerrarSesionListener {

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
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragmentSeleccionado = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_explorar) {
                fragmentSeleccionado = new ExplorarFragment();
            } else if (itemId == R.id.nav_favoritos) {
                fragmentSeleccionado = new FavoritosFragment();
            } else if (itemId == R.id.nav_estadisticas) {
                fragmentSeleccionado = new EstadisticasFragment();
            } else if (itemId == R.id.nav_perfil) {
                fragmentSeleccionado = new PerfilFragment();
            }

            if (fragmentSeleccionado != null) {
                cargarFragmento(fragmentSeleccionado);
                return true;
            }
            return false;
        });

        // Cargar por defecto la primera pestaña (Explorar)
        if (savedInstanceState == null) {
            cargarFragmento(new ExplorarFragment());
        }
    }

    private void cargarFragmento(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.contenedor_fragmentos, fragment)
                .commit();
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