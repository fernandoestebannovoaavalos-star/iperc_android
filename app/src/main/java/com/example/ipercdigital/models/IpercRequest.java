package com.example.ipercdigital.models;

public class IpercRequest {
    private int area_id;
    private int actividad_id;
    private double lat;
    private double lon;
    private boolean geo_validado;

    public IpercRequest(int area_id, int actividad_id,
                        double lat, double lon, boolean geo_validado) {
        this.area_id      = area_id;
        this.actividad_id = actividad_id;
        this.lat          = lat;
        this.lon          = lon;
        this.geo_validado = geo_validado;
    }
}