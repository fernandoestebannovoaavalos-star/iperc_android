package com.example.ipercdigital.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
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

        TextView tvBienvenido    = findViewById(R.id.tvBienvenido);
        TextView tvRol           = findViewById(R.id.tvRol);
        Button btnNuevoIperc     = findViewById(R.id.btnNuevoIperc);
        Button btnListaIperc     = findViewById(R.id.btnListaIperc);
        Button btnSupervisor     = findViewById(R.id.btnSupervisor);
        Button btnCrearUsuario   = findViewById(R.id.btnCrearUsuario);
        Button btnCerrarSesion   = findViewById(R.id.btnCerrarSesion);

        tvBienvenido.setText("Bienvenido, " + nombre);
        tvRol.setText("Rol: " + rol);

        btnNuevoIperc.setOnClickListener(v ->
                startActivity(new Intent(this, IpercActivity.class)));

        btnListaIperc.setOnClickListener(v ->
                startActivity(new Intent(this, ListaRegistrosActivity.class)));

        // Panel supervisor para supervisor y admin
        if (rol.equals("supervisor") || rol.equals("admin")) {
            btnSupervisor.setVisibility(View.VISIBLE);
            btnSupervisor.setOnClickListener(v ->
                    startActivity(new Intent(this, SupervisorActivity.class)));
        } else {
            btnSupervisor.setVisibility(View.GONE);
        }

        // Gestión de usuarios solo para admin
        if (rol.equals("admin")) {
            btnCrearUsuario.setVisibility(View.VISIBLE);
            btnCrearUsuario.setOnClickListener(v ->
                    startActivity(new Intent(this, ListaUsuariosActivity.class)));
        } else {
            btnCrearUsuario.setVisibility(View.GONE);
        }

        btnCerrarSesion.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}