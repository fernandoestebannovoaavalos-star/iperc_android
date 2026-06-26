package com.example.ipercdigital.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipercdigital.R;
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

public class ListaUsuariosActivity extends AppCompatActivity {

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

        cargarUsuarios();
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
                        recycler.setAdapter(new UsuarioAdapter());
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al cargar usuarios", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void cambiarRol(int id, String nuevoRol) {
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
                byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
                OutputStream os = conn.getOutputStream();
                os.write(data);
                os.close();

                int code = conn.getResponseCode();
                runOnUiThread(() -> {
                    if (code == 200) {
                        Toast.makeText(this, "✅ Rol actualizado", Toast.LENGTH_SHORT).show();
                        cargarUsuarios();
                    } else {
                        Toast.makeText(this, "Error al cambiar rol", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void toggleUsuario(int id, boolean activo) {
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
                        Toast.makeText(this, "Error al cambiar estado", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void mostrarDialogoResetear(int id, String nombre) {
        runOnUiThread(() -> {
            EditText etClave = new EditText(this);
            etClave.setHint("Nueva contraseña temporal (mín. 8 caracteres)");
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
                            Toast.makeText(this, "Mínimo 8 caracteres", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        resetearClave(id, nuevaClave);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
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
                byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
                OutputStream os = conn.getOutputStream();
                os.write(data);
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

    class UsuarioAdapter extends RecyclerView.Adapter<UsuarioAdapter.VH> {

        String[] roles = {"trabajador", "supervisor", "admin"};

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_usuario, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            try {
                JSONObject u = usuarios.get(position);
                int id          = u.optInt("id");
                String nombre   = u.optString("nombre") + " " + u.optString("apellido");
                String dni      = "DNI: " + u.optString("dni");
                String rol      = u.optString("rol", "trabajador");
                boolean activo  = u.optBoolean("activo", true);

                holder.tvNombre.setText(nombre);
                holder.tvDni.setText(dni);
                holder.tvEstado.setText(activo ? "ACTIVO" : "INACTIVO");
                holder.tvEstado.setBackgroundColor(activo ? 0xFF4CAF50 : 0xFF9E9E9E);

                // Spinner de rol
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        ListaUsuariosActivity.this,
                        android.R.layout.simple_spinner_item, roles);
                adapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item);
                holder.spinnerRol.setAdapter(adapter);

                // Seleccionar rol actual
                for (int i = 0; i < roles.length; i++) {
                    if (roles[i].equals(rol)) {
                        holder.spinnerRol.setSelection(i);
                        break;
                    }
                }

                // Botón cambiar rol
                holder.btnCambiarRol.setOnClickListener(v -> {
                    String nuevoRol = holder.spinnerRol.getSelectedItem().toString();
                    cambiarRol(id, nuevoRol);
                });

                // Botón resetear clave
                holder.btnResetear.setOnClickListener(v ->
                        mostrarDialogoResetear(id, nombre));

                // Botón activar/desactivar
                holder.btnToggle.setText(activo ? "Desactivar" : "Activar");
                holder.btnToggle.setBackgroundColor(activo ? 0xFFF44336 : 0xFF4CAF50);
                holder.btnToggle.setOnClickListener(v -> toggleUsuario(id, activo));

            } catch (Exception e) { e.printStackTrace(); }
        }

        @Override
        public int getItemCount() { return usuarios.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvNombre, tvDni, tvEstado;
            Spinner spinnerRol;
            Button btnCambiarRol, btnToggle, btnResetear;
            VH(View v) {
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
}