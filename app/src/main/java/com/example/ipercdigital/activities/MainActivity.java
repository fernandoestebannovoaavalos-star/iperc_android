package com.example.ipercdigital.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ipercdigital.R;
import com.example.ipercdigital.api.ApiClient;
import com.example.ipercdigital.models.LoginRequest;
import com.example.ipercdigital.models.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private EditText etDni, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etDni       = findViewById(R.id.etDni);
        etPassword  = findViewById(R.id.etPassword);
        btnLogin    = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> {
            String dni      = etDni.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (dni.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Ingresa DNI y contraseña", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);

            LoginRequest request = new LoginRequest(dni, password);
            ApiClient.getService().login(request).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);

                    if (response.isSuccessful() && response.body() != null) {
                        LoginResponse body = response.body();

                        // Guardar token y datos del usuario
                        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
                        prefs.edit()
                                .putString("token", body.getToken())
                                .putString("rol", body.getRol())
                                .putString("nombre", body.getNombre())
                                .putBoolean("debe_cambiar_clave", body.isDebe_cambiar_clave())
                                .apply();

                        Toast.makeText(MainActivity.this,
                                "Bienvenido " + body.getNombre(), Toast.LENGTH_SHORT).show();

                        // Verificar si debe cambiar clave
                        if (body.isDebe_cambiar_clave()) {
                            startActivity(new Intent(MainActivity.this, CambiarClaveActivity.class));
                        } else {
                            startActivity(new Intent(MainActivity.this, DashboardActivity.class));
                        }
                        finish();

                    } else {
                        Toast.makeText(MainActivity.this,
                                "DNI o contraseña incorrectos", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    Toast.makeText(MainActivity.this,
                            "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}