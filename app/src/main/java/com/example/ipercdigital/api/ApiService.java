package com.example.ipercdigital.api;

import com.example.ipercdigital.models.Actividad;
import com.example.ipercdigital.models.Area;
import com.example.ipercdigital.models.LoginRequest;
import com.example.ipercdigital.models.LoginResponse;
import com.example.ipercdigital.models.IpercRequest;
import com.example.ipercdigital.models.IpercResponse;
import com.example.ipercdigital.models.Peligro;

import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Path;

public interface ApiService {

    // ── AUTH ──────────────────────────────────────────
    @POST("api/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/cambiar_clave")
    Call<Map<String, Object>> cambiarClave(
            @Header("Authorization") String token,
            @Body Map<String, String> body);

    // ── CATÁLOGOS ─────────────────────────────────────
    @GET("api/areas")
    Call<List<Area>> getAreas(
            @Header("Authorization") String token);

    @GET("api/actividades/{area_id}")
    Call<List<Actividad>> getActividades(
            @Header("Authorization") String token,
            @Path("area_id") int areaId);

    @GET("api/peligros/{actividad_id}")
    Call<List<Peligro>> getPeligros(
            @Header("Authorization") String token,
            @Path("actividad_id") int actividadId);

    // ── IPERC ─────────────────────────────────────────
    @POST("api/iperc/guardar")
    Call<IpercResponse> guardarIperc(
            @Header("Authorization") String token,
            @Body IpercRequest request);

    @POST("api/iperc/firma")
    Call<Map<String, Object>> guardarFirma(
            @Header("Authorization") String token,
            @Body Map<String, Object> body);

    @GET("api/iperc/lista")
    Call<List<Map<String, Object>>> listaRegistros(
            @Header("Authorization") String token);

    @GET("api/iperc/detalle/{id}")
    Call<Map<String, Object>> detalleIperc(
            @Header("Authorization") String token,
            @Path("id") int id);

    @GET("api/iperc/pdf/{id}")
    Call<ResponseBody> descargarPdf(
            @Header("Authorization") String token,
            @Path("id") int id);

    // ── SUPERVISOR ────────────────────────────────────
    @GET("api/supervisor/pendientes")
    Call<List<Map<String, Object>>> supervisorPendientes(
            @Header("Authorization") String token);

    @GET("api/supervisor/todos")
    Call<List<Map<String, Object>>> supervisorTodos(
            @Header("Authorization") String token);

    @POST("api/supervisor/aprobar/{id}")
    Call<Map<String, Object>> aprobarIperc(
            @Header("Authorization") String token,
            @Path("id") int id);

    @POST("api/supervisor/observar/{id}")
    Call<Map<String, Object>> observarIperc(
            @Header("Authorization") String token,
            @Path("id") int id,
            @Body Map<String, String> body);

    @POST("api/supervisor/aprobar_con_firma/{id}")
    Call<Map<String, Object>> aprobarConFirma(
            @Header("Authorization") String token,
            @Path("id") int id,
            @Body Map<String, Object> body);

    // ── ADMIN ─────────────────────────────────────────
    @GET("api/admin/usuarios")
    Call<List<Map<String, Object>>> listarUsuarios(
            @Header("Authorization") String token);

    @POST("api/admin/crear_usuario")
    Call<Map<String, Object>> crearUsuario(
            @Header("Authorization") String token,
            @Body Map<String, String> body);

    @POST("api/admin/toggle_usuario/{id}")
    Call<Map<String, Object>> toggleUsuario(
            @Header("Authorization") String token,
            @Path("id") int id);

    @POST("api/admin/cambiar_rol/{id}")
    Call<Map<String, Object>> cambiarRol(
            @Header("Authorization") String token,
            @Path("id") int id,
            @Body Map<String, String> body);

    @POST("api/admin/resetear_clave/{id}")
    Call<Map<String, Object>> resetearClave(
            @Header("Authorization") String token,
            @Path("id") int id);



    // ── REPORTES ──────────────────────────────────────────
    @GET("api/reportes")
    Call<List<Map<String, Object>>> getReportes(
            @Header("Authorization") String token,
            @Query("estado") String estado,
            @Query("fecha_inicio") String fechaInicio,
            @Query("fecha_fin") String fechaFin,
            @Query("obra_id") String obraId);

    // ── ESTADÍSTICAS ──────────────────────────────────────
    @GET("api/estadisticas")
    Call<Map<String, Object>> getEstadisticas(
            @Header("Authorization") String token);

    // ── OBRAS ─────────────────────────────────────────────
    @GET("api/admin/obras")
    Call<List<Map<String, Object>>> listarObras(
            @Header("Authorization") String token);

    @POST("api/admin/obras/nueva")
    Call<Map<String, Object>> nuevaObra(
            @Header("Authorization") String token,
            @Body Map<String, Object> body);

    @POST("api/admin/obras/toggle/{id}")
    Call<Map<String, Object>> toggleObra(
            @Header("Authorization") String token,
            @Path("id") int id);

    @POST("api/admin/obras/editar/{id}")
    Call<Map<String, Object>> editarObra(
            @Header("Authorization") String token,
            @Path("id") int id,
            @Body Map<String, Object> body);


}

