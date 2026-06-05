package com.example.ipercdigital.activities;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import com.example.ipercdigital.api.ApiConfig;
import com.example.ipercdigital.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import android.content.Intent;
public class SupervisorActivity extends AppCompatActivity {

    RecyclerView recycler;
    ProgressBar progress;
    Button btnTabPendientes, btnTabTodos;
    List<JSONObject> registros = new ArrayList<>();
    String filtro = "pendiente";
    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supervisor);

        recycler           = findViewById(R.id.recyclerSupervisor);
        progress           = findViewById(R.id.progressSupervisor);
        btnTabPendientes   = findViewById(R.id.btnTabPendientes);
        btnTabTodos        = findViewById(R.id.btnTabTodos);

        recycler.setLayoutManager(new LinearLayoutManager(this));

        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
        token = prefs.getString("token", "");

        btnTabPendientes.setOnClickListener(v -> {
            filtro = "pendiente";
            actualizarTabs(true);
            cargarRegistros();
        });

        btnTabTodos.setOnClickListener(v -> {
            filtro = "todos";
            actualizarTabs(false);
            cargarRegistros();
        });

        cargarRegistros();
    }

    private void actualizarTabs(boolean pendientesActivo) {
        btnTabPendientes.setBackgroundColor(
                pendientesActivo ? 0xFF1B5E20 : 0xFFFFFFFF);
        btnTabPendientes.setTextColor(
                pendientesActivo ? 0xFFFFFFFF : 0xFF1B5E20);
        btnTabTodos.setBackgroundColor(
                pendientesActivo ? 0xFFFFFFFF : 0xFF1B5E20);
        btnTabTodos.setTextColor(
                pendientesActivo ? 0xFF1B5E20 : 0xFFFFFFFF);
    }

    private void cargarRegistros() {
        progress.setVisibility(View.VISIBLE);
        android.util.Log.d("SUPERVISOR", "Token enviado: [" + token + "]"); // ← agrega esto
        new Thread(() -> {
            try {
                String endpoint = filtro.equals("pendiente")
                        ? "/api/supervisor/pendientes"
                        : "/api/supervisor/todos";

                URL url = new URL(ApiConfig.BASE_URL + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONArray arr = new JSONArray(sb.toString());
                registros.clear();
                for (int i = 0; i < arr.length(); i++) {
                    registros.add(arr.getJSONObject(i));
                }

                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    recycler.setAdapter(new SupervisorAdapter());
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void aprobar(int registroId) {
        enviarDecision(registroId, "aprobar", null);
    }

    private void observar(int registroId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Observación");
        final EditText input = new EditText(this);
        input.setHint("Ingrese el motivo de la observación");
        builder.setView(input);
        builder.setPositiveButton("Enviar", (dialog, which) -> {
            String obs = input.getText().toString().trim();
            if (obs.isEmpty()) {
                Toast.makeText(this, "Debe ingresar una observación", Toast.LENGTH_SHORT).show();
            } else {
                enviarDecision(registroId, "observar", obs);
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void enviarDecision(int registroId, String accion, String observacion) {
        new Thread(() -> {
            try {
                String endpoint = "/api/supervisor/" + accion + "/" + registroId;
                URL url = new URL(ApiConfig.BASE_URL + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                if (observacion != null) body.put("observacion", observacion);

                byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
                OutputStream os = conn.getOutputStream();
                os.write(data);
                os.close();

                int code = conn.getResponseCode();
                runOnUiThread(() -> {
                    if (code == 200) {
                        String msg = accion.equals("aprobar") ? "✅ IPERC Aprobado" : "⚠ Observación enviada";
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                        cargarRegistros();
                    } else {
                        Toast.makeText(this, "Error al procesar", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Sin conexión", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void descargarPdf(int id) {
        Toast.makeText(this, "Descargando PDF...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(
                        ApiConfig.BASE_URL + "/api/iperc/pdf/" + id);
                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);

                if (conn.getResponseCode() != 200) {
                    runOnUiThread(() -> Toast.makeText(this,
                            "Error al descargar PDF", Toast.LENGTH_SHORT).show());
                    return;
                }

                java.io.InputStream is = conn.getInputStream();
                java.io.File archivo = new java.io.File(
                        getCacheDir(), "IPERC_" + id + ".pdf");
                java.io.FileOutputStream fos = new java.io.FileOutputStream(archivo);
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) fos.write(buffer, 0, len);
                fos.close();
                is.close();

                android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                        this, getPackageName() + ".provider", archivo);

                android.content.Intent intent = new android.content.Intent(
                        android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "application/pdf");
                intent.setFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);

                runOnUiThread(() -> {
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this,
                                "Instala un lector de PDF",
                                Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ---- Adapter ----
    class SupervisorAdapter extends RecyclerView.Adapter<SupervisorAdapter.VH> {

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_supervisor, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            try {
                JSONObject r = registros.get(position);
                int id = r.optInt("id");
                holder.tvTrabajador.setText("👷 " + r.optString("trabajador", "—"));
                holder.tvArea.setText(r.optString("area", "—") + " › " + r.optString("actividad", "—"));
                holder.tvFecha.setText("📅 " + r.optString("fecha", ""));
                holder.tvEstado.setText(r.optString("estado", "pendiente").toUpperCase());

                boolean esPendiente = "pendiente".equals(r.optString("estado"));
                boolean esAprobado  = "aprobado".equals(r.optString("estado"));

                holder.btnAprobar.setVisibility(esPendiente ? View.VISIBLE : View.GONE);
                holder.btnObservar.setVisibility(esPendiente ? View.VISIBLE : View.GONE);
                holder.btnPdf.setVisibility(esAprobado ? View.VISIBLE : View.GONE);

                holder.btnAprobar.setOnClickListener(v -> {
                    Intent intent = new Intent(SupervisorActivity.this, RevisarIpercActivity.class);
                    intent.putExtra("registro_id", id);
                    startActivity(intent);
                });
                holder.btnObservar.setOnClickListener(v -> observar(id));
                holder.btnPdf.setOnClickListener(v -> descargarPdf(id));

            } catch (Exception e) { /* ignora */ }
        }

        @Override
        public int getItemCount() { return registros.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTrabajador, tvArea, tvFecha, tvEstado;
            Button btnAprobar, btnObservar, btnPdf;
            VH(View v) {
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
}