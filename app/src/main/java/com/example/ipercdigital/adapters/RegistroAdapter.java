package com.example.ipercdigital.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipercdigital.R;
import com.example.ipercdigital.api.ApiConfig;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class RegistroAdapter extends RecyclerView.Adapter<RegistroAdapter.VH> {

    private final Context context;
    private final List<JSONObject> registros;

    public RegistroAdapter(Context context, List<JSONObject> registros) {
        this.context   = context;
        this.registros = registros;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_registro, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        try {
            JSONObject r = registros.get(position);
            String area        = r.optString("area", "—");
            String actividad   = r.optString("actividad", "—");
            String estado      = r.optString("estado", "pendiente");
            String fecha       = r.optString("fecha", "");
            String observacion = r.optString("observacion", "");

            holder.tvAreaActividad.setText(area + " › " + actividad);
            holder.tvFecha.setText("📅 " + fecha);
            holder.tvEstado.setText(estado.toUpperCase());

            int color;
            switch (estado.toLowerCase()) {
                case "aprobado":  color = 0xFF4CAF50; break;
                case "observado": color = 0xFFF44336; break;
                default:          color = 0xFFFF9800; break;
            }
            holder.tvEstado.setBackgroundColor(color);

            boolean aprobado  = "aprobado".equals(estado.toLowerCase());
            boolean observado = "observado".equals(estado.toLowerCase());

            holder.btnPdf.setVisibility(aprobado ? View.VISIBLE : View.GONE);
            holder.btnObservacion.setVisibility(observado ? View.VISIBLE : View.GONE);

            if (aprobado) {
                int regId = r.optInt("id");
                holder.btnPdf.setOnClickListener(v -> descargarPdf(regId));
            }

            if (observado) {
                holder.btnObservacion.setOnClickListener(v ->
                        mostrarObservacion(observacion));
            }

        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public int getItemCount() { return registros.size(); }

    private void mostrarObservacion(String observacion) {
        new AlertDialog.Builder(context)
                .setTitle("📋 Observación del Supervisor")
                .setMessage(observacion.isEmpty() ?
                        "El supervisor no dejó comentarios." : observacion)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void descargarPdf(int id) {
        SharedPreferences prefs = context.getSharedPreferences("iperc_prefs", Context.MODE_PRIVATE);
        String tkn = prefs.getString("token", "");
        Toast.makeText(context, "Descargando PDF...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/iperc/pdf/" + id);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + tkn);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);

                if (conn.getResponseCode() != 200) return;

                InputStream is = conn.getInputStream();
                File archivo = new File(context.getCacheDir(), "IPERC_" + id + ".pdf");
                FileOutputStream fos = new FileOutputStream(archivo);
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) fos.write(buffer, 0, len);
                fos.close();
                is.close();

                android.net.Uri uri = FileProvider.getUriForFile(
                        context, context.getPackageName() + ".provider", archivo);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "application/pdf");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                ((android.app.Activity) context).runOnUiThread(() -> {
                    try {
                        context.startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(context, "Instala un lector de PDF",
                                Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                ((android.app.Activity) context).runOnUiThread(() ->
                        Toast.makeText(context, "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    public static class VH extends RecyclerView.ViewHolder {
        TextView tvAreaActividad, tvEstado, tvFecha;
        Button btnPdf, btnObservacion;

        public VH(@NonNull View v) {
            super(v);
            tvAreaActividad = v.findViewById(R.id.tvAreaActividad);
            tvEstado        = v.findViewById(R.id.tvEstado);
            tvFecha         = v.findViewById(R.id.tvFecha);
            btnPdf          = v.findViewById(R.id.btnPdfRegistro);
            btnObservacion  = v.findViewById(R.id.btnVerObservacion);
        }
    }
}