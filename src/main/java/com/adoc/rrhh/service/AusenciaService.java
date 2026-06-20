package com.adoc.rrhh.service;

import com.adoc.rrhh.entity.Ausencia;
import com.adoc.rrhh.entity.Empleado;
import com.adoc.rrhh.entity.enums.EstadoAusencia;
import com.adoc.rrhh.entity.enums.EstadoEmpleado;
import com.adoc.rrhh.entity.enums.TipoAusencia;
import com.adoc.rrhh.repository.AusenciaRepository;
import com.adoc.rrhh.repository.EmpleadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de gestión de ausencias e incapacidades.
 *
 * Normativa aplicada (El Salvador):
 * - Incapacidad por enfermedad común: Patrono paga primeros 3 días al 100%.
 *   A partir del 4° día, ISSS cubre el 75% del salario (Reglamento ISSS).
 * - Incapacidad por accidente de trabajo: ISSS cubre desde el 1er día al 75%.
 *   El patrono complementa el 25% restante (Art. 324 C.T.).
 * - Incapacidad por maternidad: 16 semanas (112 días). ISSS paga 75%,
 *   patrono complementa 25% (Arts. 309-312 C.T.).
 * - Ausencia justificada con goce: duelo (3 días), matrimonio (5 días),
 *   citación judicial, etc. (Arts. 29 num. 4, 31 C.T.).
 * - Ausencia justificada sin goce: permiso personal sin salario (Art. 36 C.T.).
 * - Ausencia injustificada: se descuenta del salario. Más de 2 en el mismo mes
 *   constituye causal de despido (Arts. 50 num. 13, 58 C.T.).
 */
@Service
public class AusenciaService {

    private static final int DIAS_PATRONO_ENFERMEDAD = 3;
    private static final int DIAS_MATERNIDAD = 112; // 16 semanas

    private final AusenciaRepository ausenciaRepository;
    private final EmpleadoRepository empleadoRepository;

