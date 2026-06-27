package com.example.ipercdigital.activities;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipercdigital.R;
import com.example.ipercdigital.adapters.SupervisorAdapter;
import com.example.ipercdigital.api.ApiConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SupervisorActivity extends AppCompatActivity
        implements SupervisorAdapter.OnDecisionListener {

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

        recycler         = findViewById(R.id.recyclerSupervisor);
        progress         = findViewById(R.id.progressSupervisor);
        btnTabPendientes = findViewById(R.id.btnTabPendientes);
        btnTabTodos      = findViewById(R.id.btnTabTodos);

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
                    recycler.setAdapter(new SupervisorAdapter(this, registros, this));
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al cargar datos",
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    public void onObservar(int registroId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Observación");
        final EditText input = new EditText(this);
        input.setHint("Ingrese el motivo de la observación");
        builder.setView(input);
        builder.setPositiveButton("Enviar", (dialog, which) -> {
            String obs = input.getText().toString().trim();
            if (obs.isEmpty()) {
                Toast.makeText(this, "Debe ingresar una observación",
                        Toast.LENGTH_SHORT).show();
            } else {
                enviarObservacion(registroId, obs);
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void enviarObservacion(int registroId, String observacion) {
        new Thread(() -> {
            try {
                URL url = new URL(ApiConfig.BASE_URL +
                        "/api/supervisor/observar/" + registroId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("observacion", observacion);
                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                os.close();

                int code = conn.getResponseCode();
                runOnUiThread(() -> {
                    if (code == 200) {
                        Toast.makeText(this, "⚠ Observación enviada",
                                Toast.LENGTH_SHORT).show();
                        cargarRegistros();
                    } else {
                        Toast.makeText(this, "Error al procesar",
                                Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Sin conexión", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}