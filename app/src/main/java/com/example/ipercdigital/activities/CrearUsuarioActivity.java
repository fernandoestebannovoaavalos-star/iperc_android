package com.example.ipercdigital.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ipercdigital.R;
import com.example.ipercdigital.api.ApiConfig;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class CrearUsuarioActivity extends AppCompatActivity {

    EditText etNombre, etApellido, etDni, etEmail, etPassword;
    Spinner spinnerRol;
    Button btnCrear;
    ProgressBar progress;
    TextView tvResultado;
    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_usuario);

        etNombre    = findViewById(R.id.etNombre);
        etApellido  = findViewById(R.id.etApellido);
        etDni       = findViewById(R.id.etDni);
        etPassword  = findViewById(R.id.etPassword);
        etEmail = findViewById(R.id.etEmail);
        spinnerRol  = findViewById(R.id.spinnerRol);
        btnCrear    = findViewById(R.id.btnCrearUsuario);
        progress    = findViewById(R.id.progressCrear);
        tvResultado = findViewById(R.id.tvResultado);

        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
        token = prefs.getString("token", "");

        // Roles disponibles
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"trabajador", "supervisor", "admin"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRol.setAdapter(adapter);

        btnCrear.setOnClickListener(v -> crearUsuario());
    }

    private void crearUsuario() {
        String nombre   = etNombre.getText().toString().trim();
        String apellido = etApellido.getText().toString().trim();
        String dni      = etDni.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String rol      = spinnerRol.getSelectedItem().toString();
        String email = etEmail.getText().toString().trim();

        // Validaciones
        if (nombre.isEmpty() || apellido.isEmpty() || dni.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dni.length() != 8) {
            Toast.makeText(this, "El DNI debe tener 8 dígitos", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 8) {
            Toast.makeText(this, "La contraseña debe tener al menos 8 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        progress.setVisibility(View.VISIBLE);
        btnCrear.setEnabled(false);
        tvResultado.setText("");

        new Thread(() -> {
            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/admin/crear_usuario");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("nombre", nombre);
                body.put("apellido", apellido);
                body.put("dni", dni);
                body.put("password", password);
                body.put("rol", rol);
                body.put("email", email.isEmpty() ? JSONObject.NULL : email);
                body.put("debe_cambiar_clave", true);

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
                    progress.setVisibility(View.GONE);
                    btnCrear.setEnabled(true);
                    if (code == 201) {
                        tvResultado.setText("✅ " + resp.optString("mensaje"));
                        tvResultado.setTextColor(0xFF4CAF50);
                        // Limpiar campos
                        etNombre.setText("");
                        etApellido.setText("");
                        etDni.setText("");
                        etEmail.setText("");
                        etPassword.setText("");
                        spinnerRol.setSelection(0);
                    } else {
                        tvResultado.setText("❌ " + resp.optString("error"));
                        tvResultado.setTextColor(0xFFF44336);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    btnCrear.setEnabled(true);
                    tvResultado.setText("❌ Error: " + e.getMessage());
                    tvResultado.setTextColor(0xFFF44336);
                });
            }
        }).start();
    }
}