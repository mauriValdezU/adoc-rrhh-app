package com.adoc.rrhh.entity;

import com.adoc.rrhh.entity.enums.EstadoEmpleado;
import com.adoc.rrhh.entity.enums.TipoJornada;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "empleados")
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(unique = true, length = 10)
    private String dui;

    @Column(unique = true, length = 17)
    private String nit;

    @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(length = 500)
    private String direccion;

    @Column(length = 15)
    private String telefono;

    @Column(length = 100)
    private String cargo;

    @Column(length = 100)
    private String departamento;

    @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    @Column(name = "salario_base", precision = 10, scale = 2)
    private BigDecimal salarioBase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoEmpleado estado = EstadoEmpleado.ACTIVO;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_jornada", nullable = false, length = 10)
    private TipoJornada tipoJornada = TipoJornada.DIURNA;

    @Column(name = "tipo_contrato", length = 50)
    private String tipoContrato;

    // Campos institucionales
    @Column(name = "cuenta_bancaria", length = 30)
    private String cuentaBancaria;

    @Column(length = 50)
    private String banco;

    @Column(name = "numero_isss", length = 20)
    private String numeroIsss;

    @Column(name = "numero_afp", length = 20)
    private String numeroAfp;

    // ─── Getters y Setters ──────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getDui() {
        return dui;
    }

    public void setDui(String dui) {
        this.dui = dui;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public BigDecimal getSalarioBase() {
        return salarioBase;
    }

    public void setSalarioBase(BigDecimal salarioBase) {
        this.salarioBase = salarioBase;
    }

    public EstadoEmpleado getEstado() {
        return estado;
    }

    public void setEstado(EstadoEmpleado estado) {
        this.estado = estado;
    }

    public TipoJornada getTipoJornada() {
        return tipoJornada;
    }

    public void setTipoJornada(TipoJornada tipoJornada) {
        this.tipoJornada = tipoJornada;
    }

    public String getTipoContrato() {
        return tipoContrato;
    }

    public void setTipoContrato(String tipoContrato) {
        this.tipoContrato = tipoContrato;
    }

    public String getCuentaBancaria() {
        return cuentaBancaria;
    }

    public void setCuentaBancaria(String cuentaBancaria) {
        this.cuentaBancaria = cuentaBancaria;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public String getNumeroIsss() {
        return numeroIsss;
    }

    public void setNumeroIsss(String numeroIsss) {
        this.numeroIsss = numeroIsss;
    }

    public String getNumeroAfp() {
        return numeroAfp;
    }

    public void setNumeroAfp(String numeroAfp) {
        this.numeroAfp = numeroAfp;
    }

    /**
     * Devuelve el nombre completo del empleado.
     */
    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }
}
