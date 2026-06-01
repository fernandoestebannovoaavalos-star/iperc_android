package com.example.ipercdigital.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ipercdigital.R;
import com.example.ipercdigital.api.ApiClient;
import com.example.ipercdigital.models.Peligro;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PeligrosActivity extends AppCompatActivity {

    private LinearLayout contenedorPeligros;
    private Button btnFirmar;
    private String token;
    private int areaId, actividadId;
    private double lat, lon;
    private boolean geoValidado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peligros);

        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
        token = "Bearer " + prefs.getString("token", "");

        // Recibir datos de IpercActivity
        areaId      = getIntent().getIntExtra("area_id", 0);
        actividadId = getIntent().getIntExtra("actividad_id", 0);
        lat         = getIntent().getDoubleExtra("lat", 0);
        lon         = getIntent().getDoubleExtra("lon", 0);
        geoValidado = getIntent().getBooleanExtra("geo_validado", false);

        contenedorPeligros = findViewById(R.id.contenedorPeligros);
        btnFirmar          = findViewById(R.id.btnFirmar);

        cargarPeligros();

        btnFirmar.setOnClickListener(v -> {
            Intent intent = new Intent(this, FirmaActivity.class);
            intent.putExtra("area_id", areaId);
            intent.putExtra("actividad_id", actividadId);
            intent.putExtra("lat", lat);
            intent.putExtra("lon", lon);
            intent.putExtra("geo_validado", geoValidado);
            startActivity(intent);
        });
    }

    private void cargarPeligros() {
        ApiClient.getService().getPeligros(token, actividadId)
                .enqueue(new Callback<List<Peligro>>() {
                    @Override
                    public void onResponse(Call<List<Peligro>> call,
                                           Response<List<Peligro>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            for (Peligro p : response.body()) {
                                agregarTarjetaPeligro(p);
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<List<Peligro>> call, Throwable t) {
                        Toast.makeText(PeligrosActivity.this,
                                "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void agregarTarjetaPeligro(Peligro p) {
        View tarjeta = LayoutInflater.from(this)
                .inflate(R.layout.item_peligro, contenedorPeligros, false);

        ((TextView) tarjeta.findViewById(R.id.tvDescripcion))
                .setText("⚠ " + p.getDescripcion());
        ((TextView) tarjeta.findViewById(R.id.tvRiesgo))
                .setText("Riesgo: " + p.getRiesgo());
        ((TextView) tarjeta.findViewById(R.id.tvNivelSin))
                .setText("Sin control: P=" + p.getP_sin() + " S=" + p.getS_sin() +
                        " → " + p.getNivel_sin());
        ((TextView) tarjeta.findViewById(R.id.tvMedidas))
                .setText("Control: " + p.getMedidas());
        ((TextView) tarjeta.findViewById(R.id.tvNivelCon))
                .setText("Con control: P=" + p.getP_con() + " S=" + p.getS_con() +
                        " → " + p.getNivel_con());

        contenedorPeligros.addView(tarjeta);
    }
}