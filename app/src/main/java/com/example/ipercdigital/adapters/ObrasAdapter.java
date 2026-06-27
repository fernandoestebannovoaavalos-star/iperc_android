package com.example.ipercdigital.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipercdigital.R;

import org.json.JSONObject;

import java.util.List;

public class ObrasAdapter extends RecyclerView.Adapter<ObrasAdapter.VH> {

    public interface OnObraActionListener {
        void onEditar(JSONObject obra);
        void onToggle(int id, boolean activo);
    }

    private final Context context;
    private final List<JSONObject> obras;
    private final OnObraActionListener listener;

    public ObrasAdapter(Context context, List<JSONObject> obras,
                        OnObraActionListener listener) {
        this.context  = context;
        this.obras    = obras;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_obra, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        try {
            JSONObject o = obras.get(position);
            int id         = o.optInt("id");
            boolean activo = o.optBoolean("activo", true);

            holder.tvNombre.setText("🏗 " + o.optString("nombre", "—"));
            holder.tvEmpresa.setText("🏢 " + o.optString("empresa", "—"));
            holder.tvDireccion.setText("📍 " + o.optString("direccion", "—"));
            holder.tvRadio.setText("📡 Radio: " + o.optInt("radio", 100) + "m");
            holder.tvEstado.setText(activo ? "ACTIVA" : "INACTIVA");
            holder.tvEstado.setBackgroundColor(activo ? 0xFF4CAF50 : 0xFF9E9E9E);

            holder.btnEditar.setOnClickListener(v -> listener.onEditar(o));
            holder.btnToggle.setText(activo ? "Desactivar" : "Activar");
            holder.btnToggle.setBackgroundColor(activo ? 0xFFF44336 : 0xFF4CAF50);
            holder.btnToggle.setOnClickListener(v -> listener.onToggle(id, activo));

        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public int getItemCount() { return obras.size(); }

    public static class VH extends RecyclerView.ViewHolder {
        TextView tvNombre, tvEmpresa, tvDireccion, tvRadio, tvEstado;
        Button btnEditar, btnToggle;

        public VH(@NonNull View v) {
            super(v);
            tvNombre    = v.findViewById(R.id.tvObraNombre);
            tvEmpresa   = v.findViewById(R.id.tvObraEmpresa);
            tvDireccion = v.findViewById(R.id.tvObraDireccion);
            tvRadio     = v.findViewById(R.id.tvObraRadio);
            tvEstado    = v.findViewById(R.id.tvObraEstado);
            btnEditar   = v.findViewById(R.id.btnEditarObra);
            btnToggle   = v.findViewById(R.id.btnToggleObra);
        }
    }
}