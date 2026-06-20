package com.adoc.rrhh.service;

import com.adoc.rrhh.dto.ResultadoPlanillaDto;
import com.adoc.rrhh.entity.DetallePlanilla;
import com.adoc.rrhh.entity.Empleado;
import com.adoc.rrhh.entity.Planilla;
import com.adoc.rrhh.entity.enums.EstadoEmpleado;
import com.adoc.rrhh.entity.enums.TipoJornada;
import com.adoc.rrhh.entity.enums.TipoPlanilla;
import com.adoc.rrhh.repository.DetallePlanillaRepository;
import com.adoc.rrhh.repository.EmpleadoRepository;
import com.adoc.rrhh.repository.PlanillaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio de cálculo de planilla.
 *
 * Contempla:
 * - Deducciones del empleado: ISSS (3%), AFP (7.25%), Renta ISR (tablas vigentes El Salvador)
 * - Aportaciones patronales: ISSS (7.5%), AFP (8.75%)
 * - Cálculo diferenciado de horas extras según jornada (DIURNA / NOCTURNA)
 */
@Service
public class PayrollService {

    // ─── Constantes de deducciones ──────────────────────────────────────
    private static final BigDecimal ISSS_EMPLEADO_PCT     = new BigDecimal("0.03");    // 3%
    private static final BigDecimal AFP_EMPLEADO_PCT      = new BigDecimal("0.0725");  // 7.25%
    private static final BigDecimal ISSS_PATRONAL_PCT     = new BigDecimal("0.075");   // 7.5%
    private static final BigDecimal AFP_PATRONAL_PCT      = new BigDecimal("0.0875");  // 8.75%

    /** Tope mensual de ISSS empleado: $30.00 */
    private static final BigDecimal ISSS_TOPE_MENSUAL     = new BigDecimal("30.00");

    // ─── Constantes de jornada ──────────────────────────────────────────
    private static final BigDecimal DIAS_MES               = new BigDecimal("30");
    private static final BigDecimal HORAS_DIURNA           = new BigDecimal("8");
    private static final BigDecimal HORAS_NOCTURNA         = new BigDecimal("7");
    private static final BigDecimal FACTOR_EXTRA_DIURNA    = new BigDecimal("2");    // doble
    private static final BigDecimal FACTOR_EXTRA_NOCTURNA  = new BigDecimal("2.5");  // doble + 25% recargo nocturno

    // ─── Tabla de Renta ISR mensual vigente (El Salvador) ───────────────
    //  Tramo I:   $0.01   – $472.00    →  0%   sobre exceso de $0.00   + cuota fija $0.00
    //  Tramo II:  $472.01 – $895.24    → 10%   sobre exceso de $472.00 + cuota fija $17.67
    //  Tramo III: $895.25 – $2,038.10  → 20%   sobre exceso de $895.24 + cuota fija $60.00
    //  Tramo IV:  $2,038.11 en adelante → 30%  sobre exceso de $2,038.10 + cuota fija $288.57

    private static final BigDecimal RENTA_TRAMO_I_HASTA      = new BigDecimal("472.00");
    private static final BigDecimal RENTA_TRAMO_II_HASTA     = new BigDecimal("895.24");
    private static final BigDecimal RENTA_TRAMO_III_HASTA    = new BigDecimal("2038.10");

    private static final BigDecimal RENTA_TRAMO_II_EXCESO    = new BigDecimal("472.00");
    private static final BigDecimal RENTA_TRAMO_II_PCT       = new BigDecimal("0.10");
    private static final BigDecimal RENTA_TRAMO_II_CUOTA     = new BigDecimal("17.67");

    private static final BigDecimal RENTA_TRAMO_III_EXCESO   = new BigDecimal("895.24");
    private static final BigDecimal RENTA_TRAMO_III_PCT      = new BigDecimal("0.20");
    private static final BigDecimal RENTA_TRAMO_III_CUOTA    = new BigDecimal("60.00");

    private static final BigDecimal RENTA_TRAMO_IV_EXCESO    = new BigDecimal("2038.10");
    private static final BigDecimal RENTA_TRAMO_IV_PCT       = new BigDecimal("0.30");
    private static final BigDecimal RENTA_TRAMO_IV_CUOTA     = new BigDecimal("288.57");

