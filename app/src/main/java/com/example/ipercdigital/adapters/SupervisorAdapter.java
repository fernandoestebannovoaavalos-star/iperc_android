package com.example.ipercdigital.adapters;

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
import com.example.ipercdigital.activities.RevisarIpercActivity;
import com.example.ipercdigital.api.ApiConfig;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class SupervisorAdapter extends RecyclerView.Adapter<SupervisorAdapter.VH> {

    public interface OnDecisionListener {
        void onObservar(int id);
    }

    private final Context context;
    private final List<JSONObject> registros;
    private final OnDecisionListener listener;

    public SupervisorAdapter(Context context, List<JSONObject> registros,
                             OnDecisionListener listener) {
        this.context   = context;
        this.registros = registros;
        this.listener  = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_supervisor, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        try {
            JSONObject r = registros.get(position);
            int id = r.optInt("id");

            holder.tvTrabajador.setText("👷 " + r.optString("trabajador", "—"));
            holder.tvArea.setText(r.optString("area", "—") + " › " +
                    r.optString("actividad", "—"));
            holder.tvFecha.setText("📅 " + r.optString("fecha", ""));
            holder.tvEstado.setText(r.optString("estado", "pendiente").toUpperCase());

            boolean esPendiente = "pendiente".equals(r.optString("estado"));
            boolean esAprobado  = "aprobado".equals(r.optString("estado"));

            holder.btnAprobar.setVisibility(esPendiente ? View.VISIBLE : View.GONE);
            holder.btnObservar.setVisibility(esPendiente ? View.VISIBLE : View.GONE);
            holder.btnPdf.setVisibility(esAprobado ? View.VISIBLE : View.GONE);

            holder.btnAprobar.setOnClickListener(v -> {
                Intent intent = new Intent(context, RevisarIpercActivity.class);
                intent.putExtra("registro_id", id);
                context.startActivity(intent);
            });

            holder.btnObservar.setOnClickListener(v -> listener.onObservar(id));

            holder.btnPdf.setOnClickListener(v -> descargarPdf(id));

        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public int getItemCount() { return registros.size(); }

    private void descargarPdf(int id) {
        SharedPreferences prefs = context.getSharedPreferences(
                "iperc_prefs", Context.MODE_PRIVATE);
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
        TextView tvTrabajador, tvArea, tvFecha, tvEstado;
        Button btnAprobar, btnObservar, btnPdf;

        public VH(@NonNull View v) {
            super(v);
            tvTrabajador = v.findViewById(R.id.tvTrabajador);
            tvArea       = v.findViewById(R.id.tvArea);
            tvFecha      = v.findViewById(R.id.tvFecha);
            tvEstado     = v.findViewById(R.id.tvEstado);
            btnAprobar   = v.findViewById(R.id.btnAprobar);
            btnObservar  = v.findViewById(R.id.btnObservar);
            btnPdf       = v.findViewById(R.id.btnPdf);
        }
    }
}