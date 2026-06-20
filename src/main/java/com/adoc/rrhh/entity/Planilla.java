package com.adoc.rrhh.entity;

import com.adoc.rrhh.entity.enums.TipoPlanilla;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "planillas")
public class Planilla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Periodo en formato "YYYY-MM", ej: "2026-06" */
    @Column(nullable = false, length = 7)
    private String periodo;

    /** 1 = primera quincena, 2 = segunda quincena */
    @Column(nullable = false)
    private Integer quincena;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_planilla", nullable = false, length = 15)
    private TipoPlanilla tipoPlanilla;

    @Column(name = "fecha_generacion")
    private LocalDate fechaGeneracion;

    @Column(length = 500)
    private String observaciones;

    @OneToMany(mappedBy = "planilla", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePlanilla> detalles = new ArrayList<>();

    // ─── Getters y Setters ──────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }

    public Integer getQuincena() {
        return quincena;
    }

    public void setQuincena(Integer quincena) {
        this.quincena = quincena;
    }

    public TipoPlanilla getTipoPlanilla() {
        return tipoPlanilla;
    }

    public void setTipoPlanilla(TipoPlanilla tipoPlanilla) {
        this.tipoPlanilla = tipoPlanilla;
    }

    public LocalDate getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(LocalDate fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public List<DetallePlanilla> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetallePlanilla> detalles) {
        this.detalles = detalles;
    }

    /**
     * Método de conveniencia para agregar un detalle y mantener la relación bidireccional.
     */
    public void agregarDetalle(DetallePlanilla detalle) {
        detalles.add(detalle);
        detalle.setPlanilla(this);
    }
}
