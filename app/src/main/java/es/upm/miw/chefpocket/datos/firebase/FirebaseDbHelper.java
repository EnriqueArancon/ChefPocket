package es.upm.miw.chefpocket.datos.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import es.upm.miw.chefpocket.datos.api.modelos.MealDto;

/**
 * Clase auxiliar para centralizar y encapsular todas las operaciones con Firebase Realtime Database.
 * Estructura las recetas favoritas particionadas por el UID de cada usuario:
 * /usuarios/{userId}/favoritos/{idMeal} -> MealDto
 * 
 * Al estar habilitada la persistencia en disco en ChefPocketApp, todas las lecturas
 * y escrituras funcionan automáticamente en modo offline (Tema 3 / Firebase).
 */
public class FirebaseDbHelper {

    private static final String LOG_TAG = "FirebaseDbHelper";
    private static final String NODO_USUARIOS = "usuarios";
    private static final String NODO_FAVORITOS = "favoritos";
    private static final String NODO_COCINADAS = "cocinadas";

    private static FirebaseDbHelper instance;
    private final FirebaseDatabase database;

    // Callbacks para desacoplar la capa de datos de la interfaz de usuario
    public interface EsFavoritoCallback {
        void onResultado(boolean esFavorito);
    }

    public interface OperacionCallback {
        void onExito();
        void onError(String mensaje);
    }

    public interface ConmutarCallback {
        void onCompletado(boolean ahoraEsFavorito);
        void onError(String mensaje);
    }

    public interface FavoritosListCallback {
        void onFavoritosActualizados(List<MealDto> favoritos);
        void onError(String mensaje);
    }

    private FirebaseDbHelper() {
        this.database = FirebaseDatabase.getInstance();
    }

