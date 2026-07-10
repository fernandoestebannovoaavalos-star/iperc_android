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
            // Info general
            agregarInfoCard("📋 " + data.optString("codigo"),
                    "👷 " + data.optString("trabajador"));
            agregarInfoCard("🏗 " + data.optString("area"),
                    "⚙ " + data.optString("actividad"));
            agregarInfoCard("📅 " + data.optString("fecha"), null);

            boolean geoValidado = data.optBoolean("geo_validado");
            double lat = data.optDouble("lat", 0);
            double lon = data.optDouble("lon", 0);
            String gpsTexto;
            if (geoValidado && lat != 0 && lon != 0) {
                gpsTexto = "📍 GPS: ✓ Validado  |  Lat: " + String.format("%.6f", lat) +
                        "  Lon: " + String.format("%.6f", lon);
            } else {
                gpsTexto = "📍 GPS: Sin validar";
            }
            agregarInfoCard(gpsTexto, null);

            // Peligros identificados
            agregarTitulo("⚠ PELIGROS IDENTIFICADOS");
            JSONArray peligros = data.optJSONArray("peligros");
            if (peligros != null) {
                for (int i = 0; i < peligros.length(); i++) {
                    JSONObject p = peligros.getJSONObject(i);
                    agregarPeligroCard(
                            p.optString("descripcion"),
                            p.optString("riesgo"),
                            p.optString("nivel_sin"),
                            p.optString("medidas"),
                            p.optString("nivel_con"),
                            false
                    );
                }
            }

            // Peligros adicionales
            JSONArray adicionales = data.optJSONArray("adicionales");
            if (adicionales != null && adicionales.length() > 0) {
                agregarTitulo("⚠ PELIGROS ADICIONALES");
                for (int i = 0; i < adicionales.length(); i++) {
                    JSONObject p = adicionales.getJSONObject(i);
                    agregarPeligroCard(
                            p.optString("descripcion"),
                            p.optString("riesgo"),
                            p.optString("nivel_sin"),
                            p.optString("medidas"),
                            p.optString("nivel_con"),
                            true
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void agregarInfoCard(String linea1, String linea2) {
        android.widget.LinearLayout card = new android.widget.LinearLayout(this);
        card.setOrientation(android.widget.LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFFFFFFFF);
        card.setPadding(24, 16, 24, 16);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 8);
        card.setLayoutParams(lp);

        TextView tv1 = new TextView(this);
        tv1.setText(linea1);
        tv1.setTextSize(13);
        tv1.setTextColor(0xFF333333);
        card.addView(tv1);

        if (linea2 != null) {
            TextView tv2 = new TextView(this);
            tv2.setText(linea2);
            tv2.setTextSize(13);
            tv2.setTextColor(0xFF333333);
            tv2.setPadding(0, 4, 0, 0);
            card.addView(tv2);
        }
        contenedor.addView(card);
    }

    private void agregarTitulo(String texto) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextSize(15);
        tv.setTextColor(0xFF1B5E20);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 16, 0, 8);
        tv.setLayoutParams(lp);
        contenedor.addView(tv);
    }

    private void agregarPeligroCard(String descripcion, String riesgo,
                                    String nivelSin, String medidas,
                                    String nivelCon, boolean adicional) {
        android.widget.LinearLayout card = new android.widget.LinearLayout(this);
        card.setOrientation(android.widget.LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFFFFFFFF);
        card.setPadding(24, 16, 24, 16);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 10);
        card.setLayoutParams(lp);

        // Borde izquierdo de color
        android.view.View borde = new android.view.View(this);
        android.widget.FrameLayout.LayoutParams borderLp = new android.widget.FrameLayout.LayoutParams(6,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT);
        borde.setBackgroundColor(adicional ? 0xFFE65100 : 0xFF1B5E20);

        // Descripción
        TextView tvDesc = new TextView(this);
        tvDesc.setText("⚠ " + descripcion);
        tvDesc.setTextSize(14);
        tvDesc.setTextColor(0xFF212121);
        tvDesc.setTypeface(null, android.graphics.Typeface.BOLD);
        tvDesc.setPadding(0, 0, 0, 6);
        card.addView(tvDesc);

        // Riesgo
        TextView tvRiesgo = new TextView(this);
        tvRiesgo.setText("Riesgo: " + riesgo);
        tvRiesgo.setTextSize(13);
        tvRiesgo.setTextColor(0xFFE53935);
        tvRiesgo.setPadding(0, 0, 0, 6);
        card.addView(tvRiesgo);

        // Nivel sin control
        TextView tvSin = new TextView(this);
        tvSin.setText("Sin control: " + nivelSin);
        tvSin.setTextSize(12);
        tvSin.setTextColor(0xFF555555);
        tvSin.setPadding(0, 0, 0, 4);
        card.addView(tvSin);

        // Badge nivel sin control
        TextView badgeSin = new TextView(this);
        badgeSin.setText(nivelSin);
        badgeSin.setTextSize(11);
        badgeSin.setTextColor(0xFFFFFFFF);
        badgeSin.setTypeface(null, android.graphics.Typeface.BOLD);
        badgeSin.setPadding(20, 6, 20, 6);
        badgeSin.setBackgroundColor(getColorNivel(nivelSin));
        android.widget.LinearLayout.LayoutParams badgeLp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        badgeLp.setMargins(0, 0, 0, 8);
        badgeSin.setLayoutParams(badgeLp);
        card.addView(badgeSin);

        // Medidas de control
        TextView tvMedidas = new TextView(this);
        tvMedidas.setText("Control: " + medidas);
        tvMedidas.setTextSize(12);
        tvMedidas.setTextColor(0xFF1565C0);
        tvMedidas.setPadding(0, 0, 0, 4);
        card.addView(tvMedidas);

        // Nivel con control
        TextView tvCon = new TextView(this);
        tvCon.setText("Con control: " + nivelCon);
        tvCon.setTextSize(12);
        tvCon.setTextColor(0xFF555555);
        tvCon.setPadding(0, 0, 0, 4);
        card.addView(tvCon);

        // Badge nivel con control
        TextView badgeCon = new TextView(this);
        badgeCon.setText(nivelCon);
        badgeCon.setTextSize(11);
        badgeCon.setTextColor(0xFFFFFFFF);
        badgeCon.setTypeface(null, android.graphics.Typeface.BOLD);
        badgeCon.setPadding(20, 6, 20, 6);
        badgeCon.setBackgroundColor(getColorNivel(nivelCon));
        android.widget.LinearLayout.LayoutParams badgeConLp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        badgeConLp.setMargins(0, 0, 0, 4);
        badgeCon.setLayoutParams(badgeConLp);
        card.addView(badgeCon);

        contenedor.addView(card);
    }

    private int getColorNivel(String nivel) {
        if (nivel == null) return 0xFF9E9E9E;
        switch (nivel.toUpperCase()) {
            case "MUY BAJO":  return 0xFF4CAF50;
            case "BAJO":      return 0xFF8BC34A;
            case "MEDIO":     return 0xFFFF9800;
            case "ALTO":      return 0xFFFF5722;
            case "MUY ALTO":  return 0xFFF44336;
            case "EXTREMO":   return 0xFF880E4F;
            default:          return 0xFF9E9E9E;
        }
    }




}