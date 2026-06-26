package com.example.ipercdigital.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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
import java.nio.charset.StandardCharsets;

public class PerfilActivity extends AppCompatActivity {

    EditText etClaveActual, etNuevaClave, etConfirmarClave;
    Button btnGuardar;
    ProgressBar progress;
    TextView tvResultado, tvNombre, tvDni, tvRol;
    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        etClaveActual   = findViewById(R.id.etClaveActual);
        etNuevaClave    = findViewById(R.id.etNuevaClaveP);
        etConfirmarClave = findViewById(R.id.etConfirmarClaveP);
        btnGuardar      = findViewById(R.id.btnGuardarCambio);
        progress        = findViewById(R.id.progressPerfil);
        tvResultado     = findViewById(R.id.tvResultadoPerfil);
        tvNombre        = findViewById(R.id.tvNombrePerfil);
        tvDni           = findViewById(R.id.tvDniPerfil);
        tvRol           = findViewById(R.id.tvRolPerfil);

        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
        token = prefs.getString("token", "");

        // Mostrar datos del usuario
        tvNombre.setText(prefs.getString("nombre", ""));
        tvDni.setText("DNI: " + prefs.getString("dni", ""));
        tvRol.setText("Rol: " + prefs.getString("rol", ""));

        btnGuardar.setOnClickListener(v -> cambiarClave());
    }

    private void cambiarClave() {
        String actual    = etClaveActual.getText().toString().trim();
        String nueva     = etNuevaClave.getText().toString().trim();
        String confirma  = etConfirmarClave.getText().toString().trim();

        if (actual.isEmpty() || nueva.isEmpty() || confirma.isEmpty()) {
            tvResultado.setText("❌ Complete todos los campos");
            tvResultado.setTextColor(0xFFF44336);
            return;
        }
        if (nueva.length() < 8) {
            tvResultado.setText("❌ La contraseña debe tener al menos 8 caracteres");
            tvResultado.setTextColor(0xFFF44336);
            return;
        }
        if (!nueva.equals(confirma)) {
            tvResultado.setText("❌ Las contraseñas no coinciden");
            tvResultado.setTextColor(0xFFF44336);
            return;
        }

        progress.setVisibility(View.VISIBLE);
        btnGuardar.setEnabled(false);
        tvResultado.setText("");

        new Thread(() -> {
            try {
                HttpURLConnection conn = ApiConfig.getConnection("/api/cambiar_clave");
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("clave_actual", actual);
                body.put("nueva_clave", nueva);

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
                    progress.setVisibility(View.GONE);
                    btnGuardar.setEnabled(true);
                    if (code == 200) {
                        tvResultado.setText("✅ Contraseña actualizada correctamente");
                        tvResultado.setTextColor(0xFF4CAF50);
                        etClaveActual.setText("");
                        etNuevaClave.setText("");
                        etConfirmarClave.setText("");
                        Toast.makeText(this, "✅ Contraseña actualizada", Toast.LENGTH_SHORT).show();
                    } else {
                        tvResultado.setText("❌ " + resp.optString("error"));
                        tvResultado.setTextColor(0xFFF44336);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    btnGuardar.setEnabled(true);
                    tvResultado.setText("❌ Error: " + e.getMessage());
                    tvResultado.setTextColor(0xFFF44336);
                });
            }
        }).start();
    }
}