    private final EmpleadoRepository empleadoRepository;
    private final PlanillaRepository planillaRepository;
    private final DetallePlanillaRepository detallePlanillaRepository;
    private final com.adoc.rrhh.repository.AusenciaRepository ausenciaRepository;

    public PayrollService(EmpleadoRepository empleadoRepository,
                          PlanillaRepository planillaRepository,
                          DetallePlanillaRepository detallePlanillaRepository,
                          com.adoc.rrhh.repository.AusenciaRepository ausenciaRepository) {
        this.empleadoRepository = empleadoRepository;
        this.planillaRepository = planillaRepository;
        this.detallePlanillaRepository = detallePlanillaRepository;
        this.ausenciaRepository = ausenciaRepository;
    }

    private boolean esEmpleadoValidoParaPeriodo(Empleado empleado, String periodo, TipoPlanilla tipoPlanilla, ResultadoPlanillaDto resultado) {
        if (empleado.getFechaIngreso() != null) {
            if (periodo.length() == 4) {
                int anioIngreso = empleado.getFechaIngreso().getYear();
                int anioPeriodo = Integer.parseInt(periodo);
                if (anioIngreso > anioPeriodo) {
                    resultado.agregarOmitido(empleado.getNombreCompleto(), "Fecha de ingreso posterior al año del periodo");
                    return false;
                }
            } else {
                YearMonth ymPeriodo = YearMonth.parse(periodo, DateTimeFormatter.ofPattern("yyyy-MM"));
                YearMonth ymIngreso = YearMonth.from(empleado.getFechaIngreso());
                if (ymIngreso.isAfter(ymPeriodo)) {
                    resultado.agregarOmitido(empleado.getNombreCompleto(), "Fecha de ingreso posterior al periodo");
                    return false;
                }
            }
        }

        boolean existe = detallePlanillaRepository.existsByEmpleadoIdAndPlanillaPeriodoAndPlanillaTipoPlanilla(
                empleado.getId(), periodo, tipoPlanilla);
        if (existe) {
            resultado.agregarOmitido(empleado.getNombreCompleto(), "La planilla ya fue generada para este periodo");
            return false;
        }

        return true;
    }

    // ════════════════════════════════════════════════════════════════════
    //  MÉTODO PRINCIPAL: Generar planilla
    // ════════════════════════════════════════════════════════════════════

