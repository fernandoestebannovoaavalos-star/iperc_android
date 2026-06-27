package com.example.ipercdigital.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipercdigital.R;

import org.json.JSONObject;

import java.util.List;

public class UsuarioAdapter extends RecyclerView.Adapter<UsuarioAdapter.VH> {

    public interface OnUsuarioActionListener {
        void onCambiarRol(int id, String nuevoRol);
        void onToggle(int id, boolean activo);
        void onResetear(int id, String nombre);
    }

    private final Context context;
    private final List<JSONObject> usuarios;
    private final OnUsuarioActionListener listener;
    private final String[] roles = {"trabajador", "supervisor", "admin"};

    public UsuarioAdapter(Context context, List<JSONObject> usuarios,
                          OnUsuarioActionListener listener) {
        this.context  = context;
        this.usuarios = usuarios;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_usuario, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        try {
            JSONObject u = usuarios.get(position);
            int id         = u.optInt("id");
            String nombre  = u.optString("nombre") + " " + u.optString("apellido");
            String dni     = "DNI: " + u.optString("dni");
            String rol     = u.optString("rol", "trabajador");
            boolean activo = u.optBoolean("activo", true);

            holder.tvNombre.setText(nombre);
            holder.tvDni.setText(dni);
            holder.tvEstado.setText(activo ? "ACTIVO" : "INACTIVO");
            holder.tvEstado.setBackgroundColor(activo ? 0xFF4CAF50 : 0xFF9E9E9E);

            // Spinner de rol
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_spinner_item, roles);
            adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item);
            holder.spinnerRol.setAdapter(adapter);

            for (int i = 0; i < roles.length; i++) {
                if (roles[i].equals(rol)) {
                    holder.spinnerRol.setSelection(i);
                    break;
                }
            }

            holder.btnCambiarRol.setOnClickListener(v ->
                    listener.onCambiarRol(id,
                            holder.spinnerRol.getSelectedItem().toString()));

            holder.btnResetear.setOnClickListener(v ->
                    listener.onResetear(id, nombre));

            holder.btnToggle.setText(activo ? "Desactivar" : "Activar");
            holder.btnToggle.setBackgroundColor(activo ? 0xFFF44336 : 0xFF4CAF50);
            holder.btnToggle.setOnClickListener(v -> listener.onToggle(id, activo));

        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public int getItemCount() { return usuarios.size(); }

    public static class VH extends RecyclerView.ViewHolder {
        TextView tvNombre, tvDni, tvEstado;
        Spinner spinnerRol;
        Button btnCambiarRol, btnToggle, btnResetear;

        public VH(@NonNull View v) {
            super(v);
            tvNombre      = v.findViewById(R.id.tvNombreUsuario);
            tvDni         = v.findViewById(R.id.tvDniUsuario);
            tvEstado      = v.findViewById(R.id.tvEstadoUsuario);
            spinnerRol    = v.findViewById(R.id.spinnerRolUsuario);
            btnCambiarRol = v.findViewById(R.id.btnCambiarRol);
            btnToggle     = v.findViewById(R.id.btnToggleUsuario);
            btnResetear   = v.findViewById(R.id.btnResetearClave);
        }
    }
}