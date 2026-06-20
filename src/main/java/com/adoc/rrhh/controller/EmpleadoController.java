package com.adoc.rrhh.controller;

import com.adoc.rrhh.entity.Empleado;
import com.adoc.rrhh.entity.Usuario;
import com.adoc.rrhh.entity.enums.EstadoEmpleado;
import com.adoc.rrhh.entity.enums.TipoJornada;
import com.adoc.rrhh.repository.EmpleadoRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/empleados")
public class EmpleadoController {

    private final EmpleadoRepository empleadoRepository;

    public EmpleadoController(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;
    }

    private boolean isAuthenticated(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        return usuario != null;
    }

    @GetMapping
    public String listarEmpleados(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size,
            HttpSession session, Model model) {
        if (!isAuthenticated(session)) {
            return "redirect:/login";
        }
        
        org.springframework.data.domain.Page<Empleado> empleadosPage = empleadoRepository.findAll(
            org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("id").descending())
        );
        model.addAttribute("empleados", empleadosPage);
        return "empleados";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(HttpSession session, Model model) {
        if (!isAuthenticated(session)) {
            return "redirect:/login";
        }
        
        model.addAttribute("empleado", new Empleado());
        model.addAttribute("tiposJornada", TipoJornada.values());
        model.addAttribute("estados", EstadoEmpleado.values());
        
        return "nuevo-empleado";
    }

    @PostMapping("/nuevo")
    public String guardarEmpleado(@ModelAttribute("empleado") Empleado empleado, HttpSession session) {
        if (!isAuthenticated(session)) {
            return "redirect:/login";
        }
        
        empleadoRepository.save(empleado);
        return "redirect:/empleados";
    }

    @GetMapping("/{id}/editar")
    public String mostrarFormularioEditar(@org.springframework.web.bind.annotation.PathVariable Long id, HttpSession session, Model model) {
        if (!isAuthenticated(session)) {
            return "redirect:/login";
        }
        
        Empleado empleado = empleadoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("ID de empleado inválido:" + id));
        model.addAttribute("empleado", empleado);
        model.addAttribute("tiposJornada", TipoJornada.values());
        model.addAttribute("estados", EstadoEmpleado.values());
        
        return "nuevo-empleado";
    }

    @PostMapping("/{id}/editar")
    public String actualizarEmpleado(@org.springframework.web.bind.annotation.PathVariable Long id, @ModelAttribute("empleado") Empleado empleado, HttpSession session) {
        if (!isAuthenticated(session)) {
            return "redirect:/login";
        }
        
        empleado.setId(id);
        empleadoRepository.save(empleado);
        return "redirect:/empleados";
    }
}
