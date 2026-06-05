package com.example.ipercdigital.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ipercdigital.R;
import com.example.ipercdigital.api.ApiClient;
import com.example.ipercdigital.api.ApiConfig;
import com.example.ipercdigital.models.Actividad;
import com.example.ipercdigital.models.Area;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IpercActivity extends AppCompatActivity {

    private Spinner spinnerArea, spinnerActividad;
    private Button btnSiguiente;
    private ProgressBar progressBar;
    private String token;
    private List<Area> listaAreas = new ArrayList<>();
    private List<Actividad> listaActividades = new ArrayList<>();
    private boolean yaGuardo = false; // ← evita doble registro

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iperc);

        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
        token = "Bearer " + prefs.getString("token", "");

        spinnerArea      = findViewById(R.id.spinnerArea);
        spinnerActividad = findViewById(R.id.spinnerActividad);
        btnSiguiente     = findViewById(R.id.btnSiguiente);
        progressBar      = findViewById(R.id.progressBar);

        cargarAreas();

        spinnerArea.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!listaAreas.isEmpty()) {
                    cargarActividades(listaAreas.get(position).getId());
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSiguiente.setOnClickListener(v -> {
            if (listaAreas.isEmpty() || listaActividades.isEmpty()) {
                Toast.makeText(this, "Selecciona área y actividad", Toast.LENGTH_SHORT).show();
                return;
            }
            yaGuardo = false;
            solicitarGPS();
        });
    }

    private void cargarAreas() {
        progressBar.setVisibility(View.VISIBLE);
        ApiClient.getService().getAreas(token).enqueue(new Callback<List<Area>>() {
            @Override
            public void onResponse(Call<List<Area>> call, Response<List<Area>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    listaAreas = response.body();
                    List<String> nombres = new ArrayList<>();
                    for (Area a : listaAreas) nombres.add(a.getNombre());
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            IpercActivity.this,
                            android.R.layout.simple_spinner_item, nombres);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerArea.setAdapter(adapter);
                }
            }
            @Override
            public void onFailure(Call<List<Area>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(IpercActivity.this,
                        "Error cargando áreas: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void cargarActividades(int areaId) {
        ApiClient.getService().getActividades(token, areaId).enqueue(new Callback<List<Actividad>>() {
            @Override
            public void onResponse(Call<List<Actividad>> call, Response<List<Actividad>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listaActividades = response.body();
                    List<String> nombres = new ArrayList<>();
                    for (Actividad a : listaActividades) nombres.add(a.getNombre());
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            IpercActivity.this,
                            android.R.layout.simple_spinner_item, nombres);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerActividad.setAdapter(adapter);
                }
            }
            @Override
            public void onFailure(Call<List<Actividad>> call, Throwable t) {
                Toast.makeText(IpercActivity.this,
                        "Error cargando actividades: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void solicitarGPS() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }
        obtenerUbicacion();
    }

    private void obtenerUbicacion() {
        android.location.LocationManager lm = (android.location.LocationManager)
                getSystemService(LOCATION_SERVICE);

        try {
            if (lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                lm.requestSingleUpdate(
                        android.location.LocationManager.NETWORK_PROVIDER,
                        location -> {
                            if (!yaGuardo) {
                                yaGuardo = true;
                                validarGPS(location.getLatitude(), location.getLongitude());
                            }
                        },
                        null);
            } else if (lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                lm.requestSingleUpdate(
                        android.location.LocationManager.GPS_PROVIDER,
                        location -> {
                            if (!yaGuardo) {
                                yaGuardo = true;
                                validarGPS(location.getLatitude(), location.getLongitude());
                            }
                        },
                        null);
            } else {
                // Sin GPS ni red — usar coordenadas por defecto
                validarGPS(-7.1638, -78.5040);
            }
            Toast.makeText(this, "Obteniendo ubicación...", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(this, "Error GPS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void validarGPS(double latActual, double lonActual) {
        int areaId      = listaAreas.get(spinnerArea.getSelectedItemPosition()).getId();
        int actividadId = listaActividades.get(spinnerActividad.getSelectedItemPosition()).getId();
        boolean geoValidado = true;

        Toast.makeText(this, "📍 Ubicación obtenida", Toast.LENGTH_SHORT).show();
        guardarRegistro(areaId, actividadId, latActual, lonActual, geoValidado);
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

    private void guardarRegistro(int areaId, int actividadId,
                                 double lat, double lon, boolean geoValidado) {
        progressBar.setVisibility(View.VISIBLE);
        btnSiguiente.setEnabled(false);

        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
        String tkn = prefs.getString("token", "");

        JSONObject body = new JSONObject();
        try {
            body.put("area_id", areaId);
            body.put("actividad_id", actividadId);
            body.put("lat", lat);
            body.put("lon", lon);
            body.put("geo_validado", geoValidado);
        } catch (Exception e) { e.printStackTrace(); }

        String finalBody = body.toString();
        String authHeader = "Bearer " + tkn;

        new Thread(() -> {
            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/iperc/guardar");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", authHeader);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(finalBody.getBytes(StandardCharsets.UTF_8));
                os.close();

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject resp = new JSONObject(sb.toString());
                int registroId = resp.getInt("registro_id");

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnSiguiente.setEnabled(true);
                    Intent intent = new Intent(this, PeligrosActivity.class);
                    intent.putExtra("area_id", areaId);
                    intent.putExtra("actividad_id", actividadId);
                    intent.putExtra("lat", lat);
                    intent.putExtra("lon", lon);
                    intent.putExtra("geo_validado", geoValidado);
                    intent.putExtra("registro_id", registroId);
                    startActivity(intent);
                    finish();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnSiguiente.setEnabled(true);
                    Toast.makeText(this, "Error guardando IPERC: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 && grantResults.length > 0 &&
                grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacion();
        } else {
            Toast.makeText(this, "Permiso GPS denegado", Toast.LENGTH_SHORT).show();
        }
    }
}