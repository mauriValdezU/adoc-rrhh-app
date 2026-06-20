package com.adoc.rrhh.controller;

import com.adoc.rrhh.entity.Ausencia;
import com.adoc.rrhh.entity.Empleado;
import com.adoc.rrhh.entity.Usuario;
import com.adoc.rrhh.entity.enums.EstadoAusencia;
import com.adoc.rrhh.entity.enums.EstadoEmpleado;
import com.adoc.rrhh.entity.enums.TipoAusencia;
import com.adoc.rrhh.repository.EmpleadoRepository;
import com.adoc.rrhh.service.AusenciaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/ausencias")
public class AusenciaController {

    private final AusenciaService ausenciaService;
    private final EmpleadoRepository empleadoRepository;

    public AusenciaController(AusenciaService ausenciaService,
                              EmpleadoRepository empleadoRepository) {
        this.ausenciaService = ausenciaService;
        this.empleadoRepository = empleadoRepository;
    }

    private boolean isAuthenticated(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        return usuario != null;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  LISTADO
    // ═══════════════════════════════════════════════════════════════════

    @GetMapping
    public String listarAusencias(
            @RequestParam(required = false) EstadoAusencia estado,
            @RequestParam(required = false) TipoAusencia tipo,
            HttpSession session, Model model) {
        if (!isAuthenticated(session)) return "redirect:/login";

        List<Ausencia> ausencias = ausenciaService.listarPorFiltros(estado, tipo);

        model.addAttribute("ausencias", ausencias);
        model.addAttribute("estados", EstadoAusencia.values());
        model.addAttribute("tipos", TipoAusencia.values());
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("tipoSeleccionado", tipo);

        return "ausencias";
    }

    // ═══════════════════════════════════════════════════════════════════
    //  FORMULARIO DE REGISTRO
    // ═══════════════════════════════════════════════════════════════════

    @GetMapping("/nueva")
    public String mostrarFormulario(HttpSession session, Model model) {
        if (!isAuthenticated(session)) return "redirect:/login";

        List<Empleado> empleados = empleadoRepository.findByEstado(EstadoEmpleado.ACTIVO);
        model.addAttribute("empleados", empleados);
        model.addAttribute("tipos", TipoAusencia.values());
        model.addAttribute("ausencia", new Ausencia());

        return "nueva-ausencia";
    }

    @PostMapping("/nueva")
    public String registrarAusencia(
            @RequestParam Long empleadoId,
            @ModelAttribute Ausencia ausencia,
            HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(session)) return "redirect:/login";

        // Vincular empleado
        Empleado empleado = empleadoRepository.findById(empleadoId).orElse(null);
        if (empleado == null) {
            redirectAttributes.addFlashAttribute("error", "Empleado no encontrado.");
            return "redirect:/ausencias/nueva";
        }
        ausencia.setEmpleado(empleado);

        // Registrar con cálculos automáticos
        ausenciaService.registrarAusencia(ausencia);

        // Verificar faltas injustificadas en el mes (Art. 50 C.T.)
        if (ausencia.getTipoAusencia() == TipoAusencia.AUSENCIA_INJUSTIFICADA) {
            LocalDate fecha = ausencia.getFechaInicio();
            long faltas = ausenciaService.contarFaltasInjustificadasMes(
                    empleadoId, fecha.getMonthValue(), fecha.getYear());
            if (faltas > 2) {
                redirectAttributes.addFlashAttribute("alertaNormativa",
                        "⚠ ALERTA: El empleado " + empleado.getNombreCompleto()
                        + " acumula " + faltas + " faltas injustificadas en el mes. "
                        + "Esto constituye causal de despido justificado según Art. 50 numeral 13 del Código de Trabajo.");
            }
        }

        redirectAttributes.addFlashAttribute("exito", "Ausencia registrada correctamente.");
        return "redirect:/ausencias";
    }

    // ═══════════════════════════════════════════════════════════════════
    //  ACCIONES DE FLUJO
    // ═══════════════════════════════════════════════════════════════════

    @PostMapping("/{id}/aprobar")
    public String aprobar(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(session)) return "redirect:/login";
        ausenciaService.aprobarAusencia(id);
        redirectAttributes.addFlashAttribute("exito", "Ausencia aprobada. El estado del empleado ha sido actualizado.");
        return "redirect:/ausencias";
    }

    @PostMapping("/{id}/rechazar")
    public String rechazar(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(session)) return "redirect:/login";
        ausenciaService.rechazarAusencia(id);
        redirectAttributes.addFlashAttribute("exito", "Ausencia rechazada.");
        return "redirect:/ausencias";
    }

    @PostMapping("/{id}/finalizar")
    public String finalizar(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(session)) return "redirect:/login";
        ausenciaService.finalizarAusencia(id);
        redirectAttributes.addFlashAttribute("exito", "Ausencia finalizada. El empleado ha sido restaurado a estado Activo.");
        return "redirect:/ausencias";
    }
}
