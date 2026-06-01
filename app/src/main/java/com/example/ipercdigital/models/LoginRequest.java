package com.example.ipercdigital.models;

public class LoginRequest {
    private String dni;
    private String password;

    public LoginRequest(String dni, String password) {
        this.dni = dni;
        this.password = password;
    }

    public String getDni() { return dni; }
    public String getPassword() { return password; }
}