    public AusenciaService(AusenciaRepository ausenciaRepository,
                           EmpleadoRepository empleadoRepository) {
        this.ausenciaRepository = ausenciaRepository;
        this.empleadoRepository = empleadoRepository;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  REGISTRO
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Registra una nueva ausencia calculando automáticamente los días
     * que corresponden al patrono y al ISSS según la normativa.
     */
    @Transactional
    public Ausencia registrarAusencia(Ausencia ausencia) {
        // Calcular días totales (inclusive)
        ausencia.calcularDiasTotales();
        int dias = ausencia.getDiasTotales() != null ? ausencia.getDiasTotales() : 0;

        // Calcular distribución de responsabilidad según tipo
        switch (ausencia.getTipoAusencia()) {
            case INCAPACIDAD_ENFERMEDAD:
                // Patrono paga primeros 3 días; ISSS cubre el resto
                ausencia.setDiasPatrono(Math.min(dias, DIAS_PATRONO_ENFERMEDAD));
                ausencia.setDiasIsss(Math.max(dias - DIAS_PATRONO_ENFERMEDAD, 0));
                break;

            case INCAPACIDAD_ACCIDENTE:
                // ISSS cubre desde el día 1
                ausencia.setDiasPatrono(0);
                ausencia.setDiasIsss(dias);
                break;

            case INCAPACIDAD_MATERNIDAD:
                // Fijo a 112 días (16 semanas)
                ausencia.setDiasTotales(DIAS_MATERNIDAD);
                ausencia.setDiasPatrono(0);
                ausencia.setDiasIsss(DIAS_MATERNIDAD);
                break;

            case AUSENCIA_JUSTIFICADA_CON_GOCE:
                // El patrono asume todos los días con goce de salario
                ausencia.setDiasPatrono(dias);
                ausencia.setDiasIsss(0);
                break;

            case AUSENCIA_JUSTIFICADA_SIN_GOCE:
            case AUSENCIA_INJUSTIFICADA:
                // Ni patrono ni ISSS cubren el salario
                ausencia.setDiasPatrono(0);
                ausencia.setDiasIsss(0);
                break;
        }

        ausencia.setEstado(EstadoAusencia.PENDIENTE);
        ausencia.setFechaRegistro(LocalDate.now());

        return ausenciaRepository.save(ausencia);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  FLUJO DE APROBACIÓN
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Aprueba una ausencia. Si es una incapacidad, cambia el estado
     * del empleado a INCAPACITADO.
     */
    @Transactional
    public Ausencia aprobarAusencia(Long ausenciaId) {
        Ausencia ausencia = ausenciaRepository.findById(ausenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Ausencia no encontrada: " + ausenciaId));

        ausencia.setEstado(EstadoAusencia.APROBADA);

        // Si es una incapacidad, marcar al empleado como INCAPACITADO
        if (esIncapacidad(ausencia.getTipoAusencia())) {
            Empleado empleado = ausencia.getEmpleado();
            empleado.setEstado(EstadoEmpleado.INCAPACITADO);
            empleadoRepository.save(empleado);
        }

        return ausenciaRepository.save(ausencia);
    }

    /**
     * Rechaza una ausencia pendiente.
     */
    @Transactional
    public Ausencia rechazarAusencia(Long ausenciaId) {
        Ausencia ausencia = ausenciaRepository.findById(ausenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Ausencia no encontrada: " + ausenciaId));

        ausencia.setEstado(EstadoAusencia.RECHAZADA);
        return ausenciaRepository.save(ausencia);
    }

    /**
     * Finaliza una ausencia aprobada. Si el empleado estaba INCAPACITADO,
     * lo restaura a ACTIVO.
     */
    @Transactional
    public Ausencia finalizarAusencia(Long ausenciaId) {
        Ausencia ausencia = ausenciaRepository.findById(ausenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Ausencia no encontrada: " + ausenciaId));

        ausencia.setEstado(EstadoAusencia.FINALIZADA);

        // Restaurar al empleado a ACTIVO si estaba INCAPACITADO
        Empleado empleado = ausencia.getEmpleado();
        if (empleado.getEstado() == EstadoEmpleado.INCAPACITADO) {
            empleado.setEstado(EstadoEmpleado.ACTIVO);
            empleadoRepository.save(empleado);
        }

        return ausenciaRepository.save(ausencia);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CONSULTAS
    // ═══════════════════════════════════════════════════════════════════

    public List<Ausencia> listarTodas() {
        return ausenciaRepository.findAllByOrderByFechaRegistroDesc();
    }

    public List<Ausencia> listarPorFiltros(EstadoAusencia estado, TipoAusencia tipo) {
        if (estado != null && tipo != null) {
            return ausenciaRepository.findByEstadoAndTipoAusenciaOrderByFechaRegistroDesc(estado, tipo);
        } else if (estado != null) {
            return ausenciaRepository.findByEstadoOrderByFechaRegistroDesc(estado);
        } else if (tipo != null) {
            return ausenciaRepository.findByTipoAusenciaOrderByFechaRegistroDesc(tipo);
        }
        return listarTodas();
    }

    public Optional<Ausencia> buscarPorId(Long id) {
        return ausenciaRepository.findById(id);
    }

    /**
     * Cuenta las faltas injustificadas de un empleado en un mes determinado.
     * Art. 50 num. 13 C.T.: más de 2 faltas injustificadas en el mismo mes
     * constituye causal de despido justificado.
     */
    public long contarFaltasInjustificadasMes(Long empleadoId, int mes, int anio) {
        LocalDate inicioMes = LocalDate.of(anio, mes, 1);
        LocalDate finMes = inicioMes.withDayOfMonth(inicioMes.lengthOfMonth());
        return ausenciaRepository.countByEmpleadoIdAndTipoAusenciaAndFechaInicioBetween(
                empleadoId, TipoAusencia.AUSENCIA_INJUSTIFICADA, inicioMes, finMes);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  UTILIDADES
    // ═══════════════════════════════════════════════════════════════════

    private boolean esIncapacidad(TipoAusencia tipo) {
        return tipo == TipoAusencia.INCAPACIDAD_ENFERMEDAD
                || tipo == TipoAusencia.INCAPACIDAD_ACCIDENTE
                || tipo == TipoAusencia.INCAPACIDAD_MATERNIDAD;
    }
}
