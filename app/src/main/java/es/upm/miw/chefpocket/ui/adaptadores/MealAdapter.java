package es.upm.miw.chefpocket.ui.adaptadores;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import es.upm.miw.chefpocket.R;
import es.upm.miw.chefpocket.datos.api.modelos.MealDto;
import es.upm.miw.chefpocket.utilidades.TraductorUtils;

/**
 * Adaptador de RecyclerView para listar colecciones de recetas culinarias.
 * Implementa la carga asíncrona de imágenes mediante Glide y adapta las descripciones
 * al idioma preferido del usuario mediante TraductorUtils.
 */
public class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {

    public interface OnMealClickListener {
        void onMealClick(MealDto meal);
    }

    private final List<MealDto> recetas = new ArrayList<>();
    private final OnMealClickListener clickListener;

    public MealAdapter(OnMealClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setRecetas(List<MealDto> nuevasRecetas) {
        recetas.clear();
        if (nuevasRecetas != null) {
            recetas.addAll(nuevasRecetas);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_receta, parent, false);
        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        MealDto meal = recetas.get(position);
        holder.bind(meal, clickListener);
    }

    @Override
    public int getItemCount() {
        return recetas.size();
    }

    static class MealViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivReceta;
        private final TextView tvTitulo;
        private final TextView tvCategoriaOrigen;

        public MealViewHolder(@NonNull View itemView) {
            super(itemView);
            ivReceta = itemView.findViewById(R.id.iv_item_receta);
            tvTitulo = itemView.findViewById(R.id.tv_item_titulo);
            tvCategoriaOrigen = itemView.findViewById(R.id.tv_item_categoria_origen);
        }

        public void bind(MealDto meal, OnMealClickListener listener) {
            // Título de la receta (utiliza traducción síncrona en memoria O(1) para garantizar 60 FPS sin parpadeos al hacer scroll)
            tvTitulo.setText(TraductorUtils.traducirTitulo(meal.getStrMeal()));

            String categoria = TraductorUtils.traducirCategoria(meal.getStrCategory());
            String origen = TraductorUtils.traducirOrigen(meal.getStrArea());

            if (meal.getStrArea() != null && !meal.getStrArea().isEmpty()) {
                tvCategoriaOrigen.setText(String.format("%s · %s", origen, categoria));
            } else {
                tvCategoriaOrigen.setText(categoria);
            }

            // Carga asíncrona optimizada de miniatura con Glide (Tema 4, Diapositiva 15)
            if (meal.getStrMealThumb() != null && !meal.getStrMealThumb().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(meal.getStrMealThumb())
                        .placeholder(R.drawable.ic_recipe_placeholder)
                        .error(R.drawable.ic_recipe_placeholder)
                        .centerCrop()
                        .into(ivReceta);
            } else {
                ivReceta.setImageResource(R.drawable.ic_recipe_placeholder);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMealClick(meal);
                }
            });
        }
    }
}

