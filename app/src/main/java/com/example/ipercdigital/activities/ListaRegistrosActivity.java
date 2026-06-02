package com.example.ipercdigital.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import com.example.ipercdigital.api.ApiConfig;
import com.example.ipercdigital.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ListaRegistrosActivity extends AppCompatActivity {

    RecyclerView recycler;
    ProgressBar progress;
    TextView tvVacio;
    Button btnNuevo;
    List<JSONObject> registros = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_registros);

        recycler  = findViewById(R.id.recyclerRegistros);
        progress  = findViewById(R.id.progressLista);
        tvVacio   = findViewById(R.id.tvVacio);
        btnNuevo  = findViewById(R.id.btnNuevoIperc);

        recycler.setLayoutManager(new LinearLayoutManager(this));

        btnNuevo.setOnClickListener(v -> {
            startActivity(new Intent(this, IpercActivity.class));
        });

        cargarRegistros();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarRegistros(); // Recarga al volver de crear uno nuevo
    }

    private void cargarRegistros() {
        progress.setVisibility(View.VISIBLE);
        tvVacio.setVisibility(View.GONE);

        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
        String token = prefs.getString("token", "");

        new Thread(() -> {
            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/iperc/lista");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONArray array = new JSONArray(sb.toString());
                registros.clear();
                for (int i = 0; i < array.length(); i++) {
                    registros.add(array.getJSONObject(i));
                }

                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    if (registros.isEmpty()) {
                        tvVacio.setVisibility(View.VISIBLE);
                    } else {
                        recycler.setAdapter(new RegistroAdapter());
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    tvVacio.setText("Error al cargar registros. Verifique su conexión.");
                    tvVacio.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    class RegistroAdapter extends RecyclerView.Adapter<RegistroAdapter.VH> {

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_registro, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            try {
                JSONObject r = registros.get(position);
                String area = r.optString("area", "—");
                String actividad = r.optString("actividad", "—");
                String estado = r.optString("estado", "pendiente");
                String fecha = r.optString("fecha", "");
                String nivel = r.optString("nivel_riesgo", "");

                holder.tvAreaActividad.setText(area + " › " + actividad);
                holder.tvFecha.setText("📅 " + fecha);
                holder.tvNivelRiesgo.setText("⚠ Nivel: " + nivel);
                holder.tvEstado.setText(estado.toUpperCase());

                // Color del badge según estado
                int color;
                switch (estado.toLowerCase()) {
                    case "aprobado":  color = 0xFF4CAF50; break;
                    case "observado": color = 0xFFF44336; break;
                    default:          color = 0xFFFF9800; break;
                }
                holder.tvEstado.setBackgroundColor(color);

            } catch (Exception e) { /* ignora */ }
        }

        @Override
        public int getItemCount() { return registros.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvAreaActividad, tvEstado, tvFecha, tvNivelRiesgo;
            VH(View v) {
                super(v);
                tvAreaActividad = v.findViewById(R.id.tvAreaActividad);
                tvEstado        = v.findViewById(R.id.tvEstado);
                tvFecha         = v.findViewById(R.id.tvFecha);
                tvNivelRiesgo   = v.findViewById(R.id.tvNivelRiesgo);
            }
        }
    }
}