package com.example.ipercdigital.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ipercdigital.R;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
        String nombre = prefs.getString("nombre", "Usuario");
        String rol    = prefs.getString("rol", "");

        TextView tvBienvenido = findViewById(R.id.tvBienvenido);
        TextView tvRol        = findViewById(R.id.tvRol);
        Button btnNuevoIperc  = findViewById(R.id.btnNuevoIperc);
        Button btnListaIperc  = findViewById(R.id.btnListaIperc);
        Button btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        tvBienvenido.setText("Bienvenido, " + nombre);
        tvRol.setText("Rol: " + rol);

        btnNuevoIperc.setOnClickListener(v ->
                startActivity(new Intent(this, IpercActivity.class)));

        btnListaIperc.setOnClickListener(v ->
                startActivity(new Intent(this, ListaIpercActivity.class)));

        btnCerrarSesion.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}