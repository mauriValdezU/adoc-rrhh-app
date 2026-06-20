package com.adoc.rrhh.controller;

import com.adoc.rrhh.dto.ResultadoPlanillaDto;
import com.adoc.rrhh.entity.DetallePlanilla;
import com.adoc.rrhh.entity.Empleado;
import com.adoc.rrhh.entity.Planilla;
import com.adoc.rrhh.entity.Usuario;
import com.adoc.rrhh.entity.enums.EstadoEmpleado;
import com.adoc.rrhh.entity.enums.TipoPlanilla;
import com.adoc.rrhh.repository.EmpleadoRepository;
import com.adoc.rrhh.repository.PlanillaRepository;
import com.adoc.rrhh.service.PayrollService;
import com.adoc.rrhh.service.PdfGenerationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/planillas")
public class PlanillaController {

    private final PlanillaRepository planillaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PayrollService payrollService;
    private final PdfGenerationService pdfGenerationService;

    public PlanillaController(PlanillaRepository planillaRepository,
            EmpleadoRepository empleadoRepository,
            PayrollService payrollService,
            PdfGenerationService pdfGenerationService) {
        this.planillaRepository = planillaRepository;
        this.empleadoRepository = empleadoRepository;
        this.payrollService = payrollService;
        this.pdfGenerationService = pdfGenerationService;
    }

    private boolean isAuthenticated(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        return usuario != null;
    }

    @GetMapping
    public String gestionarPlanilla(
            @RequestParam(required = false) Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session, Model model) {
        if (!isAuthenticated(session)) {
            return "redirect:/login";
        }

        if (id == null) {
            org.springframework.data.domain.Page<Planilla> planillas = planillaRepository.findAll(
                    org.springframework.data.domain.PageRequest.of(page, size,
                            org.springframework.data.domain.Sort.by("id").descending()));
            model.addAttribute("planillas", planillas);
            return "listar-planillas";
        }

        Planilla planilla = planillaRepository.findById(id).orElse(null);

        if (planilla == null) {
            return "redirect:/planillas";
        }

        model.addAttribute("planilla", planilla);

        // Calcular totales para la columna derecha
        BigDecimal totalIsssEmpleado = BigDecimal.ZERO;
        BigDecimal totalAfpEmpleado = BigDecimal.ZERO;
        BigDecimal totalRenta = BigDecimal.ZERO;

        BigDecimal totalIsssPatrono = BigDecimal.ZERO;
        BigDecimal totalAfpPatrono = BigDecimal.ZERO;

        BigDecimal liquidoTotal = BigDecimal.ZERO;

        for (DetallePlanilla detalle : planilla.getDetalles()) {
            totalIsssEmpleado = totalIsssEmpleado.add(detalle.getDeduccionIsss());
            totalAfpEmpleado = totalAfpEmpleado.add(detalle.getDeduccionAfp());
            totalRenta = totalRenta.add(detalle.getDeduccionRenta());

            totalIsssPatrono = totalIsssPatrono.add(detalle.getAportacionPatronalIsss());
            totalAfpPatrono = totalAfpPatrono.add(detalle.getAportacionPatronalAfp());

            liquidoTotal = liquidoTotal.add(detalle.getSalarioNeto());
        }

        model.addAttribute("totalIsssEmpleado", totalIsssEmpleado);
        model.addAttribute("totalAfpEmpleado", totalAfpEmpleado);
        model.addAttribute("totalRenta", totalRenta);

        model.addAttribute("totalIsssPatrono", totalIsssPatrono);
        model.addAttribute("totalAfpPatrono", totalAfpPatrono);

        model.addAttribute("liquidoTotal", liquidoTotal);

        String nombrePeriodo;
        if (planilla.getTipoPlanilla() == TipoPlanilla.ORDINARIA) {
            nombrePeriodo = "Quincena " + planilla.getQuincena() + " - " + planilla.getPeriodo();
        } else {
            nombrePeriodo = planilla.getTipoPlanilla().name() + " - " + planilla.getPeriodo();
        }
        model.addAttribute("nombrePeriodo", nombrePeriodo);

        return "gestionar-planilla";
    }

    @GetMapping("/generar")
    public String mostrarFormularioGeneracion(HttpSession session, Model model) {
        if (!isAuthenticated(session))
            return "redirect:/login";

        List<Empleado> empleadosActivos = empleadoRepository.findByEstado(EstadoEmpleado.ACTIVO);
        model.addAttribute("empleados", empleadosActivos);
        return "generar-planilla";
    }

    @PostMapping("/generar")
    public String procesarGeneracion(@RequestParam TipoPlanilla tipoPlanilla,
            @RequestParam String periodo,
            @RequestParam(required = false, defaultValue = "1") Integer quincena,
            @RequestParam(required = false) java.util.List<Long> empleadoIds,
            HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(session))
            return "redirect:/login";

        if (empleadoIds == null || empleadoIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Debe seleccionar al menos un empleado.");
            return "redirect:/planillas/generar";
        }

        ResultadoPlanillaDto resultado = null;
        switch (tipoPlanilla) {
            case ORDINARIA:
                resultado = payrollService.generarPlanillaOrdinaria(periodo, quincena, empleadoIds);
                break;
            case AGUINALDO:
                resultado = payrollService.generarPlanillaAguinaldo(periodo, empleadoIds);
                break;
            case VACACIONES:
                resultado = payrollService.generarPlanillaVacaciones(periodo, empleadoIds);
                break;
            case PLANILLA_25:
                resultado = payrollService.generarPlanilla25(periodo, empleadoIds);
                break;
        }

        redirectAttributes.addFlashAttribute("resultadoGeneracion", resultado);
        return "redirect:/planillas/resumen";
    }

    @GetMapping("/resumen")
    public String mostrarResumen(HttpSession session, Model model) {
        if (!isAuthenticated(session))
            return "redirect:/login";

        if (!model.containsAttribute("resultadoGeneracion")) {
            return "redirect:/dashboard";
        }
        return "resumen-generacion";
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdfColillas(@PathVariable Long id, HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Planilla> planillaOpt = planillaRepository.findById(id);
        if (planillaOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        byte[] pdfBytes = pdfGenerationService.generarColillasPdf(planillaOpt.get());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "colillas_planilla_" + id + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
