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

public class ExplorarFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_explorar, container, false);

        TextView tvSaludo = root.findViewById(R.id.tv_saludo_usuario);
        TextView tvEmail = root.findViewById(R.id.tv_email_usuario);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String nombre = (user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                    ? user.getDisplayName()
                    : (user.getEmail() != null ? user.getEmail() : getString(R.string.usuario_desconocido));
            tvSaludo.setText(getString(R.string.saludo_usuario, nombre));
            tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        }

        return root;
    }
}
