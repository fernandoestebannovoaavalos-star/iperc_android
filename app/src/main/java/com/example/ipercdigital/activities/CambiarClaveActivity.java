package com.example.ipercdigital.activities;

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

import com.example.ipercdigital.R;
import com.example.ipercdigital.api.ApiConfig;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class CambiarClaveActivity extends AppCompatActivity {

    EditText etNuevaClave, etConfirmarClave;
    Button btnGuardar;
    ProgressBar progress;
    TextView tvError;
    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cambiar_clave);

        etNuevaClave    = findViewById(R.id.etNuevaClave);
        etConfirmarClave = findViewById(R.id.etConfirmarClave);
        btnGuardar      = findViewById(R.id.btnGuardarClave);
        progress        = findViewById(R.id.progressClave);
        tvError         = findViewById(R.id.tvErrorClave);

        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
        token = prefs.getString("token", "");

        // No permitir retroceder
        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        Toast.makeText(CambiarClaveActivity.this,
                                "Debe cambiar su contraseña para continuar",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        btnGuardar.setOnClickListener(v -> guardarClave());
    }

    private void guardarClave() {
        String nueva    = etNuevaClave.getText().toString().trim();
        String confirma = etConfirmarClave.getText().toString().trim();

        if (nueva.isEmpty() || confirma.isEmpty()) {
            tvError.setText("❌ Complete todos los campos");
            tvError.setTextColor(0xFFF44336);
            return;
        }
        if (nueva.length() < 8) {
            tvError.setText("❌ La contraseña debe tener al menos 8 caracteres");
            tvError.setTextColor(0xFFF44336);
            return;
        }
        if (!nueva.equals(confirma)) {
            tvError.setText("❌ Las contraseñas no coinciden");
            tvError.setTextColor(0xFFF44336);
            return;
        }

        progress.setVisibility(View.VISIBLE);
        btnGuardar.setEnabled(false);
        tvError.setText("");

        new Thread(() -> {
            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/cambiar_clave");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("nueva_clave", nueva);

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
                    btnGuardar.setEnabled(true);
                    if (code == 200) {
                        Toast.makeText(this,
                                "✅ Contraseña actualizada", Toast.LENGTH_SHORT).show();
                        // Ir al Dashboard
                        startActivity(new Intent(this, DashboardActivity.class));
                        finish();
                    } else {
                        tvError.setText("❌ " + resp.optString("error"));
                        tvError.setTextColor(0xFFF44336);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    btnGuardar.setEnabled(true);
                    tvError.setText("❌ Error: " + e.getMessage());
                    tvError.setTextColor(0xFFF44336);
                });
            }
        }).start();
    }
}