package es.upm.miw.chefpocket;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Collections;
import java.util.List;

import es.upm.miw.chefpocket.ui.dialogos.CerrarSesionDialogFragment;

public class MainActivity extends AppCompatActivity implements CerrarSesionDialogFragment.CerrarSesionListener {

    private TextView tvSaludoUsuario;
    private TextView tvEmailUsuario;

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            this::onSignInResult
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvSaludoUsuario = findViewById(R.id.tv_saludo_usuario);
        tvEmailUsuario = findViewById(R.id.tv_email_usuario);

        verificarSesion();
    }

    private void verificarSesion() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            iniciarFlujoLogin();
        } else {
            actualizarInterfazUsuario(currentUser);
        }
    }

    private void iniciarFlujoLogin() {
        List<AuthUI.IdpConfig> providers = Collections.singletonList(
                new AuthUI.IdpConfig.EmailBuilder().build()
        );

        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build();

        signInLauncher.launch(signInIntent);
    }

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        IdpResponse response = result.getIdpResponse();
        if (result.getResultCode() == RESULT_OK) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            actualizarInterfazUsuario(user);
            if (user != null) {
                String nombre = (user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                        ? user.getDisplayName()
                        : (user.getEmail() != null ? user.getEmail() : getString(R.string.usuario_desconocido));
                Snackbar.make(findViewById(R.id.main), getString(R.string.mensaje_bienvenida, nombre), Snackbar.LENGTH_LONG).show();
            }
        } else {
            if (response == null) {
                // El usuario ha pulsado atrás sin autenticarse
                Snackbar.make(findViewById(R.id.main), R.string.login_cancelado, Snackbar.LENGTH_SHORT).show();
                finish();
            } else {
                Snackbar.make(findViewById(R.id.main), R.string.error_login, Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void actualizarInterfazUsuario(FirebaseUser user) {
        if (user != null) {
            String nombre = (user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                    ? user.getDisplayName()
                    : getString(R.string.usuario_desconocido);
            tvSaludoUsuario.setText(getString(R.string.mensaje_bienvenida, nombre));
            tvEmailUsuario.setText(user.getEmail() != null ? user.getEmail() : "");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_principal, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.accion_cerrar_sesion) {
            new CerrarSesionDialogFragment().show(getSupportFragmentManager(), "CerrarSesionDialog");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfirmarCierreSesion() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(task -> {
                    Snackbar.make(findViewById(R.id.main), R.string.sesion_cerrada, Snackbar.LENGTH_SHORT).show();
                    iniciarFlujoLogin();
                });
    }
}