package com.adoc.rrhh.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "detalle_planilla")
public class DetallePlanilla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planilla_id", nullable = false)
    private Planilla planilla;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    /** Snapshot del salario base al momento de generar la planilla */
    @Column(name = "salario_base", precision = 10, scale = 2)
    private BigDecimal salarioBase;

    /** Cantidad de horas extras trabajadas en el periodo */
    @Column(name = "horas_extras")
    private Double horasExtras = 0.0;

    /** Monto calculado por horas extras */
    @Column(name = "monto_horas_extras", precision = 10, scale = 2)
    private BigDecimal montoHorasExtras = BigDecimal.ZERO;

    /** Bonificaciones, comisiones u otros ingresos adicionales */
    @Column(name = "otros_ingresos", precision = 10, scale = 2)
    private BigDecimal otrosIngresos = BigDecimal.ZERO;

    /** salarioBase + montoHorasExtras + otrosIngresos - montoDescuentoAusencias */
    @Column(name = "total_devengado", precision = 10, scale = 2)
    private BigDecimal totalDevengado = BigDecimal.ZERO;

    // ─── Descuentos por Ausencias ──────────────────────────────────────

    @Column(name = "dias_ausencia_descontados")
    private Integer diasAusenciaDescontados = 0;

    @Column(name = "monto_descuento_ausencias", precision = 10, scale = 2)
    private BigDecimal montoDescuentoAusencias = BigDecimal.ZERO;

    // ─── Deducciones del empleado ───────────────────────────────────────

    /** ISSS empleado: 3% (tope $30.00 mensual) */
    @Column(name = "deduccion_isss", precision = 10, scale = 2)
    private BigDecimal deduccionIsss = BigDecimal.ZERO;

    /** AFP empleado: 7.25% */
    @Column(name = "deduccion_afp", precision = 10, scale = 2)
    private BigDecimal deduccionAfp = BigDecimal.ZERO;

    /** ISR según tablas de renta vigentes de El Salvador */
    @Column(name = "deduccion_renta", precision = 10, scale = 2)
    private BigDecimal deduccionRenta = BigDecimal.ZERO;

    /** Suma de todas las deducciones */
    @Column(name = "total_deducciones", precision = 10, scale = 2)
    private BigDecimal totalDeducciones = BigDecimal.ZERO;

    /** totalDevengado - totalDeducciones */
    @Column(name = "salario_neto", precision = 10, scale = 2)
    private BigDecimal salarioNeto = BigDecimal.ZERO;

    // ─── Aportaciones patronales ────────────────────────────────────────

    /** ISSS patronal: 7.5% */
    @Column(name = "aportacion_patronal_isss", precision = 10, scale = 2)
    private BigDecimal aportacionPatronalIsss = BigDecimal.ZERO;

    /** AFP patronal: 8.75% */
    @Column(name = "aportacion_patronal_afp", precision = 10, scale = 2)
    private BigDecimal aportacionPatronalAfp = BigDecimal.ZERO;

    // ─── Getters y Setters ──────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Planilla getPlanilla() {
        return planilla;
    }

    public void setPlanilla(Planilla planilla) {
        this.planilla = planilla;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public BigDecimal getSalarioBase() {
        return salarioBase;
    }

    public void setSalarioBase(BigDecimal salarioBase) {
        this.salarioBase = salarioBase;
    }

    public Double getHorasExtras() {
        return horasExtras;
    }

    public void setHorasExtras(Double horasExtras) {
        this.horasExtras = horasExtras;
    }

    public BigDecimal getMontoHorasExtras() {
        return montoHorasExtras;
    }

    public void setMontoHorasExtras(BigDecimal montoHorasExtras) {
        this.montoHorasExtras = montoHorasExtras;
    }

    public BigDecimal getOtrosIngresos() {
        return otrosIngresos;
    }

    public void setOtrosIngresos(BigDecimal otrosIngresos) {
        this.otrosIngresos = otrosIngresos;
    }

    public BigDecimal getTotalDevengado() {
        return totalDevengado;
    }

    public void setTotalDevengado(BigDecimal totalDevengado) {
        this.totalDevengado = totalDevengado;
    }

    public Integer getDiasAusenciaDescontados() {
        return diasAusenciaDescontados;
    }

    public void setDiasAusenciaDescontados(Integer diasAusenciaDescontados) {
        this.diasAusenciaDescontados = diasAusenciaDescontados;
    }

    public BigDecimal getMontoDescuentoAusencias() {
        return montoDescuentoAusencias;
    }

    public void setMontoDescuentoAusencias(BigDecimal montoDescuentoAusencias) {
        this.montoDescuentoAusencias = montoDescuentoAusencias;
    }

    public BigDecimal getDeduccionIsss() {
        return deduccionIsss;
    }

    public void setDeduccionIsss(BigDecimal deduccionIsss) {
        this.deduccionIsss = deduccionIsss;
    }

    public BigDecimal getDeduccionAfp() {
        return deduccionAfp;
    }

    public void setDeduccionAfp(BigDecimal deduccionAfp) {
        this.deduccionAfp = deduccionAfp;
    }

    public BigDecimal getDeduccionRenta() {
        return deduccionRenta;
    }

    public void setDeduccionRenta(BigDecimal deduccionRenta) {
        this.deduccionRenta = deduccionRenta;
    }

    public BigDecimal getTotalDeducciones() {
        return totalDeducciones;
    }

    public void setTotalDeducciones(BigDecimal totalDeducciones) {
        this.totalDeducciones = totalDeducciones;
    }

    public BigDecimal getSalarioNeto() {
        return salarioNeto;
    }

    public void setSalarioNeto(BigDecimal salarioNeto) {
        this.salarioNeto = salarioNeto;
    }

    public BigDecimal getAportacionPatronalIsss() {
        return aportacionPatronalIsss;
    }

    public void setAportacionPatronalIsss(BigDecimal aportacionPatronalIsss) {
        this.aportacionPatronalIsss = aportacionPatronalIsss;
    }

    public BigDecimal getAportacionPatronalAfp() {
        return aportacionPatronalAfp;
    }

    public void setAportacionPatronalAfp(BigDecimal aportacionPatronalAfp) {
        this.aportacionPatronalAfp = aportacionPatronalAfp;
    }
}
