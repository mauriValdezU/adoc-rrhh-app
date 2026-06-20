package com.adoc.rrhh.dto;

import com.adoc.rrhh.entity.Planilla;

import java.util.ArrayList;
import java.util.List;

public class ResultadoPlanillaDto {

    private Planilla planillaGenerada;
    private int empleadosGenerados;
    private List<String> empleadosOmitidos = new ArrayList<>();

    public ResultadoPlanillaDto() {
        this.empleadosGenerados = 0;
    }

    public Planilla getPlanillaGenerada() {
        return planillaGenerada;
    }

    public void setPlanillaGenerada(Planilla planillaGenerada) {
        this.planillaGenerada = planillaGenerada;
    }

    public int getEmpleadosGenerados() {
        return empleadosGenerados;
    }

    public void setEmpleadosGenerados(int empleadosGenerados) {
        this.empleadosGenerados = empleadosGenerados;
    }

    public void incrementarGenerados() {
        this.empleadosGenerados++;
    }

    public List<String> getEmpleadosOmitidos() {
        return empleadosOmitidos;
    }

    public void setEmpleadosOmitidos(List<String> empleadosOmitidos) {
        this.empleadosOmitidos = empleadosOmitidos;
    }

    public void agregarOmitido(String nombreEmpleado, String motivo) {
        this.empleadosOmitidos.add(nombreEmpleado + " - " + motivo);
    }
}
