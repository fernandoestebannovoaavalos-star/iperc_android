package com.example.ipercdigital.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ipercdigital.R;
import com.example.ipercdigital.api.ApiClient;
import com.example.ipercdigital.api.ApiConfig;
import com.example.ipercdigital.models.Peligro;

import org.json.JSONArray;
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

public class PeligrosActivity extends AppCompatActivity {

    private LinearLayout contenedorPeligros;
    private LinearLayout contenedorAdicionales;
    private Button btnFirmar, btnAgregarAdicional;
    private String token;
    private int areaId, actividadId, registroId;
    private double lat, lon;
    private boolean geoValidado;
    private List<View> vistasAdicionales = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peligros);

        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
        token = "Bearer " + prefs.getString("token", "");

        areaId      = getIntent().getIntExtra("area_id", 0);
        actividadId = getIntent().getIntExtra("actividad_id", 0);
        lat         = getIntent().getDoubleExtra("lat", 0);
        lon         = getIntent().getDoubleExtra("lon", 0);
        geoValidado = getIntent().getBooleanExtra("geo_validado", false);
        registroId  = getIntent().getIntExtra("registro_id", -1);

        contenedorPeligros    = findViewById(R.id.contenedorPeligros);
        contenedorAdicionales = findViewById(R.id.contenedorAdicionales);
        btnFirmar             = findViewById(R.id.btnFirmar);
        btnAgregarAdicional   = findViewById(R.id.btnAgregarAdicional);

        cargarPeligros();

        btnAgregarAdicional.setOnClickListener(v -> agregarFormularioAdicional());

        btnFirmar.setOnClickListener(v -> {
            List<JSONObject> adicionales = recopilarAdicionales();
            if (adicionales == null) return; // validación falló

            if (adicionales.isEmpty()) {
                // Sin adicionales, ir directo a firma
                irAFirma();
            } else {
                // Guardar adicionales primero
                guardarAdicionalesYFirmar(adicionales);
            }
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

    private void agregarFormularioAdicional() {
        View form = LayoutInflater.from(this)
                .inflate(R.layout.item_peligro_adicional, contenedorAdicionales, false);

        Button btnEliminar = form.findViewById(R.id.btnEliminarAdicional);
        btnEliminar.setOnClickListener(v -> {
            contenedorAdicionales.removeView(form);
            vistasAdicionales.remove(form);
        });

        contenedorAdicionales.addView(form);
        vistasAdicionales.add(form);
    }

    private List<JSONObject> recopilarAdicionales() {
        List<JSONObject> lista = new ArrayList<>();
        for (View form : vistasAdicionales) {
            try {
                String tipo       = ((EditText) form.findViewById(R.id.etTipoAdicional)).getText().toString().trim();
                String descripcion= ((EditText) form.findViewById(R.id.etDescripcionAdicional)).getText().toString().trim();
                String riesgo     = ((EditText) form.findViewById(R.id.etRiesgoAdicional)).getText().toString().trim();
                String pStr       = ((EditText) form.findViewById(R.id.etPAdicional)).getText().toString().trim();
                String sStr       = ((EditText) form.findViewById(R.id.etSAdicional)).getText().toString().trim();
                String medidas    = ((EditText) form.findViewById(R.id.etMedidasAdicional)).getText().toString().trim();

                if (tipo.isEmpty() || descripcion.isEmpty()) {
                    Toast.makeText(this, "Complete tipo y descripción en todos los peligros adicionales", Toast.LENGTH_SHORT).show();
                    return null;
                }

                int p = pStr.isEmpty() ? 1 : Integer.parseInt(pStr);
                int s = sStr.isEmpty() ? 1 : Integer.parseInt(sStr);

                JSONObject obj = new JSONObject();
                obj.put("tipo", tipo);
                obj.put("descripcion", descripcion);
                obj.put("riesgo", riesgo);
                obj.put("p", p);
                obj.put("s", s);
                obj.put("medidas", medidas);
                lista.add(obj);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return lista;
    }

    private void guardarAdicionalesYFirmar(List<JSONObject> adicionales) {
        btnFirmar.setEnabled(false);
        Toast.makeText(this, "Guardando peligros adicionales...", Toast.LENGTH_SHORT).show();

        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
        String tkn = prefs.getString("token", "");

        new Thread(() -> {
            try {
                JSONArray array = new JSONArray();
                for (JSONObject obj : adicionales) array.put(obj);

                JSONObject body = new JSONObject();
                body.put("registro_id", registroId);
                body.put("adicionales", array);

                HttpURLConnection conn = ApiConfig.getConnection("/api/iperc/adicionales");
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + tkn);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                os.close();

                int code = conn.getResponseCode();
                runOnUiThread(() -> {
                    btnFirmar.setEnabled(true);
                    if (code == 201 || code == 200) {
                        irAFirma();
                    } else {
                        Toast.makeText(this, "Error al guardar adicionales: " + code, Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnFirmar.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void irAFirma() {
        Intent intent = new Intent(this, FirmaActivity.class);
        intent.putExtra("registro_id", registroId);
        intent.putExtra("lat", lat);
        intent.putExtra("lon", lon);
        startActivity(intent);
        finish();
    }
}