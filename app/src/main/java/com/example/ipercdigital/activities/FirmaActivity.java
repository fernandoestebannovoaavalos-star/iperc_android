package com.example.ipercdigital.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ipercdigital.R;
import com.example.ipercdigital.api.ApiClient;
import com.example.ipercdigital.models.IpercRequest;
import com.example.ipercdigital.models.IpercResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FirmaActivity extends AppCompatActivity {

    private FirmaView firmaView;
    private Button btnGuardar, btnLimpiar;
    private String token;
    private int areaId, actividadId;
    private double lat, lon;
    private boolean geoValidado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firma);

        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
        token = "Bearer " + prefs.getString("token", "");

        areaId      = getIntent().getIntExtra("area_id", 0);
        actividadId = getIntent().getIntExtra("actividad_id", 0);
        lat         = getIntent().getDoubleExtra("lat", 0);
        lon         = getIntent().getDoubleExtra("lon", 0);
        geoValidado = getIntent().getBooleanExtra("geo_validado", false);

        firmaView  = findViewById(R.id.firmaView);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnLimpiar = findViewById(R.id.btnLimpiar);

        btnLimpiar.setOnClickListener(v -> firmaView.limpiar());
        btnGuardar.setOnClickListener(v -> guardarRegistro());
    }

    private void guardarRegistro() {
        IpercRequest request = new IpercRequest(
                areaId, actividadId, lat, lon, geoValidado);

        ApiClient.getService().guardarIperc(token, request)
                .enqueue(new Callback<IpercResponse>() {
                    @Override
                    public void onResponse(Call<IpercResponse> call,
                                           Response<IpercResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(FirmaActivity.this,
                                    "✓ IPERC guardado: " + response.body().getCodigo(),
                                    Toast.LENGTH_LONG).show();
                            startActivity(new Intent(FirmaActivity.this,
                                    DashboardActivity.class));
                            finish();
                        } else {
                            Toast.makeText(FirmaActivity.this,
                                    "Error al guardar", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<IpercResponse> call, Throwable t) {
                        Toast.makeText(FirmaActivity.this,
                                "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}