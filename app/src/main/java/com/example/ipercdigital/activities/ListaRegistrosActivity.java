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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipercdigital.R;
import com.example.ipercdigital.adapters.RegistroAdapter;
import com.example.ipercdigital.api.ApiConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ListaRegistrosActivity extends AppCompatActivity {

    RecyclerView recycler;
    ProgressBar progress;
    TextView tvVacio;
    Button btnNuevo;
    List<JSONObject> registros = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_registros);

        recycler = findViewById(R.id.recyclerRegistros);
        progress = findViewById(R.id.progressLista);
        tvVacio  = findViewById(R.id.tvVacio);
        btnNuevo = findViewById(R.id.btnNuevoIperc);

        recycler.setLayoutManager(new LinearLayoutManager(this));

        btnNuevo.setOnClickListener(v ->
                startActivity(new Intent(this, IpercActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarRegistros();
    }

    private void cargarRegistros() {
        progress.setVisibility(View.VISIBLE);
        tvVacio.setVisibility(View.GONE);

        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
        String token = prefs.getString("token", "");

        new Thread(() -> {
            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/iperc/lista");
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
                registros.clear();
                for (int i = 0; i < array.length(); i++) {
                    registros.add(array.getJSONObject(i));
                }

                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    if (registros.isEmpty()) {
                        tvVacio.setVisibility(View.VISIBLE);
                    } else {
                        recycler.setAdapter(new RegistroAdapter(this, registros));
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    tvVacio.setText("Error al cargar registros.");
                    tvVacio.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }
}