package com.example.ipercdigital.models;

public class Peligro {
    private int id;
    private String descripcion;
    private String riesgo;
    private int p_sin;
    private int s_sin;
    private String nivel_sin;
    private String medidas;
    private int p_con;
    private int s_con;
    private String nivel_con;

    public int getId()          { return id; }
    public String getDescripcion() { return descripcion; }
    public String getRiesgo()   { return riesgo; }
    public int getP_sin()       { return p_sin; }
    public int getS_sin()       { return s_sin; }
    public String getNivel_sin() { return nivel_sin; }
    public String getMedidas()  { return medidas; }
    public int getP_con()       { return p_con; }
    public int getS_con()       { return s_con; }
    public String getNivel_con() { return nivel_con; }
}