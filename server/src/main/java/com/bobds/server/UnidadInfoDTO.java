package com.bobds.server;

public class UnidadInfoDTO {
    private String nombre;
    private String idUnidad;
    private String descripcion;
    private String propietario;
    private String codVinculacion;
    private String estadoCodigo;

    public UnidadInfoDTO(String nombre, String idUnidad, String descripcion, String propietario, String codVinculacion, String estadoCodigo) {
        this.nombre = nombre;
        this.idUnidad = idUnidad;
        this.descripcion = descripcion;
        this.propietario = propietario;
        this.codVinculacion = codVinculacion;
        this.estadoCodigo = estadoCodigo;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getIdUnidad() { return idUnidad; }
    public void setIdUnidad(String idUnidad) { this.idUnidad = idUnidad; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getPropietario() { return propietario; }
    public void setPropietario(String propietario) { this.propietario = propietario; }

    public String getCodVinculacion() { return codVinculacion; }
    public void setCodVinculacion(String codVinculacion) { this.codVinculacion = codVinculacion; }

    public String getEstadoCodigo() { return estadoCodigo; }
    public void setEstadoCodigo(String estadoCodigo) { this.estadoCodigo = estadoCodigo; }
}