    public static synchronized FirebaseDbHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseDbHelper();
        }
        return instance;
    }

    /**
     * Obtiene la referencia al nodo de favoritos del usuario autenticado actual.
     * Retorna null si el usuario no ha iniciado sesión.
     */
    private DatabaseReference getFavoritosUsuarioRef() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(LOG_TAG, "getFavoritosUsuarioRef: No hay usuario autenticado.");
            return null;
        }
        return database.getReference(NODO_USUARIOS)
                .child(user.getUid())
                .child(NODO_FAVORITOS);
    }

    /**
     * Comprueba de manera asíncrona si una receta específica ya está guardada en favoritos.
     */
    public void comprobarEsFavorito(String idMeal, EsFavoritoCallback callback) {
        if (idMeal == null || callback == null) return;

        DatabaseReference ref = getFavoritosUsuarioRef();
        if (ref == null) {
            callback.onResultado(false);
            return;
        }

        ref.child(idMeal).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                callback.onResultado(snapshot.exists());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(LOG_TAG, "Error al comprobar favorito: " + error.getMessage());
                callback.onResultado(false);
            }
        });
    }

    /**
     * Guarda una receta completa en los favoritos del usuario en Firebase.
     */
    public void guardarFavorito(MealDto meal, OperacionCallback callback) {
        if (meal == null || meal.getIdMeal() == null) {
            if (callback != null) callback.onError("Receta inválida.");
            return;
        }

        DatabaseReference ref = getFavoritosUsuarioRef();
        if (ref == null) {
            if (callback != null) callback.onError("Sesión no iniciada.");
            return;
        }

        ref.child(meal.getIdMeal()).setValue(meal)
                .addOnSuccessListener(aVoid -> {
                    Log.i(LOG_TAG, "Receta guardada en favoritos: " + meal.getStrMeal());
                    if (callback != null) callback.onExito();
                })
                .addOnFailureListener(e -> {
                    Log.e(LOG_TAG, "Error al guardar favorito: " + e.getMessage());
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Elimina una receta de los favoritos del usuario en Firebase.
     */
    public void eliminarFavorito(String idMeal, OperacionCallback callback) {
        if (idMeal == null) {
            if (callback != null) callback.onError("Identificador inválido.");
            return;
        }

        DatabaseReference ref = getFavoritosUsuarioRef();
        if (ref == null) {
            if (callback != null) callback.onError("Sesión no iniciada.");
            return;
        }

        ref.child(idMeal).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.i(LOG_TAG, "Receta eliminada de favoritos: " + idMeal);
                    if (callback != null) callback.onExito();
                })
                .addOnFailureListener(e -> {
                    Log.e(LOG_TAG, "Error al eliminar favorito: " + e.getMessage());
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Conmuta el estado de una receta: si existe la borra, si no existe la añade.
     */
    public void conmutarFavorito(MealDto meal, ConmutarCallback callback) {
        if (meal == null || meal.getIdMeal() == null) {
            if (callback != null) callback.onError("Receta no válida.");
            return;
        }

        comprobarEsFavorito(meal.getIdMeal(), esFavorito -> {
            if (esFavorito) {
                eliminarFavorito(meal.getIdMeal(), new OperacionCallback() {
                    @Override
                    public void onExito() {
                        if (callback != null) callback.onCompletado(false);
                    }

                    @Override
                    public void onError(String mensaje) {
                        if (callback != null) callback.onError(mensaje);
                    }
                });
            } else {
                guardarFavorito(meal, new OperacionCallback() {
                    @Override
                    public void onExito() {
                        if (callback != null) callback.onCompletado(true);
                    }

                    @Override
                    public void onError(String mensaje) {
                        if (callback != null) callback.onError(mensaje);
                    }
                });
            }
        });
    }

    /**
     * Suscribe un escuchador en tiempo real a la colección de favoritos del usuario.
     * Retorna el ValueEventListener registrado para permitir su desuscripción en onDestroy/onStop.
     */
    public ValueEventListener suscribirAFavoritos(FavoritosListCallback callback) {
        DatabaseReference ref = getFavoritosUsuarioRef();
        if (ref == null) {
            if (callback != null) callback.onError("Usuario no autenticado.");
            return null;
        }

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<MealDto> lista = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    MealDto meal = child.getValue(MealDto.class);
                    if (meal != null) {
                        lista.add(meal);
                    }
                }
                Log.d(LOG_TAG, "Favoritos actualizados en tiempo real: " + lista.size() + " recetas.");
                if (callback != null) {
                    callback.onFavoritosActualizados(lista);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(LOG_TAG, "Error en suscripción de favoritos: " + error.getMessage());
                if (callback != null) {
                    callback.onError(error.getMessage());
                }
            }
        };

        ref.addValueEventListener(listener);
        return listener;
    }

    /**
     * Desregistra un escuchador de favoritos para evitar fugas de memoria.
     */
    public void desuscribirDeFavoritos(ValueEventListener listener) {
        if (listener == null) return;
        DatabaseReference ref = getFavoritosUsuarioRef();
        if (ref != null) {
            ref.removeEventListener(listener);
        }
    }

    /**
     * Obtiene la referencia al nodo de recetas cocinadas por el usuario.
     */
    private DatabaseReference getCocinadasUsuarioRef() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(LOG_TAG, "getCocinadasUsuarioRef: No hay usuario autenticado.");
            return null;
        }
        return database.getReference(NODO_USUARIOS)
                .child(user.getUid())
                .child(NODO_COCINADAS);
    }

    /**
     * Comprueba si una receta ya ha sido marcada como cocinada por el usuario.
     */
    public void comprobarEsCocinada(String idMeal, EsFavoritoCallback callback) {
        if (idMeal == null || callback == null) return;

        DatabaseReference ref = getCocinadasUsuarioRef();
        if (ref == null) {
            callback.onResultado(false);
            return;
        }

        ref.child(idMeal).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                callback.onResultado(snapshot.exists());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(LOG_TAG, "Error al comprobar cocinada: " + error.getMessage());
                callback.onResultado(false);
            }
        });
    }

    /**
     * Conmuta el estado de cocinada: la guarda en el historial o la retira.
     */
    public void conmutarCocinada(MealDto meal, ConmutarCallback callback) {
        if (meal == null || meal.getIdMeal() == null) {
            if (callback != null) callback.onError("Receta inválida.");
            return;
        }

        DatabaseReference ref = getCocinadasUsuarioRef();
        if (ref == null) {
            if (callback != null) callback.onError("Sesión no iniciada.");
            return;
        }

        comprobarEsCocinada(meal.getIdMeal(), yaCocinada -> {
            if (yaCocinada) {
                ref.child(meal.getIdMeal()).removeValue()
                        .addOnSuccessListener(aVoid -> {
                            Log.i(LOG_TAG, "Plato retirado de cocinados: " + meal.getStrMeal());
                            if (callback != null) callback.onCompletado(false);
                        })
                        .addOnFailureListener(e -> {
                            if (callback != null) callback.onError(e.getMessage());
                        });
            } else {
                ref.child(meal.getIdMeal()).setValue(meal)
                        .addOnSuccessListener(aVoid -> {
                            Log.i(LOG_TAG, "¡Plato cocinado con éxito y registrado!: " + meal.getStrMeal());
                            if (callback != null) callback.onCompletado(true);
                        })
                        .addOnFailureListener(e -> {
                            if (callback != null) callback.onError(e.getMessage());
                        });
            }
        });
    }

    /**
     * Suscribe un escuchador a la lista de platos cocinados para actualizar el Rango de Chef en tiempo real.
     */
    public ValueEventListener suscribirAPlatosCocinados(FavoritosListCallback callback) {
        DatabaseReference ref = getCocinadasUsuarioRef();
        if (ref == null) {
            if (callback != null) callback.onError("Usuario no autenticado.");
            return null;
        }

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<MealDto> lista = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    MealDto meal = child.getValue(MealDto.class);
                    if (meal != null) {
                        lista.add(meal);
                    }
                }
                Log.d(LOG_TAG, "Platos cocinados actualizados: " + lista.size());
                if (callback != null) {
                    callback.onFavoritosActualizados(lista);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(LOG_TAG, "Error en suscripción de cocinados: " + error.getMessage());
                if (callback != null) {
                    callback.onError(error.getMessage());
                }
            }
        };

        ref.addValueEventListener(listener);
        return listener;
    }

    /**
     * Desregistra el escuchador de platos cocinados.
     */
    public void desuscribirDeCocinadas(ValueEventListener listener) {
        if (listener == null) return;
        DatabaseReference ref = getCocinadasUsuarioRef();
        if (ref != null) {
            ref.removeEventListener(listener);
        }
    }
}

