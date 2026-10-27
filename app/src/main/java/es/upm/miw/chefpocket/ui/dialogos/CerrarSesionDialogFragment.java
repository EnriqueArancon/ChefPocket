package es.upm.miw.chefpocket.ui.dialogos;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import es.upm.miw.chefpocket.R;

public class CerrarSesionDialogFragment extends DialogFragment {

    public interface CerrarSesionListener {
        void onConfirmarCierreSesion();
    }

    private CerrarSesionListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof CerrarSesionListener) {
            listener = (CerrarSesionListener) context;
        } else {
            throw new ClassCastException(context + " debe implementar CerrarSesionListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());
        builder.setTitle(R.string.dialogo_cerrar_sesion_titulo)
               .setMessage(R.string.dialogo_cerrar_sesion_mensaje)
               .setPositiveButton(R.string.dialogo_accion_aceptar, (dialog, which) -> {
                   if (listener != null) {
                       listener.onConfirmarCierreSesion();
                   }
               })
               .setNegativeButton(R.string.dialogo_accion_cancelar, (dialog, which) -> dialog.dismiss());

        return builder.create();
    }
}
