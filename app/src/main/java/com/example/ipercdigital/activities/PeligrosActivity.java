package com.example.ipercdigital.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ipercdigital.R;
import com.example.ipercdigital.api.ApiClient;
import com.example.ipercdigital.api.ApiConfig;
import com.example.ipercdigital.models.Peligro;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PeligrosActivity extends AppCompatActivity {

    private LinearLayout contenedorPeligros, contenedorAdicionales;
    private Button btnFirmar, btnAgregarAdicional;
    private String token;
    private int areaId, actividadId, registroId;
    private double lat, lon;
    private boolean geoValidado;
    private List<View> vistasAdicionales = new ArrayList<>();
    private List<View> tarjetasPeligro = new ArrayList<>();
    private List<Peligro> listaPeligros = new ArrayList<>();

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
            List<JSONObject> peligrosSeleccionados = recopilarPeligrosSeleccionados();
            List<JSONObject> adicionales = recopilarAdicionales();
            if (adicionales == null) return;

            guardarPeligrosYFirmar(peligrosSeleccionados, adicionales);
        });
    }

    private void cargarPeligros() {
        ApiClient.getService().getPeligros(token, actividadId)
                .enqueue(new Callback<List<Peligro>>() {
                    @Override
                    public void onResponse(Call<List<Peligro>> call,
                                           Response<List<Peligro>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            listaPeligros = response.body();
                            for (Peligro p : listaPeligros) {
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

        tarjeta.setTag(p.getId());

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
        tarjetasPeligro.add(tarjeta);
    }

    private List<JSONObject> recopilarPeligrosSeleccionados() {
        List<JSONObject> lista = new ArrayList<>();
        for (View tarjeta : tarjetasPeligro) {
            CheckBox check = tarjeta.findViewById(R.id.checkPeligro);
            if (check != null && check.isChecked()) {
                int peligroId = (int) tarjeta.getTag();
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("peligro_id", peligroId);
                    obj.put("p", 1);
                    obj.put("s", 1);
                    lista.add(obj);
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
        return lista;
    }

    private List<JSONObject> recopilarAdicionales() {
        List<JSONObject> lista = new ArrayList<>();
        for (View form : vistasAdicionales) {
            try {
                String tipo        = ((EditText) form.findViewById(R.id.etTipoAdicional)).getText().toString().trim();
                String descripcion = ((EditText) form.findViewById(R.id.etDescripcionAdicional)).getText().toString().trim();
                String riesgo      = ((EditText) form.findViewById(R.id.etRiesgoAdicional)).getText().toString().trim();
                String pStr        = ((EditText) form.findViewById(R.id.etPAdicional)).getText().toString().trim();
                String sStr        = ((EditText) form.findViewById(R.id.etSAdicional)).getText().toString().trim();
                String medidas     = ((EditText) form.findViewById(R.id.etMedidasAdicional)).getText().toString().trim();

                if (tipo.isEmpty() || descripcion.isEmpty()) {
                    Toast.makeText(this, "Complete tipo y descripción en todos los peligros adicionales",
                            Toast.LENGTH_SHORT).show();
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

            } catch (Exception e) { e.printStackTrace(); }
        }
        return lista;
    }

    private void guardarPeligrosYFirmar(List<JSONObject> peligros,
                                        List<JSONObject> adicionales) {
        btnFirmar.setEnabled(false);
        Toast.makeText(this, "Guardando peligros...", Toast.LENGTH_SHORT).show();

        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
        String tkn = prefs.getString("token", "");

        new Thread(() -> {
            try {
                JSONArray arrPeligros    = new JSONArray();
                JSONArray arrAdicionales = new JSONArray();

                for (JSONObject obj : peligros)    arrPeligros.put(obj);
                for (JSONObject obj : adicionales) arrAdicionales.put(obj);

                JSONObject body = new JSONObject();
                body.put("registro_id", registroId);
                body.put("peligros",    arrPeligros);
                body.put("adicionales", arrAdicionales);

                HttpURLConnection conn = ApiConfig.getConnection("/api/iperc/adicionales");
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + tkn);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes());
                os.close();

                int code = conn.getResponseCode();
                runOnUiThread(() -> {
                    btnFirmar.setEnabled(true);
                    if (code == 200 || code == 201) {
                        irAFirma();
                    } else {
                        Toast.makeText(this, "Error al guardar: " + code,
                                Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnFirmar.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void agregarFormularioAdicional() {
        View form = LayoutInflater.from(this)
                .inflate(R.layout.item_peligro_adicional, contenedorAdicionales, false);

        EditText etP     = form.findViewById(R.id.etPAdicional);
        EditText etS     = form.findViewById(R.id.etSAdicional);
        TextView tvNivel = form.findViewById(R.id.tvNivelAdicional);

        // Watcher para calcular nivel automáticamente
        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable e) {
                String pStr = etP.getText().toString().trim();
                String sStr = etS.getText().toString().trim();
                if (!pStr.isEmpty() && !sStr.isEmpty()) {
                    try {
                        int p = Integer.parseInt(pStr);
                        int s = Integer.parseInt(sStr);
                        if (p >= 1 && p <= 5 && s >= 1 && s <= 5) {
                            String nivel = calcularNivel(p, s);
                            tvNivel.setText("Nivel de riesgo: " + nivel);
                            tvNivel.setVisibility(View.VISIBLE);
                            // Color según nivel
                            int color;
                            switch (nivel) {
                                case "MUY BAJO":  color = 0xFF4CAF50; break;
                                case "BAJO":      color = 0xFF8BC34A; break;
                                case "MEDIO":     color = 0xFFFFEB3B; break;
                                case "ALTO":      color = 0xFFFF9800; break;
                                case "MUY ALTO":  color = 0xFFF44336; break;
                                case "EXTREMO":   color = 0xFF880E4F; break;
                                default:          color = 0xFFEEEEEE; break;
                            }
                            tvNivel.setBackgroundColor(color);
                            tvNivel.setTextColor(nivel.equals("MEDIO") ? 0xFF333333 : 0xFFFFFFFF);
                        } else {
                            tvNivel.setText("P y S deben estar entre 1 y 5");
                            tvNivel.setVisibility(View.VISIBLE);
                            tvNivel.setBackgroundColor(0xFFEEEEEE);
                            tvNivel.setTextColor(0xFF555555);
                        }
                    } catch (NumberFormatException ex) {
                        tvNivel.setVisibility(View.GONE);
                    }
                } else {
                    tvNivel.setVisibility(View.GONE);
                }
            }
        };

        etP.addTextChangedListener(watcher);
        etS.addTextChangedListener(watcher);

        Button btnEliminar = form.findViewById(R.id.btnEliminarAdicional);
        btnEliminar.setOnClickListener(v -> {
            contenedorAdicionales.removeView(form);
            vistasAdicionales.remove(form);
        });

        contenedorAdicionales.addView(form);
        vistasAdicionales.add(form);
    }

    private String calcularNivel(int p, int s) {
        int[][] MATRIZ = {
                {0,       0,         0,       0,       0,       0      },
                {0, 0/*11*/, 0/*12*/, 0/*13*/, 0/*14*/, 0/*15*/},
                {0, 0/*21*/, 0/*22*/, 0/*23*/, 0/*24*/, 0/*25*/},
                {0, 0/*31*/, 0/*32*/, 0/*33*/, 0/*34*/, 0/*35*/},
                {0, 0/*41*/, 0/*42*/, 0/*43*/, 0/*44*/, 0/*45*/},
                {0, 0/*51*/, 0/*52*/, 0/*53*/, 0/*54*/, 0/*55*/},
        };
        String[][] NIVELES = {
                {"",         "",          "",          "",          "",          ""},
                {"", "MUY BAJO",  "MUY BAJO",  "BAJO",      "MEDIO",     "MEDIO"    },
                {"", "MUY BAJO",  "BAJO",      "MEDIO",     "MEDIO",     "ALTO"     },
                {"", "BAJO",      "MEDIO",     "MEDIO",     "ALTO",      "MUY ALTO" },
                {"", "MEDIO",     "MEDIO",     "ALTO",      "MUY ALTO",  "EXTREMO"  },
                {"", "MEDIO",     "ALTO",      "MUY ALTO",  "EXTREMO",   "EXTREMO"  },
        };
        if (p >= 1 && p <= 5 && s >= 1 && s <= 5) {
            return NIVELES[p][s];
        }
        return "MEDIO";
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