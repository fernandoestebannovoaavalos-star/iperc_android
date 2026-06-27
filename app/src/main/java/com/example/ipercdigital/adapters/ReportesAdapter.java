package com.example.ipercdigital.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipercdigital.R;

import org.json.JSONObject;

import java.util.List;

public class ReportesAdapter extends RecyclerView.Adapter<ReportesAdapter.VH> {

    private final Context context;
    private final List<JSONObject> reportes;

    public ReportesAdapter(Context context, List<JSONObject> reportes) {
        this.context  = context;
        this.reportes = reportes;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reporte, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        try {
            JSONObject r = reportes.get(position);

            holder.tvCodigo.setText("📋 " + r.optString("codigo", "—"));
            holder.tvTrabajador.setText("👷 " + r.optString("trabajador", "—"));
            holder.tvAreaActividad.setText(
                    r.optString("area", "—") + " › " + r.optString("actividad", "—"));
            holder.tvFecha.setText("📅 " + r.optString("fecha", ""));
            holder.tvObra.setText("🏗 " + r.optString("obra", "Sin obra"));
            holder.tvNivel.setText("⚠ " + r.optString("nivel_riesgo", "—"));

            String estado = r.optString("estado", "pendiente");
            holder.tvEstado.setText(estado.toUpperCase());

            int color;
            switch (estado.toLowerCase()) {
                case "aprobado":  color = 0xFF4CAF50; break;
                case "observado": color = 0xFFF44336; break;
                default:          color = 0xFFFF9800; break;
            }
            holder.tvEstado.setBackgroundColor(color);

        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public int getItemCount() { return reportes.size(); }

    public static class VH extends RecyclerView.ViewHolder {
        TextView tvCodigo, tvTrabajador, tvAreaActividad,
                tvFecha, tvObra, tvNivel, tvEstado;

        public VH(@NonNull View v) {
            super(v);
            tvCodigo        = v.findViewById(R.id.tvCodigoReporte);
            tvTrabajador    = v.findViewById(R.id.tvTrabajadorReporte);
            tvAreaActividad = v.findViewById(R.id.tvAreaActividadReporte);
            tvFecha         = v.findViewById(R.id.tvFechaReporte);
            tvObra          = v.findViewById(R.id.tvObraReporte);
            tvNivel         = v.findViewById(R.id.tvNivelReporte);
            tvEstado        = v.findViewById(R.id.tvEstadoReporte);
        }
    }
}