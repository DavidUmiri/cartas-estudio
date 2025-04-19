package com.app.recordatorios.controllers;

import com.app.recordatorios.models.Tarjeta;
import com.app.recordatorios.models.Usuario;
import com.app.recordatorios.models.EstadisticasEstudio;
import com.app.recordatorios.services.TarjetaService;
import com.app.recordatorios.repositories.UsuarioRepository;
import com.app.recordatorios.exceptions.TarjetaNoEncontradaException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/tarjetas")
public class TarjetaController {

    private static final Logger logger = LoggerFactory.getLogger(TarjetaController.class);
    private static final String REDIRECT_TARJETAS = "redirect:/tarjetas";
    private static final String INDICE_ESTUDIO = "indiceEstudio";

    @Autowired
    private TarjetaService tarjetaService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario obtenerUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> {
                    logger.error("Usuario no encontrado: {}", auth.getName());
                    return new RuntimeException("Usuario no encontrado");
                });
    }

    @GetMapping
    public String tarjetasEstudio(Model model) {
        Usuario usuario = obtenerUsuarioActual();
        List<Tarjeta> tarjetas = tarjetaService.obtenerTarjetasDeUsuario(usuario.getId());
        List<Tarjeta> tarjetasParaRepasar = tarjetaService.obtenerTarjetasParaRepasar(usuario.getId());
        EstadisticasEstudio estadisticas = tarjetaService.obtenerEstadisticas(usuario.getId());
        
        Tarjeta nuevaTarjeta = new Tarjeta();
        nuevaTarjeta.setUsuario(usuario);
        
        model.addAttribute("tarjetas", tarjetas);
        model.addAttribute("tarjeta", nuevaTarjeta);
        model.addAttribute("estadisticas", estadisticas);
        model.addAttribute("tarjetasParaRepasar", tarjetasParaRepasar.size());
        
        logger.info("Usuario {} accedió a sus tarjetas. Total: {}, Para repasar: {}, Estudiadas hoy: {}", 
            usuario.getUsername(), tarjetas.size(), tarjetasParaRepasar.size(), estadisticas.getTarjetasEstudiadasHoy());
        
        return "tarjetas";
    }

    @PostMapping("/guardar")
    public String guardarTarjeta(
            @Valid @ModelAttribute Tarjeta tarjeta,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            logger.warn("Error de validación al guardar tarjeta: {}", result.getAllErrors());
            redirectAttributes.addFlashAttribute("error", "Por favor verifica los campos ingresados");
            return REDIRECT_TARJETAS;
        }

        try {
            Usuario usuario = obtenerUsuarioActual();
            boolean esNueva = tarjeta.getId() == null;
            
            if (!esNueva) {
                Tarjeta existente = tarjetaService.obtenerTarjetaDeUsuario(tarjeta.getId(), usuario.getId());
                tarjeta.setUsuario(existente.getUsuario());
                tarjeta.setVecesEstudiada(existente.getVecesEstudiada());
                tarjeta.setNivelDominio(existente.getNivelDominio());
                tarjeta.setUltimaVezEstudiada(existente.getUltimaVezEstudiada());
            } else {
                tarjeta.setUsuario(usuario);
            }
            
            Tarjeta guardada = tarjetaService.guardarTarjeta(tarjeta);
            String mensaje = esNueva ? "Tarjeta creada exitosamente" : "Tarjeta actualizada exitosamente";
            redirectAttributes.addFlashAttribute("success", mensaje);
            
            logger.info("Usuario {} {} la tarjeta con ID {}", 
                usuario.getUsername(),
                esNueva ? "creó" : "actualizó",
                guardada.getId());
                
        } catch (TarjetaNoEncontradaException e) {
            logger.error("Error al guardar tarjeta: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "No se encontró la tarjeta a actualizar");
            return "tarjetaNoEncontrada";
        } catch (Exception e) {
            logger.error("Error al guardar tarjeta: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al guardar la tarjeta: " + e.getMessage());
        }
        
        return REDIRECT_TARJETAS;
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarTarjeta(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        
        Usuario usuario = obtenerUsuarioActual();
        try {
            tarjetaService.eliminarTarjetaDeUsuario(id, usuario.getId());
            redirectAttributes.addFlashAttribute("success", "Tarjeta eliminada exitosamente");
            logger.info("Usuario {} eliminó la tarjeta con ID {}", usuario.getUsername(), id);
        } catch (TarjetaNoEncontradaException e) {
            logger.error("Intento fallido de eliminar tarjeta con ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "No se encontró la tarjeta a eliminar");
            return "tarjetaNoEncontrada";
        } catch (Exception e) {
            logger.error("Error al eliminar tarjeta: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al eliminar la tarjeta: " + e.getMessage());
        }
        
        return REDIRECT_TARJETAS;
    }

    @GetMapping("/editar/{id}")
    public String editarTarjeta(@PathVariable Long id, Model model) {
        Usuario usuario = obtenerUsuarioActual();
        try {
            Tarjeta tarjeta = tarjetaService.obtenerTarjetaDeUsuario(id, usuario.getId());
            model.addAttribute("tarjeta", tarjeta);
            model.addAttribute("edicion", true);
            
            logger.info("Usuario {} está editando la tarjeta con ID {}", 
                usuario.getUsername(), id);
                
            return "editarTarjeta";
        } catch (TarjetaNoEncontradaException e) {
            logger.error("Intento fallido de editar tarjeta con ID {}: {}", id, e.getMessage());
            return "tarjetaNoEncontrada";
        }
    }

    @GetMapping("/estudiar")
    public String estudiar(
            @RequestParam(required = false) String siguiente,
            @RequestParam(required = false) String anterior,
            @RequestParam(required = false) Integer indice,
            @RequestParam(required = false) Integer nivelDominio,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        Usuario usuario = obtenerUsuarioActual();
        List<Tarjeta> tarjetasParaEstudiar = tarjetaService.obtenerTarjetasParaEstudiar(usuario.getId());
        
        if (tarjetasParaEstudiar.isEmpty()) {
            redirectAttributes.addFlashAttribute("warning", "¡No tienes tarjetas pendientes para estudiar!");
            return REDIRECT_TARJETAS;
        }

        Integer indiceActual = Optional.ofNullable((Integer) session.getAttribute(INDICE_ESTUDIO))
                .orElse(0);

        try {
            // Si se está calificando una tarjeta
            if (nivelDominio != null && indiceActual < tarjetasParaEstudiar.size()) {
                tarjetaService.registrarEstudio(tarjetasParaEstudiar.get(indiceActual).getId(), 
                    usuario.getId(), nivelDominio);
            }

            // Manejar navegación
            if (indice != null && indice >= 0 && indice < tarjetasParaEstudiar.size()) {
                indiceActual = indice;
            } else if (siguiente != null && indiceActual < tarjetasParaEstudiar.size() - 1) {
                indiceActual++;
            } else if (anterior != null && indiceActual > 0) {
                indiceActual--;
            }

            session.setAttribute(INDICE_ESTUDIO, indiceActual);
            
            Tarjeta tarjetaActual = tarjetasParaEstudiar.get(indiceActual);
            EstadisticasEstudio estadisticas = tarjetaService.obtenerEstadisticas(usuario.getId());
            
            model.addAttribute("tarjetaActual", tarjetaActual);
            model.addAttribute("esPrimera", indiceActual == 0);
            model.addAttribute("esUltima", indiceActual == tarjetasParaEstudiar.size() - 1);
            model.addAttribute("posicionActual", indiceActual);
            model.addAttribute("totalTarjetas", tarjetasParaEstudiar.size());
            model.addAttribute("tarjetas", tarjetasParaEstudiar);
            model.addAttribute("estadisticas", estadisticas);
            model.addAttribute("nivelDominio", tarjetaActual.getNivelDominio());
            model.addAttribute("porcentajeProgreso", calcularPorcentajeProgreso(indiceActual + 1, tarjetasParaEstudiar.size()));

            logger.info("Usuario {} está estudiando la tarjeta {} de {}. Nivel: {}", 
                usuario.getUsername(), indiceActual + 1, tarjetasParaEstudiar.size(), 
                tarjetaActual.getNivelDominio());

        } catch (Exception e) {
            logger.error("Error durante la sesión de estudio: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ocurrió un error durante la sesión de estudio");
            return REDIRECT_TARJETAS;
        }

        return "estudiar";
    }

    private double calcularPorcentajeProgreso(int actual, int total) {
        if (total == 0) return 0;
        return Math.round((double) actual / total * 100);
    }
}
