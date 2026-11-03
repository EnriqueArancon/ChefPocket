package es.upm.miw.chefpocket.ui.fragmentos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import es.upm.miw.chefpocket.R;
import es.upm.miw.chefpocket.ui.dialogos.CerrarSesionDialogFragment;

public class PerfilFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_perfil, container, false);

        TextView tvNombre = root.findViewById(R.id.tv_perfil_nombre);
        TextView tvEmail = root.findViewById(R.id.tv_perfil_email);
        TextView tvUid = root.findViewById(R.id.tv_perfil_uid);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String rawNombre = user.getDisplayName();
            if (rawNombre != null && rawNombre.contains("passwo")) {
                com.google.firebase.auth.UserProfileChangeRequest req = new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName("Enrique Arancón")
                        .build();
                user.updateProfile(req);
                rawNombre = "Enrique Arancón";
            }
            String nombre = (rawNombre != null && !rawNombre.isEmpty())
                    ? rawNombre
                    : (user.getEmail() != null ? user.getEmail() : getString(R.string.usuario_desconocido));
            tvNombre.setText(nombre);
            tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");
            tvUid.setText(getString(R.string.id_usuario_formato, user.getUid()));
        }

        root.findViewById(R.id.btn_cerrar_sesion_perfil).setOnClickListener(v -> {
            new CerrarSesionDialogFragment().show(getParentFragmentManager(), "CerrarSesionDialog");
        });

        return root;
    }
}
