package com.adoc.rrhh.entity;

import com.adoc.rrhh.entity.enums.EstadoAusencia;
import com.adoc.rrhh.entity.enums.TipoAusencia;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "ausencias")
public class Ausencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ausencia", nullable = false, length = 40)
    private TipoAusencia tipoAusencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoAusencia estado = EstadoAusencia.PENDIENTE;

    @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "dias_totales")
    private Integer diasTotales;

    @Column(name = "dias_patrono")
    private Integer diasPatrono;

    @Column(name = "dias_isss")
    private Integer diasIsss;

    @Column(length = 500)
    private String motivo;

    @Column(name = "documento_respaldo", length = 255)
    private String documentoRespaldo;

    @Column(length = 500)
    private String observaciones;

    @Column(name = "fecha_registro")
    private LocalDate fechaRegistro = LocalDate.now();

    // ─── Método de cálculo de días ──────────────────────────────────────

    /**
     * Calcula los días totales entre fechaInicio y fechaFin (inclusive).
     */
    public void calcularDiasTotales() {
        if (fechaInicio != null && fechaFin != null) {
            this.diasTotales = (int) ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1;
        }
    }

    // ─── Getters y Setters ──────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public TipoAusencia getTipoAusencia() {
        return tipoAusencia;
    }

    public void setTipoAusencia(TipoAusencia tipoAusencia) {
        this.tipoAusencia = tipoAusencia;
    }

    public EstadoAusencia getEstado() {
        return estado;
    }

    public void setEstado(EstadoAusencia estado) {
        this.estado = estado;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Integer getDiasTotales() {
        return diasTotales;
    }

    public void setDiasTotales(Integer diasTotales) {
        this.diasTotales = diasTotales;
    }

    public Integer getDiasPatrono() {
        return diasPatrono;
    }

    public void setDiasPatrono(Integer diasPatrono) {
        this.diasPatrono = diasPatrono;
    }

    public Integer getDiasIsss() {
        return diasIsss;
    }

    public void setDiasIsss(Integer diasIsss) {
        this.diasIsss = diasIsss;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getDocumentoRespaldo() {
        return documentoRespaldo;
    }

    public void setDocumentoRespaldo(String documentoRespaldo) {
        this.documentoRespaldo = documentoRespaldo;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDate fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    /**
     * Devuelve una etiqueta legible para el tipo de ausencia.
     */
    public String getTipoAusenciaLabel() {
        if (tipoAusencia == null) return "";
        return switch (tipoAusencia) {
            case INCAPACIDAD_ENFERMEDAD -> "Incapacidad - Enfermedad";
            case INCAPACIDAD_ACCIDENTE -> "Incapacidad - Accidente Laboral";
            case INCAPACIDAD_MATERNIDAD -> "Incapacidad - Maternidad";
            case AUSENCIA_JUSTIFICADA_CON_GOCE -> "Ausencia Justificada (Con Goce)";
            case AUSENCIA_JUSTIFICADA_SIN_GOCE -> "Ausencia Justificada (Sin Goce)";
            case AUSENCIA_INJUSTIFICADA -> "Ausencia Injustificada";
        };
    }
}
