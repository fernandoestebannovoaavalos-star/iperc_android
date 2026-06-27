package com.example.ipercdigital.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipercdigital.R;
import com.example.ipercdigital.adapters.UsuarioAdapter;
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

public class ListaUsuariosActivity extends AppCompatActivity
        implements UsuarioAdapter.OnUsuarioActionListener {

    RecyclerView recycler;
    ProgressBar progress;
    TextView tvVacio;
    Button btnAgregar;
    List<JSONObject> usuarios = new ArrayList<>();
    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_usuarios);

        recycler   = findViewById(R.id.recyclerUsuarios);
        progress   = findViewById(R.id.progressUsuarios);
        tvVacio    = findViewById(R.id.tvVacioUsuarios);
        btnAgregar = findViewById(R.id.btnAgregarUsuario);

        recycler.setLayoutManager(new LinearLayoutManager(this));

        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
        token = prefs.getString("token", "");

        btnAgregar.setOnClickListener(v ->
                startActivity(new Intent(this, CrearUsuarioActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarUsuarios();
    }

    private void cargarUsuarios() {
        progress.setVisibility(View.VISIBLE);
        tvVacio.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/admin/usuarios");
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
                usuarios.clear();
                for (int i = 0; i < array.length(); i++) {
                    usuarios.add(array.getJSONObject(i));
                }

                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    if (usuarios.isEmpty()) {
                        tvVacio.setVisibility(View.VISIBLE);
                    } else {
                        recycler.setAdapter(new UsuarioAdapter(this, usuarios, this));
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al cargar usuarios",
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    public void onCambiarRol(int id, String nuevoRol) {
        new Thread(() -> {
            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/admin/cambiar_rol/" + id);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("rol", nuevoRol);
                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                os.close();

                int code = conn.getResponseCode();
                runOnUiThread(() -> {
                    if (code == 200) {
                        Toast.makeText(this, "✅ Rol actualizado",
                                Toast.LENGTH_SHORT).show();
                        cargarUsuarios();
                    } else {
                        Toast.makeText(this, "Error al cambiar rol",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    public void onToggle(int id, boolean activo) {
        new Thread(() -> {
            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/admin/toggle_usuario/" + id);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);
                conn.getOutputStream().close();

                int code = conn.getResponseCode();
                runOnUiThread(() -> {
                    if (code == 200) {
                        String msg = activo ? "Usuario desactivado" : "Usuario activado";
                        Toast.makeText(this, "✅ " + msg, Toast.LENGTH_SHORT).show();
                        cargarUsuarios();
                    } else {
                        Toast.makeText(this, "Error al cambiar estado",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    public void onResetear(int id, String nombre) {
        EditText etClave = new EditText(this);
        etClave.setHint("Nueva contraseña (mín. 8 caracteres)");
        etClave.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etClave.setPadding(40, 20, 40, 20);

        new AlertDialog.Builder(this)
                .setTitle("Resetear contraseña")
                .setMessage("Usuario: " + nombre)
                .setView(etClave)
                .setPositiveButton("Resetear", (dialog, which) -> {
                    String nuevaClave = etClave.getText().toString().trim();
                    if (nuevaClave.length() < 8) {
                        Toast.makeText(this, "Mínimo 8 caracteres",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    resetearClave(id, nuevaClave);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void resetearClave(int id, String nuevaClave) {
        new Thread(() -> {
            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/admin/resetear_clave/" + id);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("nueva_clave", nuevaClave);
                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                os.close();

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        code >= 400 ? conn.getErrorStream() : conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject resp = new JSONObject(sb.toString());
                runOnUiThread(() -> {
                    if (code == 200) {
                        Toast.makeText(this, "✅ " + resp.optString("mensaje"),
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "❌ " + resp.optString("error"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}