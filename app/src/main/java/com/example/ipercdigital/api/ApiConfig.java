package com.example.ipercdigital.api;

import java.net.HttpURLConnection;
import java.net.URL;

public class ApiConfig {

    // URLs disponibles
    private static final String URL_CASA        = "http://192.168.1.43:5000";
    private static final String URL_UNIVERSIDAD = "http://10.253.139.139:5000";
    private static final String URL_NGROK       = "https://partake-legibly-overlap.ngrok-free.dev";
    private static final String URL_EMULADOR    = "http://10.0.2.2:5000";

    // ← Cambia solo esta línea según dónde estés
    public static final String BASE_URL = URL_CASA + "/";

    public static HttpURLConnection getConnection(String endpoint) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("ngrok-skip-browser-warning", "true");
        conn.setRequestProperty("Content-Type", "application/json");
        return conn;
    }
}