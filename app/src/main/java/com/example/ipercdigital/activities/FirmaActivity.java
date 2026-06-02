// FirmaActivity.java
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
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.example.ipercdigital.api.ApiConfig;

public class FirmaActivity extends AppCompatActivity {

    FirmaView firmaView;
    Button btnLimpiar, btnEnviar;
    ProgressBar progress;
    TextView tvEstado;
    int registroId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firma);

        firmaView  = findViewById(R.id.firmaView);
        btnLimpiar = findViewById(R.id.btnLimpiar);
        btnEnviar  = findViewById(R.id.btnEnviarFirma);
        progress   = findViewById(R.id.progressFirma);
        tvEstado   = findViewById(R.id.tvEstadoFirma);

        registroId = getIntent().getIntExtra("registro_id", -1);

        btnLimpiar.setOnClickListener(v -> firmaView.limpiar());

        btnEnviar.setOnClickListener(v -> {
            if (firmaView.estaVacia()) {
                Toast.makeText(this, "Por favor firme antes de continuar", Toast.LENGTH_SHORT).show();
                return;
            }
            enviarFirma();
        });
    }

    private void enviarFirma() {
        progress.setVisibility(View.VISIBLE);
        btnEnviar.setEnabled(false);
        tvEstado.setText("Enviando firma...");

        String base64Firma = firmaView.obtenerBase64();
        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
        String token = prefs.getString("token", "");

        new Thread(() -> {
            try {
                String apiUrl = ApiConfig.BASE_URL + "/api/iperc/firma";
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("registro_id", registroId);
                body.put("firma_base64", "data:image/png;base64," + base64Firma);

                byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
                OutputStream os = conn.getOutputStream();
                os.write(data);
                os.close();

                int code = conn.getResponseCode();

                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    if (code == 200) {
                        tvEstado.setText("✅ Firma guardada correctamente");
                        Toast.makeText(this, "IPERC enviado al supervisor", Toast.LENGTH_LONG).show();
                        // Ir a lista de registros
                        Intent intent = new Intent(this, IpercActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    } else {
                        btnEnviar.setEnabled(true);
                        tvEstado.setText("❌ Error al enviar. Intente nuevamente.");
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    btnEnviar.setEnabled(true);
                    tvEstado.setText("❌ Sin conexión al servidor");
                });
            }
        }).start();
    }
}