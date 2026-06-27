package com.example.ipercdigital.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipercdigital.R;
import com.example.ipercdigital.adapters.ReportesAdapter;
import com.example.ipercdigital.api.ApiConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ReportesActivity extends AppCompatActivity {

    RecyclerView recycler;
    ProgressBar progress;
    TextView tvTotal, tvVacio;
    Spinner spinnerEstado;
    Button btnFiltrar, btnLimpiar;
    List<JSONObject> reportes = new ArrayList<>();
    String token;
    String estadoSeleccionado = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportes);

        recycler      = findViewById(R.id.recyclerReportes);
        progress      = findViewById(R.id.progressReportes);
        tvTotal       = findViewById(R.id.tvTotalReportes);
        tvVacio       = findViewById(R.id.tvVacioReportes);
        spinnerEstado = findViewById(R.id.spinnerEstado);
        btnFiltrar    = findViewById(R.id.btnFiltrar);
        btnLimpiar    = findViewById(R.id.btnLimpiarFiltros);

        recycler.setLayoutManager(new LinearLayoutManager(this));

        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
        token = prefs.getString("token", "");

        String[] estados = {"Todos", "pendiente", "aprobado", "observado"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, estados);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinnerEstado.setAdapter(adapter);

        spinnerEstado.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                estadoSeleccionado = position == 0 ? "" : estados[position];
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnFiltrar.setOnClickListener(v -> cargarReportes());
        btnLimpiar.setOnClickListener(v -> {
            spinnerEstado.setSelection(0);
            estadoSeleccionado = "";
            cargarReportes();
        });

        cargarReportes();
    }

    private void cargarReportes() {
        progress.setVisibility(View.VISIBLE);
        tvVacio.setVisibility(View.GONE);
        tvTotal.setText("");

        new Thread(() -> {
            try {
                String urlStr = ApiConfig.BASE_URL + "/api/reportes";
                if (!estadoSeleccionado.isEmpty()) {
                    urlStr += "?estado=" + estadoSeleccionado;
                }

                URL url = new URL(urlStr);
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
                reportes.clear();
                for (int i = 0; i < array.length(); i++) {
                    reportes.add(array.getJSONObject(i));
                }

                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    tvTotal.setText("Total: " + reportes.size() + " registros");
                    if (reportes.isEmpty()) {
                        tvVacio.setVisibility(View.VISIBLE);
                        recycler.setAdapter(null);
                    } else {
                        tvVacio.setVisibility(View.GONE);
                        recycler.setAdapter(new ReportesAdapter(this, reportes));
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al cargar reportes",
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}