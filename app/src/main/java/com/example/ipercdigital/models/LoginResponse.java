package com.example.ipercdigital.models;

public class LoginResponse {
    private String token;
    private String rol;
    private String nombre;
    private boolean debe_cambiar_clave;

    public String getToken() { return token; }
    public String getRol() { return rol; }
    public String getNombre() { return nombre; }
    public boolean isDebe_cambiar_clave() { return debe_cambiar_clave; }
}