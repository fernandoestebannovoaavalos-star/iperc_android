package com.example.ipercdigital.activities;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipercdigital.R;
import com.example.ipercdigital.api.ApiConfig;

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

public class ObrasActivity extends AppCompatActivity {

    RecyclerView recycler;
    ProgressBar progress;
    TextView tvVacio;
    Button btnNuevaObra;
    List<JSONObject> obras = new ArrayList<>();
    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obras);

        recycler     = findViewById(R.id.recyclerObras);
        progress     = findViewById(R.id.progressObras);
        tvVacio      = findViewById(R.id.tvVacioObras);
        btnNuevaObra = findViewById(R.id.btnNuevaObra);

        recycler.setLayoutManager(new LinearLayoutManager(this));

        SharedPreferences prefs = getSharedPreferences("iperc_prefs", MODE_PRIVATE);
        token = prefs.getString("token", "");

        btnNuevaObra.setOnClickListener(v -> mostrarDialogoNuevaObra());
        cargarObras();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarObras();
    }

    private void cargarObras() {
        progress.setVisibility(View.VISIBLE);
        tvVacio.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/admin/obras");
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
                obras.clear();
                for (int i = 0; i < array.length(); i++) {
                    obras.add(array.getJSONObject(i));
                }

                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    if (obras.isEmpty()) {
                        tvVacio.setVisibility(View.VISIBLE);
                        recycler.setAdapter(null);
                    } else {
                        tvVacio.setVisibility(View.GONE);
                        recycler.setAdapter(new ObrasAdapter());
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al cargar obras", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void mostrarDialogoNuevaObra() {
        View form = LayoutInflater.from(this)
                .inflate(R.layout.dialog_obra, null);

        EditText etNombre    = form.findViewById(R.id.etObraNombre);
        EditText etEmpresa   = form.findViewById(R.id.etObraEmpresa);
        EditText etDireccion = form.findViewById(R.id.etObraDireccion);
        EditText etLat       = form.findViewById(R.id.etObraLat);
        EditText etLon       = form.findViewById(R.id.etObraLon);
        EditText etRadio     = form.findViewById(R.id.etObraRadio);

        new AlertDialog.Builder(this)
                .setTitle("Nueva Obra")
                .setView(form)
                .setPositiveButton("Crear", (dialog, which) -> {
                    String nombre = etNombre.getText().toString().trim();
                    if (nombre.isEmpty()) {
                        Toast.makeText(this, "El nombre es obligatorio",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    crearObra(
                            nombre,
                            etEmpresa.getText().toString().trim(),
                            etDireccion.getText().toString().trim(),
                            etLat.getText().toString().trim(),
                            etLon.getText().toString().trim(),
                            etRadio.getText().toString().trim()
                    );
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoEditarObra(JSONObject obra) {
        try {
            View form = LayoutInflater.from(this)
                    .inflate(R.layout.dialog_obra, null);

            EditText etNombre    = form.findViewById(R.id.etObraNombre);
            EditText etEmpresa   = form.findViewById(R.id.etObraEmpresa);
            EditText etDireccion = form.findViewById(R.id.etObraDireccion);
            EditText etLat       = form.findViewById(R.id.etObraLat);
            EditText etLon       = form.findViewById(R.id.etObraLon);
            EditText etRadio     = form.findViewById(R.id.etObraRadio);

            etNombre.setText(obra.optString("nombre"));
            etEmpresa.setText(obra.optString("empresa"));
            etDireccion.setText(obra.optString("direccion"));
            if (!obra.isNull("lat"))
                etLat.setText(String.valueOf(obra.optDouble("lat")));
            if (!obra.isNull("lon"))
                etLon.setText(String.valueOf(obra.optDouble("lon")));
            etRadio.setText(String.valueOf(obra.optInt("radio", 100)));

            int id = obra.optInt("id");

            new AlertDialog.Builder(this)
                    .setTitle("Editar Obra")
                    .setView(form)
                    .setPositiveButton("Guardar", (dialog, which) -> {
                        editarObra(id,
                                etNombre.getText().toString().trim(),
                                etEmpresa.getText().toString().trim(),
                                etDireccion.getText().toString().trim(),
                                etLat.getText().toString().trim(),
                                etLon.getText().toString().trim(),
                                etRadio.getText().toString().trim()
                        );
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void crearObra(String nombre, String empresa, String direccion,
                           String lat, String lon, String radio) {
        new Thread(() -> {
            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/admin/obras/nueva");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("nombre", nombre);
                body.put("empresa", empresa);
                body.put("direccion", direccion);
                if (!lat.isEmpty()) body.put("lat", Double.parseDouble(lat));
                if (!lon.isEmpty()) body.put("lon", Double.parseDouble(lon));
                body.put("radio", radio.isEmpty() ? 100 : Integer.parseInt(radio));

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                os.close();

                int code = conn.getResponseCode();
                runOnUiThread(() -> {
                    if (code == 201) {
                        Toast.makeText(this, "✅ Obra creada", Toast.LENGTH_SHORT).show();
                        cargarObras();
                    } else {
                        Toast.makeText(this, "Error al crear obra", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void editarObra(int id, String nombre, String empresa, String direccion,
                            String lat, String lon, String radio) {
        new Thread(() -> {
            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/admin/obras/editar/" + id);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("nombre", nombre);
                body.put("empresa", empresa);
                body.put("direccion", direccion);
                if (!lat.isEmpty()) body.put("lat", Double.parseDouble(lat));
                if (!lon.isEmpty()) body.put("lon", Double.parseDouble(lon));
                if (!radio.isEmpty()) body.put("radio", Integer.parseInt(radio));

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                os.close();

                int code = conn.getResponseCode();
                runOnUiThread(() -> {
                    if (code == 200) {
                        Toast.makeText(this, "✅ Obra actualizada", Toast.LENGTH_SHORT).show();
                        cargarObras();
                    } else {
                        Toast.makeText(this, "Error al editar obra", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void toggleObra(int id, boolean activo) {
        new Thread(() -> {
            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/admin/obras/toggle/" + id);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);
                conn.getOutputStream().close();

                int code = conn.getResponseCode();
                runOnUiThread(() -> {
                    if (code == 200) {
                        String msg = activo ? "Obra desactivada" : "Obra activada";
                        Toast.makeText(this, "✅ " + msg, Toast.LENGTH_SHORT).show();
                        cargarObras();
                    } else {
                        Toast.makeText(this, "Error al cambiar estado", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    class ObrasAdapter extends RecyclerView.Adapter<ObrasAdapter.VH> {

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_obra, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            try {
                JSONObject o = obras.get(position);
                int id       = o.optInt("id");
                boolean activo = o.optBoolean("activo", true);

                holder.tvNombre.setText("🏗 " + o.optString("nombre", "—"));
                holder.tvEmpresa.setText("🏢 " + o.optString("empresa", "—"));
                holder.tvDireccion.setText("📍 " + o.optString("direccion", "—"));
                holder.tvRadio.setText("📡 Radio: " + o.optInt("radio", 100) + "m");
                holder.tvEstado.setText(activo ? "ACTIVA" : "INACTIVA");
                holder.tvEstado.setBackgroundColor(activo ? 0xFF4CAF50 : 0xFF9E9E9E);

                holder.btnEditar.setOnClickListener(v -> mostrarDialogoEditarObra(o));
                holder.btnToggle.setText(activo ? "Desactivar" : "Activar");
                holder.btnToggle.setBackgroundColor(activo ? 0xFFF44336 : 0xFF4CAF50);
                holder.btnToggle.setOnClickListener(v -> toggleObra(id, activo));

            } catch (Exception e) { e.printStackTrace(); }
        }

        @Override
        public int getItemCount() { return obras.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvNombre, tvEmpresa, tvDireccion, tvRadio, tvEstado;
            Button btnEditar, btnToggle;
            VH(View v) {
                super(v);
                tvNombre    = v.findViewById(R.id.tvObraNombre);
                tvEmpresa   = v.findViewById(R.id.tvObraEmpresa);
                tvDireccion = v.findViewById(R.id.tvObraDireccion);
                tvRadio     = v.findViewById(R.id.tvObraRadio);
                tvEstado    = v.findViewById(R.id.tvObraEstado);
                btnEditar   = v.findViewById(R.id.btnEditarObra);
                btnToggle   = v.findViewById(R.id.btnToggleObra);
            }
        }
    }
}