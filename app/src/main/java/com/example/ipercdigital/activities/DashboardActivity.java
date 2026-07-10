package com.example.ipercdigital.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ipercdigital.R;
import com.example.ipercdigital.api.ApiConfig;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
        String nombre = prefs.getString("nombre", "Usuario");
        String rol = prefs.getString("rol", "");
        String token = prefs.getString("token", "");

        TextView tvBienvenido = findViewById(R.id.tvBienvenido);
        TextView tvRol = findViewById(R.id.tvRol);
        Button btnNuevoIperc = findViewById(R.id.btnNuevoIperc);
        Button btnListaIperc = findViewById(R.id.btnListaIperc);
        Button btnSupervisor = findViewById(R.id.btnSupervisor);
        Button btnCrearUsuario = findViewById(R.id.btnCrearUsuario);
        Button btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        Button btnMiPerfil = findViewById(R.id.btnMiPerfil);
        Button btnReportes = findViewById(R.id.btnReportes);
        Button btnEstadisticas = findViewById(R.id.btnEstadisticas);
        Button btnObras = findViewById(R.id.btnObras);
        TextView badgeSupervisor = findViewById(R.id.badgeSupervisor);
        FrameLayout frameSupervisor = findViewById(R.id.frameSupervisor);

        tvBienvenido.setText("Bienvenido, " + nombre);
        tvRol.setText("Rol: " + rol);

        btnMiPerfil.setOnClickListener(v ->
                startActivity(new Intent(this, PerfilActivity.class)));

        btnNuevoIperc.setOnClickListener(v ->
                startActivity(new Intent(this, IpercActivity.class)));

        btnListaIperc.setOnClickListener(v ->
                startActivity(new Intent(this, ListaRegistrosActivity.class)));

        btnReportes.setOnClickListener(v ->
                startActivity(new Intent(this, ReportesActivity.class)));

        // Supervisor y admin
        if (rol.equals("supervisor") || rol.equals("admin")) {
            frameSupervisor.setVisibility(View.VISIBLE);
            btnSupervisor.setOnClickListener(v ->
                    startActivity(new Intent(this, SupervisorActivity.class)));
            btnEstadisticas.setVisibility(View.VISIBLE);
            btnEstadisticas.setOnClickListener(v ->
                    startActivity(new Intent(this, EstadisticasActivity.class)));

            // Cargar badge de pendientes
            new Thread(() -> {
                try {
                    URL url = new URL(ApiConfig.BASE_URL + "/api/supervisor/pendientes");
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
                    int count = array.length();

                    runOnUiThread(() -> {
                        if (count > 0) {
                            badgeSupervisor.setText(String.valueOf(count));
                            badgeSupervisor.setVisibility(View.VISIBLE);
                            android.util.Log.d("BADGE", "Mostrando badge: " + count);
                        } else {
                            badgeSupervisor.setVisibility(View.GONE);
                            android.util.Log.d("BADGE", "Sin pendientes");
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        } else {
            frameSupervisor.setVisibility(View.GONE);
            btnEstadisticas.setVisibility(View.GONE);
        }

        // Solo admin
        if (rol.equals("admin")) {
            btnCrearUsuario.setVisibility(View.VISIBLE);
            btnCrearUsuario.setOnClickListener(v ->
                    startActivity(new Intent(this, ListaUsuariosActivity.class)));
            btnObras.setVisibility(View.VISIBLE);
            btnObras.setOnClickListener(v ->
                    startActivity(new Intent(this, ObrasActivity.class)));
        } else {
            btnCrearUsuario.setVisibility(View.GONE);
            btnObras.setVisibility(View.GONE);
        }

        btnCerrarSesion.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
        String rol   = prefs.getString("rol", "");
        String token = prefs.getString("token", "");

        TextView badgeSupervisor = findViewById(R.id.badgeSupervisor);

        if (rol.equals("supervisor") || rol.equals("admin")) {
            new Thread(() -> {
                try {
                    java.net.URL url = new java.net.URL(
                            ApiConfig.BASE_URL + "/api/supervisor/pendientes");
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
                    int count = array.length();

                    runOnUiThread(() -> {
                        if (count > 0) {
                            badgeSupervisor.setText(String.valueOf(count));
                            badgeSupervisor.setVisibility(View.VISIBLE);
                        } else {
                            badgeSupervisor.setVisibility(View.GONE);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }


}