    /**
     * Genera una planilla ordinaria para todos los empleados activos.
     *
     * @param periodo   periodo en formato "YYYY-MM"
     * @return la planilla generada con todos sus detalles calculados
     */
    @Transactional
    public ResultadoPlanillaDto generarPlanillaOrdinaria(String periodo, List<Long> empleadoIds) {
        ResultadoPlanillaDto resultado = new ResultadoPlanillaDto();
        Planilla planilla = new Planilla();
        planilla.setPeriodo(periodo);
        planilla.setTipoPlanilla(TipoPlanilla.ORDINARIA);
        planilla.setFechaGeneracion(LocalDate.now());

        List<Empleado> empleadosAProcesar = empleadoRepository.findAllById(empleadoIds);

        // Determinar rango de fechas del mes completo
        YearMonth ym = YearMonth.parse(periodo, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate inicioMes = ym.atDay(1);
        LocalDate finMes = ym.atEndOfMonth();

        boolean tieneDetalles = false;
        for (Empleado empleado : empleadosAProcesar) {
            if (!esEmpleadoValidoParaPeriodo(empleado, periodo, TipoPlanilla.ORDINARIA, resultado)) {
                continue;
            }
            BigDecimal salarioProrrateado = calcularSalarioProrrateado(empleado, periodo);

            // Descontar días de ausencia aprobadas en el período
            int diasADescontar = calcularDiasADescontar(empleado.getId(), inicioMes, finMes);
            BigDecimal montoDescuento = BigDecimal.ZERO;
            if (diasADescontar > 0) {
                BigDecimal salarioDiario = empleado.getSalarioBase().divide(DIAS_MES, 10, RoundingMode.HALF_UP);
                montoDescuento = salarioDiario.multiply(BigDecimal.valueOf(diasADescontar)).setScale(2, RoundingMode.HALF_UP);
                salarioProrrateado = salarioProrrateado.subtract(montoDescuento);
                if (salarioProrrateado.compareTo(BigDecimal.ZERO) < 0) {
                    salarioProrrateado = BigDecimal.ZERO;
                    montoDescuento = empleado.getSalarioBase(); // Cap the discount to max salary
                }
            }

            DetallePlanilla detalle = calcularDetalleEmpleado(empleado, 0.0, BigDecimal.ZERO, salarioProrrateado, diasADescontar, montoDescuento);
            planilla.agregarDetalle(detalle);
            resultado.incrementarGenerados();
            tieneDetalles = true;
        }

        if (tieneDetalles) {
            planilla = planillaRepository.save(planilla);
            resultado.setPlanillaGenerada(planilla);
        }
        return resultado;
    }

    /**
     * Genera una planilla ordinaria incluyendo horas extras por empleado.
     * El parámetro horasExtrasPorEmpleado puede ser null para empleados sin horas extras.
     */
    @Transactional
    public ResultadoPlanillaDto generarPlanillaConHorasExtras(String periodo, List<Long> empleadoIds,
                                                   java.util.Map<Long, Double> horasExtrasPorEmpleado) {
        ResultadoPlanillaDto resultado = new ResultadoPlanillaDto();
        Planilla planilla = new Planilla();
        planilla.setPeriodo(periodo);
        planilla.setTipoPlanilla(TipoPlanilla.ORDINARIA);
        planilla.setFechaGeneracion(LocalDate.now());

        List<Empleado> empleadosAProcesar = empleadoRepository.findAllById(empleadoIds);

        // Determinar rango de fechas del mes completo
        YearMonth ym = YearMonth.parse(periodo, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate inicioMes = ym.atDay(1);
        LocalDate finMes = ym.atEndOfMonth();

        boolean tieneDetalles = false;
        for (Empleado empleado : empleadosAProcesar) {
            if (!esEmpleadoValidoParaPeriodo(empleado, periodo, TipoPlanilla.ORDINARIA, resultado)) {
                continue;
            }
            double horas = horasExtrasPorEmpleado != null
                    ? horasExtrasPorEmpleado.getOrDefault(empleado.getId(), 0.0)
                    : 0.0;
            BigDecimal salarioProrrateado = calcularSalarioProrrateado(empleado, periodo);

            // Descontar días de ausencia aprobadas en el período
            int diasADescontar = calcularDiasADescontar(empleado.getId(), inicioMes, finMes);
            BigDecimal montoDescuento = BigDecimal.ZERO;
            if (diasADescontar > 0) {
                BigDecimal salarioDiario = empleado.getSalarioBase().divide(DIAS_MES, 10, RoundingMode.HALF_UP);
                montoDescuento = salarioDiario.multiply(BigDecimal.valueOf(diasADescontar)).setScale(2, RoundingMode.HALF_UP);
                salarioProrrateado = salarioProrrateado.subtract(montoDescuento);
                if (salarioProrrateado.compareTo(BigDecimal.ZERO) < 0) {
                    salarioProrrateado = BigDecimal.ZERO;
                    montoDescuento = empleado.getSalarioBase(); // Cap the discount to max salary
                }
            }

            DetallePlanilla detalle = calcularDetalleEmpleado(empleado, horas, BigDecimal.ZERO, salarioProrrateado, diasADescontar, montoDescuento);
            planilla.agregarDetalle(detalle);
            resultado.incrementarGenerados();
            tieneDetalles = true;
        }

        if (tieneDetalles) {
            planilla = planillaRepository.save(planilla);
            resultado.setPlanillaGenerada(planilla);
        }
        return resultado;
    }

    // ════════════════════════════════════════════════════════════════════
    //  PLANILLAS ESPECIALES
    // ════════════════════════════════════════════════════════════════════

    @Transactional
    public ResultadoPlanillaDto generarPlanillaAguinaldo(String periodo, List<Long> empleadoIds) {
        ResultadoPlanillaDto resultado = new ResultadoPlanillaDto();
        Planilla planilla = new Planilla();
        planilla.setPeriodo(periodo);
        planilla.setTipoPlanilla(TipoPlanilla.AGUINALDO);
        planilla.setFechaGeneracion(LocalDate.now());

        List<Empleado> empleadosAProcesar = empleadoRepository.findAllById(empleadoIds);
        BigDecimal techoExento = new BigDecimal("1500.00");

        boolean tieneDetalles = false;
        for (Empleado empleado : empleadosAProcesar) {
            if (!esEmpleadoValidoParaPeriodo(empleado, periodo, TipoPlanilla.AGUINALDO, resultado)) {
                continue;
            }
            DetallePlanilla detalle = new DetallePlanilla();
            detalle.setEmpleado(empleado);
            detalle.setSalarioBase(empleado.getSalarioBase());
            
            // Cálculo de antigüedad (asumiendo corte 12 de diciembre del año en curso)
            int anioCorte = LocalDate.now().getYear();
            LocalDate fechaCorte = LocalDate.of(anioCorte, 12, 12);
            LocalDate fechaIngreso = empleado.getFechaIngreso() != null ? empleado.getFechaIngreso() : LocalDate.now();
            
            long diasLaborados = java.time.temporal.ChronoUnit.DAYS.between(fechaIngreso, fechaCorte);
            double aniosServicio = diasLaborados / 365.25;

            BigDecimal diasAguinaldo = BigDecimal.ZERO;
            if (aniosServicio >= 1 && aniosServicio < 3) {
                diasAguinaldo = new BigDecimal("15");
            } else if (aniosServicio >= 3 && aniosServicio < 10) {
                diasAguinaldo = new BigDecimal("19");
            } else if (aniosServicio >= 10) {
                diasAguinaldo = new BigDecimal("21");
            } else if (aniosServicio > 0 && aniosServicio < 1) {
                // Proporcional
                diasAguinaldo = new BigDecimal("15").multiply(BigDecimal.valueOf(diasLaborados)).divide(new BigDecimal("365"), 2, RoundingMode.HALF_UP);
            }

            BigDecimal salarioDiario = empleado.getSalarioBase().divide(DIAS_MES, 10, RoundingMode.HALF_UP);
            BigDecimal totalAguinaldo = salarioDiario.multiply(diasAguinaldo).setScale(2, RoundingMode.HALF_UP);

            detalle.setOtrosIngresos(totalAguinaldo);
            detalle.setTotalDevengado(totalAguinaldo);
            
            // Deducciones: ISSS y AFP no aplican. Renta aplica sobre el exceso de $1500
            detalle.setDeduccionIsss(BigDecimal.ZERO);
            detalle.setDeduccionAfp(BigDecimal.ZERO);
            
            BigDecimal baseGravable = totalAguinaldo.subtract(techoExento);
            if (baseGravable.compareTo(BigDecimal.ZERO) > 0) {
                detalle.setDeduccionRenta(calcularRenta(baseGravable));
            } else {
                detalle.setDeduccionRenta(BigDecimal.ZERO);
            }

            detalle.setTotalDeducciones(detalle.getDeduccionRenta());
            detalle.setSalarioNeto(totalAguinaldo.subtract(detalle.getTotalDeducciones()));
            
            // Aportaciones patronales: no aplican
            detalle.setAportacionPatronalIsss(BigDecimal.ZERO);
            detalle.setAportacionPatronalAfp(BigDecimal.ZERO);

            planilla.agregarDetalle(detalle);
            resultado.incrementarGenerados();
            tieneDetalles = true;
        }

        if (tieneDetalles) {
            planilla = planillaRepository.save(planilla);
            resultado.setPlanillaGenerada(planilla);
        }
        return resultado;
    }

    @Transactional
    public ResultadoPlanillaDto generarPlanillaVacaciones(String periodo, List<Long> empleadoIds) {
        ResultadoPlanillaDto resultado = new ResultadoPlanillaDto();
        Planilla planilla = new Planilla();
        planilla.setPeriodo(periodo);
        planilla.setTipoPlanilla(TipoPlanilla.VACACIONES);
        planilla.setFechaGeneracion(LocalDate.now());

        List<Empleado> empleadosAProcesar = empleadoRepository.findAllById(empleadoIds);

        boolean tieneDetalles = false;
        for (Empleado empleado : empleadosAProcesar) {
            if (!esEmpleadoValidoParaPeriodo(empleado, periodo, TipoPlanilla.VACACIONES, resultado)) {
                continue;
            }
            DetallePlanilla detalle = new DetallePlanilla();
            detalle.setEmpleado(empleado);
            detalle.setSalarioBase(empleado.getSalarioBase());
            
            // Vacaciones: 15 días + 30% recargo
            BigDecimal salarioDiario = empleado.getSalarioBase().divide(DIAS_MES, 10, RoundingMode.HALF_UP);
            BigDecimal quinceDias = salarioDiario.multiply(new BigDecimal("15"));
            BigDecimal totalVacaciones = quinceDias.multiply(new BigDecimal("1.30")).setScale(2, RoundingMode.HALF_UP);

            detalle.setOtrosIngresos(totalVacaciones);
            detalle.setTotalDevengado(totalVacaciones);
            
            // Deducciones de ley aplican normalmente sobre el devengado de vacaciones
            detalle.setDeduccionIsss(calcularIsssEmpleado(totalVacaciones));
            detalle.setDeduccionAfp(calcularAfpEmpleado(totalVacaciones));
            BigDecimal baseGravable = totalVacaciones.subtract(detalle.getDeduccionIsss()).subtract(detalle.getDeduccionAfp());
            detalle.setDeduccionRenta(calcularRenta(baseGravable));

            detalle.setTotalDeducciones(detalle.getDeduccionIsss().add(detalle.getDeduccionAfp()).add(detalle.getDeduccionRenta()));
            detalle.setSalarioNeto(totalVacaciones.subtract(detalle.getTotalDeducciones()));
            
            detalle.setAportacionPatronalIsss(calcularIsssPatronal(totalVacaciones));
            detalle.setAportacionPatronalAfp(calcularAfpPatronal(totalVacaciones));

            planilla.agregarDetalle(detalle);
            resultado.incrementarGenerados();
            tieneDetalles = true;
        }

        if (tieneDetalles) {
            planilla = planillaRepository.save(planilla);
            resultado.setPlanillaGenerada(planilla);
        }
        return resultado;
    }

    @Transactional
    public ResultadoPlanillaDto generarPlanilla25(String periodo, List<Long> empleadoIds) {
        ResultadoPlanillaDto resultado = new ResultadoPlanillaDto();
        Planilla planilla = new Planilla();
        planilla.setPeriodo(periodo);
        planilla.setTipoPlanilla(TipoPlanilla.PLANILLA_25);
        planilla.setFechaGeneracion(LocalDate.now());

        List<Empleado> empleadosAProcesar = empleadoRepository.findAllById(empleadoIds);
        BigDecimal limiteSalarial = new BigDecimal("1500.00");

        boolean tieneDetalles = false;
        for (Empleado empleado : empleadosAProcesar) {
            if (!esEmpleadoValidoParaPeriodo(empleado, periodo, TipoPlanilla.PLANILLA_25, resultado)) {
                continue;
            }
            DetallePlanilla detalle = new DetallePlanilla();
            detalle.setEmpleado(empleado);
            detalle.setSalarioBase(empleado.getSalarioBase());
            
            BigDecimal bonoPlanilla25 = BigDecimal.ZERO;
            
            // Aplica solo para salarios menores a $1500
            if (empleado.getSalarioBase().compareTo(limiteSalarial) < 0) {
                // 15 días de salario
                bonoPlanilla25 = empleado.getSalarioBase().divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
            }

            detalle.setOtrosIngresos(bonoPlanilla25);
            detalle.setTotalDevengado(bonoPlanilla25);
            
            // Gratificación suele estar exenta de ISSS/AFP, pero sí paga Renta
            detalle.setDeduccionIsss(BigDecimal.ZERO);
            detalle.setDeduccionAfp(BigDecimal.ZERO);
            detalle.setDeduccionRenta(calcularRenta(bonoPlanilla25));

            detalle.setTotalDeducciones(detalle.getDeduccionRenta());
            detalle.setSalarioNeto(bonoPlanilla25.subtract(detalle.getTotalDeducciones()));
            
            detalle.setAportacionPatronalIsss(BigDecimal.ZERO);
            detalle.setAportacionPatronalAfp(BigDecimal.ZERO);

            planilla.agregarDetalle(detalle);
            resultado.incrementarGenerados();
            tieneDetalles = true;
        }

        if (tieneDetalles) {
            planilla = planillaRepository.save(planilla);
            resultado.setPlanillaGenerada(planilla);
        }
        return resultado;
    }

    // ════════════════════════════════════════════════════════════════════
    //  CÁLCULO POR EMPLEADO

    private BigDecimal calcularSalarioProrrateado(Empleado empleado, String periodo) {
        if (empleado.getFechaIngreso() == null) {
            return empleado.getSalarioBase();
        }
        
        YearMonth ymPeriodo = YearMonth.parse(periodo, DateTimeFormatter.ofPattern("yyyy-MM"));
        YearMonth ymIngreso = YearMonth.from(empleado.getFechaIngreso());
        
        if (!ymIngreso.equals(ymPeriodo)) {
            // Ingresó antes de este mes, devenga el mes completo
            return empleado.getSalarioBase();
        }
        
        // Si ingresó este mismo mes, calcular proporcional
        int diaIngreso = empleado.getFechaIngreso().getDayOfMonth();
        int diasTrabajados = 30 - diaIngreso + 1; 
        if (diasTrabajados < 0) diasTrabajados = 0; // Por si ingresa el 31
        if (diasTrabajados > 30) diasTrabajados = 30; // Limitar a mes comercial de 30 días
        
        BigDecimal salarioDiario = empleado.getSalarioBase().divide(DIAS_MES, 10, RoundingMode.HALF_UP);
        return salarioDiario.multiply(BigDecimal.valueOf(diasTrabajados)).setScale(2, RoundingMode.HALF_UP);
    }

    // ════════════════════════════════════════════════════════════════════

    /**
     * Calcula el detalle de planilla para un empleado individual.
     *
     * @param empleado      el empleado
     * @param horasExtras   cantidad de horas extras trabajadas
     * @param otrosIngresos ingresos adicionales (bonos, comisiones, etc.)
     * @param salarioPeriodo salario proporcional a los días laborados
     * @param diasADescontar días de ausencias injustificadas o ISSS a descontar
     * @param montoDescuento monto en $ descontado por las ausencias
     * @return DetallePlanilla con todos los cálculos realizados
     */
    public DetallePlanilla calcularDetalleEmpleado(Empleado empleado, double horasExtras,
                                                    BigDecimal otrosIngresos, BigDecimal salarioPeriodo,
                                                    int diasADescontar, BigDecimal montoDescuento) {
        DetallePlanilla detalle = new DetallePlanilla();
        detalle.setEmpleado(empleado);

        detalle.setSalarioBase(empleado.getSalarioBase());
        detalle.setDiasAusenciaDescontados(diasADescontar);
        detalle.setMontoDescuentoAusencias(montoDescuento);
        detalle.setHorasExtras(horasExtras);

        // 1. Calcular monto de horas extras (utilizando el salario base mensual para el valor hora)
        BigDecimal montoHorasExtras = calcularHorasExtras(empleado.getTipoJornada(), empleado.getSalarioBase(), horasExtras);
        detalle.setMontoHorasExtras(montoHorasExtras);

        // 2. Otros ingresos
        detalle.setOtrosIngresos(otrosIngresos != null ? otrosIngresos : BigDecimal.ZERO);

        // 3. Total devengado
        BigDecimal totalDevengado = salarioPeriodo.add(montoHorasExtras).add(detalle.getOtrosIngresos());
        detalle.setTotalDevengado(totalDevengado);

        // 4. Deducciones del empleado
        BigDecimal deduccionIsss = calcularIsssEmpleado(totalDevengado);
        BigDecimal deduccionAfp  = calcularAfpEmpleado(totalDevengado);
        BigDecimal baseGravable  = totalDevengado.subtract(deduccionIsss).subtract(deduccionAfp);
        BigDecimal deduccionRenta = calcularRenta(baseGravable);

        detalle.setDeduccionIsss(deduccionIsss);
        detalle.setDeduccionAfp(deduccionAfp);
        detalle.setDeduccionRenta(deduccionRenta);

        // 5. Total deducciones
        BigDecimal totalDeducciones = deduccionIsss.add(deduccionAfp).add(deduccionRenta);
        detalle.setTotalDeducciones(totalDeducciones);

        // 6. Salario neto
        BigDecimal salarioNeto = totalDevengado.subtract(totalDeducciones);
        detalle.setSalarioNeto(salarioNeto);

        // 7. Aportaciones patronales
        detalle.setAportacionPatronalIsss(calcularIsssPatronal(totalDevengado));
        detalle.setAportacionPatronalAfp(calcularAfpPatronal(totalDevengado));

        return detalle;
    }

    // ════════════════════════════════════════════════════════════════════
    //  DESCUENTO POR AUSENCIAS
    // ════════════════════════════════════════════════════════════════════

    /**
     * Calcula los días a descontar del salario de un empleado basándose
     * en las ausencias aprobadas que caen dentro del rango de la quincena.
     *
     * Se descuentan:
     * - Días cubiertos por ISSS (enfermedad desde el 4° día, accidente, maternidad)
     * - Días sin goce de salario (ausencia justificada sin goce)
     * - Días injustificados
     *
     * NO se descuentan:
     * - Días que paga el patrono en enfermedad común (primeros 3 días)
     * - Ausencias justificadas con goce de salario
     */
    private int calcularDiasADescontar(Long empleadoId, LocalDate inicioQuincena, LocalDate finQuincena) {
        List<com.adoc.rrhh.entity.Ausencia> ausencias = ausenciaRepository.findByEmpleadoIdAndFechaInicioBetween(
                empleadoId, inicioQuincena, finQuincena);

        int diasDescuento = 0;
        for (com.adoc.rrhh.entity.Ausencia ausencia : ausencias) {
            // Solo considerar ausencias aprobadas o finalizadas
            if (ausencia.getEstado() != com.adoc.rrhh.entity.enums.EstadoAusencia.APROBADA
                    && ausencia.getEstado() != com.adoc.rrhh.entity.enums.EstadoAusencia.FINALIZADA) {
                continue;
            }

            // Calcular cuántos días de esta ausencia caen dentro de la quincena
            LocalDate inicioEfectivo = ausencia.getFechaInicio().isBefore(inicioQuincena)
                    ? inicioQuincena : ausencia.getFechaInicio();
            LocalDate finEfectivo = ausencia.getFechaFin().isAfter(finQuincena)
                    ? finQuincena : ausencia.getFechaFin();
            int diasEnQuincena = (int) java.time.temporal.ChronoUnit.DAYS.between(inicioEfectivo, finEfectivo) + 1;
            if (diasEnQuincena <= 0) continue;

            switch (ausencia.getTipoAusencia()) {
                case INCAPACIDAD_ENFERMEDAD:
                    // Patrono paga primeros 3 días, ISSS el resto → descontar solo días ISSS
                    int diasIsss = ausencia.getDiasIsss() != null ? ausencia.getDiasIsss() : 0;
                    // Proporción de días ISSS que caen en esta quincena
                    int diasPatrono = ausencia.getDiasPatrono() != null ? ausencia.getDiasPatrono() : 0;
                    int diasIsssEnQuincena = Math.max(diasEnQuincena - diasPatrono, 0);
                    diasDescuento += diasIsssEnQuincena;
                    break;

                case INCAPACIDAD_ACCIDENTE:
                case INCAPACIDAD_MATERNIDAD:
                    // ISSS cubre todos los días → descontar todos
                    diasDescuento += diasEnQuincena;
                    break;

                case AUSENCIA_JUSTIFICADA_SIN_GOCE:
                case AUSENCIA_INJUSTIFICADA:
                    // No se paga salario → descontar todos
                    diasDescuento += diasEnQuincena;
                    break;

                case AUSENCIA_JUSTIFICADA_CON_GOCE:
                    // Con goce de salario → NO se descuenta
                    break;
            }
        }
        return diasDescuento;
    }

    // ════════════════════════════════════════════════════════════════════
    //  CÁLCULOS INDIVIDUALES
    // ════════════════════════════════════════════════════════════════════

    /**
     * Calcula el monto de horas extras según el tipo de jornada.
     *
     * Jornada DIURNA:   valorHora = salarioBase / 30 / 8;  extra = valorHora × 2
     * Jornada NOCTURNA: valorHora = salarioBase / 30 / 7;  extra = valorHora × 2.5
     */
    public BigDecimal calcularHorasExtras(TipoJornada tipoJornada, BigDecimal salarioBase, double horasExtras) {
        if (horasExtras <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal horasDiarias;
        BigDecimal factorExtra;

        if (tipoJornada == TipoJornada.NOCTURNA) {
            horasDiarias = HORAS_NOCTURNA;
            factorExtra  = FACTOR_EXTRA_NOCTURNA;
        } else {
            horasDiarias = HORAS_DIURNA;
            factorExtra  = FACTOR_EXTRA_DIURNA;
        }

        // Valor de la hora ordinaria
        BigDecimal valorHora = salarioBase
                .divide(DIAS_MES, 10, RoundingMode.HALF_UP)
                .divide(horasDiarias, 10, RoundingMode.HALF_UP);

        // Monto por horas extras
        return valorHora
                .multiply(factorExtra)
                .multiply(BigDecimal.valueOf(horasExtras))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * ISSS empleado: 3% del total devengado.
     * Tope mensual: $30.00. Para quincenas se aplica proporcional ($15.00).
     */
    public BigDecimal calcularIsssEmpleado(BigDecimal totalDevengado) {
        BigDecimal isss = totalDevengado.multiply(ISSS_EMPLEADO_PCT).setScale(2, RoundingMode.HALF_UP);
        // Aplicar tope mensual
        if (isss.compareTo(ISSS_TOPE_MENSUAL) > 0) {
            isss = ISSS_TOPE_MENSUAL;
        }
        return isss;
    }

    /**
     * AFP empleado: 7.25% del total devengado.
     */
    public BigDecimal calcularAfpEmpleado(BigDecimal totalDevengado) {
        return totalDevengado.multiply(AFP_EMPLEADO_PCT).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula el Impuesto Sobre la Renta (ISR) según tablas mensuales vigentes de El Salvador.
     *
     * La base gravable es: totalDevengado - ISSS - AFP
     *
     * Tramos mensuales:
     *   I:   $0.01   – $472.00     →  0%
     *   II:  $472.01 – $895.24     → 10% sobre exceso de $472.00 + $17.67
     *   III: $895.25 – $2,038.10   → 20% sobre exceso de $895.24 + $60.00
     *   IV:  $2,038.11 en adelante → 30% sobre exceso de $2,038.10 + $288.57
     */
    public BigDecimal calcularRenta(BigDecimal baseGravable) {
        if (baseGravable == null || baseGravable.compareTo(RENTA_TRAMO_I_HASTA) <= 0) {
            // Tramo I: exento
            return BigDecimal.ZERO;
        }

        if (baseGravable.compareTo(RENTA_TRAMO_II_HASTA) <= 0) {
            // Tramo II
            BigDecimal exceso = baseGravable.subtract(RENTA_TRAMO_II_EXCESO);
            return exceso.multiply(RENTA_TRAMO_II_PCT)
                    .add(RENTA_TRAMO_II_CUOTA)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        if (baseGravable.compareTo(RENTA_TRAMO_III_HASTA) <= 0) {
            // Tramo III
            BigDecimal exceso = baseGravable.subtract(RENTA_TRAMO_III_EXCESO);
            return exceso.multiply(RENTA_TRAMO_III_PCT)
                    .add(RENTA_TRAMO_III_CUOTA)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Tramo IV
        BigDecimal exceso = baseGravable.subtract(RENTA_TRAMO_IV_EXCESO);
        return exceso.multiply(RENTA_TRAMO_IV_PCT)
                .add(RENTA_TRAMO_IV_CUOTA)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * ISSS patronal: 7.5% del total devengado.
     */
    public BigDecimal calcularIsssPatronal(BigDecimal totalDevengado) {
        return totalDevengado.multiply(ISSS_PATRONAL_PCT).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * AFP patronal: 8.75% del total devengado.
     */
    public BigDecimal calcularAfpPatronal(BigDecimal totalDevengado) {
        return totalDevengado.multiply(AFP_PATRONAL_PCT).setScale(2, RoundingMode.HALF_UP);
    }
}
