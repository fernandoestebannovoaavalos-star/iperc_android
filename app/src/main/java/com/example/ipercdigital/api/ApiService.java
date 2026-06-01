package com.example.ipercdigital.api;

import com.example.ipercdigital.models.Actividad;
import com.example.ipercdigital.models.Area;
import com.example.ipercdigital.models.LoginRequest;
import com.example.ipercdigital.models.LoginResponse;
import java.util.List;

import com.example.ipercdigital.models.IpercRequest;
import com.example.ipercdigital.models.IpercResponse;
import com.example.ipercdigital.models.Peligro;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    @POST("api/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @GET("api/actividades/{area_id}")
    Call<List<Actividad>> getActividades(
            @Header("Authorization") String token,
            @Path("area_id") int areaId);

    @GET("api/areas")
    Call<List<Area>> getAreas(
            @Header("Authorization") String token);




    @GET("api/peligros/{actividad_id}")
    Call<List<Peligro>> getPeligros(
            @Header("Authorization") String token,
            @Path("actividad_id") int actividadId);

    @POST("api/iperc/guardar")
    Call<IpercResponse> guardarIperc(
            @Header("Authorization") String token,
            @Body IpercRequest request);
}

