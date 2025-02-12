package com.app.recordatorios.controllers;

import com.app.recordatorios.models.Tarjeta;
import com.app.recordatorios.services.TarjetaService;
import com.app.recordatorios.exceptions.TarjetaNoEncontradaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/tarjetas")
public class TarjetaController {

    private static final Logger logger = LoggerFactory.getLogger(TarjetaController.class);

    @Autowired
    private TarjetaService tarjetaService;

    // Mostrar todas las tarjetas y permitir la creación
    @GetMapping
    public String tarjetasEstudio(Model model) {
        model.addAttribute("tarjetas", tarjetaService.obtenerTodas());
        model.addAttribute("tarjeta", new Tarjeta()); // Objeto vacío para crear una nueva tarjeta
        logger.info("Usuario accedió a la lista de tarjetas de estudio");
        return "tarjetas"; // Vista que muestra tanto el formulario como la lista
    }

    // Guardar una tarjeta (nueva o editada)
    @PostMapping("/guardar")
    public String guardarTarjeta(@ModelAttribute Tarjeta tarjeta, Model model) {
        boolean esNueva = tarjeta.getId() == null;
        Tarjeta guardada = tarjetaService.guardarTarjeta(tarjeta);

        if (esNueva) {
            logger.info("Usuario creó una nueva tarjeta con ID {}", guardada.getId());
        } else {
            logger.info("Usuario actualizó la tarjeta con ID {}", guardada.getId());
        }

        // Vuelve a cargar todas las tarjetas y el formulario vacío
        model.addAttribute("tarjetas", tarjetaService.obtenerTodas());
        model.addAttribute("tarjeta", new Tarjeta()); // Reinicia el formulario vacío
        return "tarjetas"; // Recarga la vista con las tarjetas y el formulario vacío
    }

    // Eliminar una tarjeta
    @GetMapping("/eliminar/{id}")
    public String eliminarTarjeta(@PathVariable Long id) {
        try {
            tarjetaService.eliminarTarjeta(id);
            logger.info("Usuario eliminó la tarjeta con ID {}", id);
        } catch (TarjetaNoEncontradaException e) {
            logger.error("Intento fallido de eliminar tarjeta con ID {}: {}", id, e.getMessage());
            return "tarjetaNoEncontrada"; // Vista de error
        }
        return "redirect:/tarjetas";
    }

    // Editar una tarjeta
    @GetMapping("/editar/{id}")
    public String editarTarjeta(@PathVariable Long id, Model model) {
        try {
            Tarjeta tarjeta = tarjetaService.obtenerPorId(id);
            model.addAttribute("tarjeta", tarjeta);
            logger.info("Usuario editó la tarjeta con ID {}", id);
            return "editarTarjeta"; // Vista para editar la tarjeta
        } catch (TarjetaNoEncontradaException e) {
            logger.error("Intento fallido de editar tarjeta con ID {}: {}", id, e.getMessage());
            return "tarjetaNoEncontrada"; // Vista de error
        }
    }

    @GetMapping("/estudiar")
    public String estudiar(
            @RequestParam(required = false) String siguiente,
            @RequestParam(required = false) String anterior,
            Model model,
            HttpSession session) {
        
        List<Tarjeta> tarjetas = tarjetaService.obtenerTodas();
        
        if (tarjetas.isEmpty()) {
            return "estudiar";
        }

        // Obtener o inicializar el índice de la tarjeta actual
        Integer indiceActual = (Integer) session.getAttribute("indiceEstudio");
        if (indiceActual == null) {
            indiceActual = 0;
        } else {
            if (siguiente != null && indiceActual < tarjetas.size() - 1) {
                indiceActual++;
            } else if (anterior != null && indiceActual > 0) {
                indiceActual--;
            }
        }

        // Guardar el índice actual en la sesión
        session.setAttribute("indiceEstudio", indiceActual);

        // Preparar el modelo
        model.addAttribute("tarjetaActual", tarjetas.get(indiceActual));
        model.addAttribute("esPrimera", indiceActual == 0);
        model.addAttribute("esUltima", indiceActual == tarjetas.size() - 1);
        model.addAttribute("posicionActual", indiceActual);
        model.addAttribute("totalTarjetas", tarjetas.size());
        model.addAttribute("tarjetas", tarjetas);

        return "estudiar";
    }
}
