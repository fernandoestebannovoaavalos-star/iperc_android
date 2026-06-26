package com.example.ipercdigital.activities;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ipercdigital.R;
import com.example.ipercdigital.api.ApiConfig;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class EstadisticasActivity extends AppCompatActivity {

    ProgressBar progress;
    TextView tvTotal, tvAprobados, tvPendientes, tvObservados;
    PieChart pieEstado;
    BarChart barNivel, barDias;
    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_estadisticas);

        progress      = findViewById(R.id.progressEstadisticas);
        tvTotal       = findViewById(R.id.tvTotalEstad);
        tvAprobados   = findViewById(R.id.tvAprobadosEstad);
        tvPendientes  = findViewById(R.id.tvPendientesEstad);
        tvObservados  = findViewById(R.id.tvObservadosEstad);
        pieEstado     = findViewById(R.id.pieEstado);
        barNivel      = findViewById(R.id.barNivel);
        barDias       = findViewById(R.id.barDias);

        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
        token = prefs.getString("token", "");

        cargarEstadisticas();
    }

    private void cargarEstadisticas() {
        progress.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/estadisticas");
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
                    mostrarEstadisticas(data);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al cargar estadísticas", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void mostrarEstadisticas(JSONObject data) {
        try {
            // ── Tarjetas resumen ──────────────────────────────
            int total = data.optInt("total", 0);
            tvTotal.setText(String.valueOf(total));

            int aprobados = 0, pendientes = 0, observados = 0;
            JSONArray porEstado = data.optJSONArray("por_estado");
            if (porEstado != null) {
                for (int i = 0; i < porEstado.length(); i++) {
                    JSONObject e = porEstado.getJSONObject(i);
                    switch (e.optString("estado")) {
                        case "aprobado":  aprobados  = e.optInt("total"); break;
                        case "pendiente": pendientes = e.optInt("total"); break;
                        case "observado": observados = e.optInt("total"); break;
                    }
                }
            }
            tvAprobados.setText(String.valueOf(aprobados));
            tvPendientes.setText(String.valueOf(pendientes));
            tvObservados.setText(String.valueOf(observados));

            // ── Pie chart — por estado ────────────────────────
            List<PieEntry> pieEntries = new ArrayList<>();
            if (aprobados > 0)  pieEntries.add(new PieEntry(aprobados,  "Aprobado"));
            if (pendientes > 0) pieEntries.add(new PieEntry(pendientes, "Pendiente"));
            if (observados > 0) pieEntries.add(new PieEntry(observados, "Observado"));

            if (!pieEntries.isEmpty()) {
                PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
                pieDataSet.setColors(
                        Color.parseColor("#4CAF50"),
                        Color.parseColor("#FF9800"),
                        Color.parseColor("#F44336"));
                pieDataSet.setValueTextSize(12f);
                pieDataSet.setValueTextColor(Color.WHITE);

                pieEstado.setData(new PieData(pieDataSet));
                pieEstado.setUsePercentValues(true);
                pieEstado.getDescription().setEnabled(false);
                pieEstado.setHoleRadius(40f);
                pieEstado.setTransparentCircleRadius(45f);
                pieEstado.setCenterText("Estados");
                pieEstado.setCenterTextSize(13f);
                pieEstado.animateY(800);
                pieEstado.invalidate();
            }

            // ── Bar chart — por nivel de riesgo ──────────────
            JSONArray porNivel = data.optJSONArray("por_nivel");
            if (porNivel != null && porNivel.length() > 0) {
                List<BarEntry> barEntries = new ArrayList<>();
                List<String> labels = new ArrayList<>();
                for (int i = 0; i < porNivel.length(); i++) {
                    JSONObject n = porNivel.getJSONObject(i);
                    barEntries.add(new BarEntry(i, n.optInt("total")));
                    labels.add(n.optString("nivel", "—"));
                }

                BarDataSet barDataSet = new BarDataSet(barEntries, "Registros por nivel");
                barDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                barDataSet.setValueTextSize(11f);

                barNivel.setData(new BarData(barDataSet));
                barNivel.getDescription().setEnabled(false);
                barNivel.getXAxis().setValueFormatter(
                        new IndexAxisValueFormatter(labels));
                barNivel.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                barNivel.getXAxis().setGranularity(1f);
                barNivel.getXAxis().setLabelRotationAngle(-30f);
                barNivel.getAxisRight().setEnabled(false);
                barNivel.animateY(800);
                barNivel.invalidate();
            }

            // ── Bar chart — últimos 7 días ────────────────────
            JSONArray porDia = data.optJSONArray("por_dia");
            if (porDia != null && porDia.length() > 0) {
                List<BarEntry> barEntries = new ArrayList<>();
                List<String> labels = new ArrayList<>();
                for (int i = 0; i < porDia.length(); i++) {
                    JSONObject d = porDia.getJSONObject(i);
                    barEntries.add(new BarEntry(i, d.optInt("total")));
                    labels.add(d.optString("dia", ""));
                }

                BarDataSet barDataSet = new BarDataSet(barEntries, "Registros por día");
                barDataSet.setColor(Color.parseColor("#1B5E20"));
                barDataSet.setValueTextSize(11f);

                barDias.setData(new BarData(barDataSet));
                barDias.getDescription().setEnabled(false);
                barDias.getXAxis().setValueFormatter(
                        new IndexAxisValueFormatter(labels));
                barDias.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                barDias.getXAxis().setGranularity(1f);
                barDias.getAxisRight().setEnabled(false);
                barDias.animateY(800);
                barDias.invalidate();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}