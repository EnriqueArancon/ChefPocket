package es.upm.miw.chefpocket.ui.actividades;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import es.upm.miw.chefpocket.R;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilNombre;
    private TextInputEditText etNombre;
    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText etPassword;

    private TextView tabLogin;
    private TextView tabRegistro;
    private MaterialButton btnAccionAuth;
    private TextView tvToggleModo;
    private ProgressBar progressAuth;

    private boolean esModoRegistro = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Si ya hay sesión activa en Firebase, saltamos directamente a la pantalla principal
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            irAMainActivity();
            return;
        }

        setContentView(R.layout.activity_login);

        inicializarVistas();
        configurarListeners();
        actualizarModoUI(false);
    }

    private void inicializarVistas() {
        tilNombre = findViewById(R.id.til_nombre);
        etNombre = findViewById(R.id.et_nombre);
        tilEmail = findViewById(R.id.til_email);
        etEmail = findViewById(R.id.et_email);
        tilPassword = findViewById(R.id.til_password);
        etPassword = findViewById(R.id.et_password);

        tabLogin = findViewById(R.id.tab_login);
        tabRegistro = findViewById(R.id.tab_registro);
        btnAccionAuth = findViewById(R.id.btn_accion_auth);
        tvToggleModo = findViewById(R.id.tv_toggle_modo);
        progressAuth = findViewById(R.id.progress_auth);
    }

    private void configurarListeners() {
        tabLogin.setOnClickListener(v -> {
            if (esModoRegistro) {
                esModoRegistro = false;
                actualizarModoUI(true);
            }
        });

        tabRegistro.setOnClickListener(v -> {
            if (!esModoRegistro) {
                esModoRegistro = true;
                actualizarModoUI(true);
            }
        });

        tvToggleModo.setOnClickListener(v -> {
            esModoRegistro = !esModoRegistro;
            actualizarModoUI(true);
        });

        btnAccionAuth.setOnClickListener(v -> procesarAutenticacion());
    }

    private void actualizarModoUI(boolean limpiarCampos) {
        limpiarErrores();
        if (limpiarCampos) {
            if (etNombre != null) etNombre.setText("");
            if (etEmail != null) etEmail.setText("");
            if (etPassword != null) etPassword.setText("");
        }
        if (esModoRegistro) {
            tilNombre.setVisibility(View.VISIBLE);
            btnAccionAuth.setText(R.string.btn_crear_cuenta_accion);
            tvToggleModo.setText(R.string.pregunta_si_cuenta);

            tabRegistro.setBackgroundResource(R.drawable.bg_badge_origin);
            tabRegistro.setTextColor(getColor(R.color.chef_primary_dark));
            tabRegistro.setTypeface(null, Typeface.BOLD);

            tabLogin.setBackground(null);
            tabLogin.setTextColor(getColor(R.color.chef_text_secondary));
            tabLogin.setTypeface(null, Typeface.NORMAL);
        } else {
            tilNombre.setVisibility(View.GONE);
            btnAccionAuth.setText(R.string.btn_iniciar_sesion_accion);
            tvToggleModo.setText(R.string.pregunta_no_cuenta);

            tabLogin.setBackgroundResource(R.drawable.bg_badge_origin);
            tabLogin.setTextColor(getColor(R.color.chef_primary_dark));
            tabLogin.setTypeface(null, Typeface.BOLD);

            tabRegistro.setBackground(null);
            tabRegistro.setTextColor(getColor(R.color.chef_text_secondary));
            tabRegistro.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void limpiarErrores() {
        tilNombre.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
    }

    private void procesarAutenticacion() {
        limpiarErrores();

        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String nombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";

        boolean hayError = false;

        if (esModoRegistro && nombre.isEmpty()) {
            tilNombre.setError(getString(R.string.error_campo_vacio));
            hayError = true;
        }

        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.error_campo_vacio));
            hayError = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_correo_invalido));
            hayError = true;
        }

        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.error_campo_vacio));
            hayError = true;
        } else if (password.length() < 6) {
            tilPassword.setError(getString(R.string.error_contrasena_corta));
            hayError = true;
        }

        if (hayError) {
            return;
        }

        mostrarCargando(true);

        if (esModoRegistro) {
            ejecutarRegistro(email, password, nombre);
        } else {
            ejecutarLogin(email, password);
        }
    }

    private void ejecutarLogin(String email, String password) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    mostrarCargando(false);
                    if (task.isSuccessful()) {
                        irAMainActivity();
                    } else {
                        String errorMsg = task.getException() != null && task.getException().getMessage() != null
                                ? task.getException().getMessage()
                                : getString(R.string.error_credenciales_invalidas);
                        if (errorMsg.contains("password") || errorMsg.contains("credential") || errorMsg.contains("user-not-found")) {
                            errorMsg = getString(R.string.error_credenciales_invalidas);
                        }
                        Snackbar.make(btnAccionAuth, errorMsg, Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void ejecutarRegistro(String email, String password, String nombre) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null && !nombre.isEmpty()) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(nombre)
                                    .build();
                            user.updateProfile(profileUpdates).addOnCompleteListener(profileTask -> {
                                mostrarCargando(false);
                                irAMainActivity();
                            });
                        } else {
                            mostrarCargando(false);
                            irAMainActivity();
                        }
                    } else {
                        mostrarCargando(false);
                        String errorMsg = task.getException() != null && task.getException().getMessage() != null
                                ? task.getException().getMessage()
                                : getString(R.string.error_login);
                        if (errorMsg.contains("already in use")) {
                            errorMsg = getString(R.string.error_correo_en_uso);
                        }
                        Snackbar.make(btnAccionAuth, errorMsg, Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void mostrarCargando(boolean cargando) {
        progressAuth.setVisibility(cargando ? View.VISIBLE : View.GONE);
        btnAccionAuth.setEnabled(!cargando);
        tabLogin.setEnabled(!cargando);
        tabRegistro.setEnabled(!cargando);
        tvToggleModo.setEnabled(!cargando);
    }

    private void irAMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
