package com.example.ipercdigital.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ipercdigital.R;
import com.example.ipercdigital.api.ApiConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RevisarIpercActivity extends AppCompatActivity {

    int registroId;
    String token;
    Button btnFirmarAprobar;
    ProgressBar progress;
    LinearLayout contenedor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revisar_iperc);

        registroId       = getIntent().getIntExtra("registro_id", -1);
        contenedor       = findViewById(R.id.contenedorDetalle);
        progress         = findViewById(R.id.progressRevisar);
        btnFirmarAprobar = findViewById(R.id.btnFirmarAprobar);

        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
        token = prefs.getString("token", "");

        btnFirmarAprobar.setVisibility(View.GONE);
        cargarDetalle();

        btnFirmarAprobar.setOnClickListener(v -> {
            Intent intent = new Intent(this, FirmaSupervisorActivity.class);
            intent.putExtra("registro_id", registroId);
            startActivity(intent);
            finish();
        });
    }

    private void cargarDetalle() {
        progress.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/iperc/detalle/" + registroId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject data = new JSONObject(sb.toString());

                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    mostrarDetalle(data);
                    btnFirmarAprobar.setVisibility(View.VISIBLE);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al cargar detalle", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void mostrarDetalle(JSONObject data) {
        try {
            agregarTexto("📋 Código: " + data.optString("codigo"), 16, true);
            agregarTexto("👷 Trabajador: " + data.optString("trabajador"), 15, false);
            agregarTexto("🏗 Área: " + data.optString("area"), 15, false);
            agregarTexto("⚙ Actividad: " + data.optString("actividad"), 15, false);
            agregarTexto("📅 Fecha: " + data.optString("fecha"), 14, false);
            agregarTexto("📍 GPS: " + (data.optBoolean("geo_validado") ? "✓ Validado" : "Sin validar"), 14, false);

            agregarSeparador();
            agregarTexto("⚠ PELIGROS IDENTIFICADOS", 15, true);

            JSONArray peligros = data.optJSONArray("peligros");
            if (peligros != null) {
                for (int i = 0; i < peligros.length(); i++) {
                    JSONObject p = peligros.getJSONObject(i);
                    agregarTexto("• " + p.optString("descripcion"), 14, false);
                    agregarTexto("  Riesgo: " + p.optString("riesgo"), 13, false);
                    agregarTexto("  Sin control: " + p.optString("nivel_sin") +
                            " → Con control: " + p.optString("nivel_con"), 13, false);
                    agregarTexto("  Control: " + p.optString("medidas"), 13, false);
                    agregarSeparador();
                }
            }

            JSONArray adicionales = data.optJSONArray("adicionales");
            if (adicionales != null && adicionales.length() > 0) {
                agregarSeparador();
                agregarTexto("⚠ PELIGROS ADICIONALES", 15, true);
                for (int i = 0; i < adicionales.length(); i++) {
                    JSONObject p = adicionales.getJSONObject(i);
                    agregarTexto("• " + p.optString("descripcion"), 14, false);
                    agregarTexto("  Riesgo: " + p.optString("riesgo"), 13, false);
                    agregarTexto("  Sin control: " + p.optString("nivel_sin") +
                            " → Con control: " + p.optString("nivel_con"), 13, false);
                    agregarTexto("  Control: " + p.optString("medidas"), 13, false);
                    agregarSeparador();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void agregarTexto(String texto, int size, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextSize(size);
        tv.setTextColor(bold ? 0xFF212121 : 0xFF555555);
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(0, 6, 0, 6);
        contenedor.addView(tv);
    }

    private void agregarSeparador() {
        View sep = new View(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        p.setMargins(0, 8, 0, 8);
        sep.setLayoutParams(p);
        sep.setBackgroundColor(0xFFDDDDDD);
        contenedor.addView(sep);
    }
}