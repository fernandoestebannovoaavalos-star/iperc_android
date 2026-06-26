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

        TextView tvBienvenido  = findViewById(R.id.tvBienvenido);
        TextView tvRol         = findViewById(R.id.tvRol);
        Button btnNuevoIperc   = findViewById(R.id.btnNuevoIperc);
        Button btnListaIperc   = findViewById(R.id.btnListaIperc);
        Button btnSupervisor   = findViewById(R.id.btnSupervisor);
        Button btnCrearUsuario = findViewById(R.id.btnCrearUsuario);
        Button btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        Button btnMiPerfil     = findViewById(R.id.btnMiPerfil);
        Button btnReportes     = findViewById(R.id.btnReportes);
        Button btnEstadisticas = findViewById(R.id.btnEstadisticas);
        Button btnObras        = findViewById(R.id.btnObras);

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
            btnSupervisor.setVisibility(View.VISIBLE);
            btnSupervisor.setOnClickListener(v ->
                    startActivity(new Intent(this, SupervisorActivity.class)));
            btnEstadisticas.setVisibility(View.VISIBLE);
            btnEstadisticas.setOnClickListener(v ->
                    startActivity(new Intent(this, EstadisticasActivity.class)));
        } else {
            btnSupervisor.setVisibility(View.GONE);
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
}