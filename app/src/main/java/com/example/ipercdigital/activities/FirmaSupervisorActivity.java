package com.example.ipercdigital.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ipercdigital.R;
import com.example.ipercdigital.api.ApiConfig;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class FirmaSupervisorActivity extends AppCompatActivity {

    FirmaView firmaView;
    Button btnLimpiar, btnAprobar;
    ProgressBar progress;
    TextView tvEstado;
    int registroId;
    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firma_supervisor);

        registroId = getIntent().getIntExtra("registro_id", -1);

        firmaView  = findViewById(R.id.firmaViewSupervisor);
        btnLimpiar = findViewById(R.id.btnLimpiarSupervisor);
        btnAprobar = findViewById(R.id.btnAprobarConFirma);
        progress   = findViewById(R.id.progressFirmaSupervisor);
        tvEstado   = findViewById(R.id.tvEstadoFirmaSupervisor);

        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
        token = prefs.getString("token", "");

        btnLimpiar.setOnClickListener(v -> firmaView.limpiar());

        btnAprobar.setOnClickListener(v -> {
            if (firmaView.estaVacia()) {
                Toast.makeText(this, "Debe firmar antes de aprobar", Toast.LENGTH_SHORT).show();
                return;
            }
            aprobarConFirma();
        });
    }

    private void aprobarConFirma() {
        progress.setVisibility(View.VISIBLE);
        btnAprobar.setEnabled(false);
        tvEstado.setText("Procesando...");

        String base64 = firmaView.obtenerBase64();

        new Thread(() -> {
            try {
                String urlStr = ApiConfig.BASE_URL + "/api/supervisor/aprobar_con_firma/" + registroId;
                android.util.Log.d("FIRMA_SUP", "URL: " + urlStr);
                android.util.Log.d("FIRMA_SUP", "Token: " + token);
                android.util.Log.d("FIRMA_SUP", "RegistroId: " + registroId);

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(30000);

                JSONObject body = new JSONObject();
                body.put("firma_base64", "data:image/png;base64," + base64);

                byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
                OutputStream os = conn.getOutputStream();
                os.write(data);
                os.close();

                int code = conn.getResponseCode();
                android.util.Log.d("FIRMA_SUP", "Código respuesta: " + code);

                java.io.InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
                BufferedReader br = new BufferedReader(new java.io.InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                android.util.Log.d("FIRMA_SUP", "Respuesta: " + sb.toString());

                final int finalCode = code;
                final String respuesta = sb.toString();

                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    if (finalCode == 200) {
                        Toast.makeText(this, "✅ IPERC Aprobado correctamente", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(this, SupervisorActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    } else {
                        btnAprobar.setEnabled(true);
                        tvEstado.setText("❌ Código: " + finalCode + " | " + respuesta);
                    }
                });

            } catch (Exception e) {
                android.util.Log.e("FIRMA_SUP", "Excepción: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    btnAprobar.setEnabled(true);
                    tvEstado.setText("❌ Excepción: " + e.getMessage());
                });
            }
        }).start();
    }
}