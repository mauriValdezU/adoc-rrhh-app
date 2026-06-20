package com.adoc.rrhh.controller;

import com.adoc.rrhh.entity.DetallePlanilla;
import com.adoc.rrhh.entity.Planilla;
import com.adoc.rrhh.entity.Usuario;
import com.adoc.rrhh.repository.EmpleadoRepository;
import com.adoc.rrhh.repository.PlanillaRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Controller
public class HomeController {

    private final EmpleadoRepository empleadoRepository;
    private final PlanillaRepository planillaRepository;

    public HomeController(EmpleadoRepository empleadoRepository,
                          PlanillaRepository planillaRepository) {
        this.empleadoRepository = empleadoRepository;
        this.planillaRepository = planillaRepository;
    }

    @GetMapping("/")
    public String dashboard(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        // ─── Periodo actual ─────────────────────────────────────────
        String periodoActual = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        model.addAttribute("periodoActual", periodoActual);

        // ─── Métricas ───────────────────────────────────────────────
        long totalEmpleados = empleadoRepository.count();
        model.addAttribute("totalEmpleados", totalEmpleados);

        // Planillas del periodo actual
        List<Planilla> planillasPeriodo = planillaRepository.findByPeriodo(periodoActual);

        BigDecimal totalPlanilla = BigDecimal.ZERO;
        BigDecimal totalDeducciones = BigDecimal.ZERO;

        for (Planilla planilla : planillasPeriodo) {
            for (DetallePlanilla detalle : planilla.getDetalles()) {
                totalPlanilla = totalPlanilla.add(
                        detalle.getTotalDevengado() != null ? detalle.getTotalDevengado() : BigDecimal.ZERO);
                totalDeducciones = totalDeducciones.add(
                        detalle.getTotalDeducciones() != null ? detalle.getTotalDeducciones() : BigDecimal.ZERO);
            }
        }

        // Formatear montos como moneda USD
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        model.addAttribute("totalPlanilla", currencyFormat.format(totalPlanilla));
        model.addAttribute("totalDeducciones", currencyFormat.format(totalDeducciones));

        // Total de planillas procesadas (histórico)
        long planillasProcesadas = planillaRepository.count();
        model.addAttribute("planillasProcesadas", planillasProcesadas);

        return "dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboardAlias(HttpSession session, Model model) {
        return dashboard(session, model);
    }
}
