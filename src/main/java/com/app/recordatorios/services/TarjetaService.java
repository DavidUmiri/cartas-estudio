package com.app.recordatorios.services;

import com.app.recordatorios.models.Tarjeta;
import com.app.recordatorios.repositories.TarjetaRepository;
import com.app.recordatorios.exceptions.TarjetaNoEncontradaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TarjetaService {

    private static final Logger logger = LoggerFactory.getLogger(TarjetaService.class);

    @Autowired
    private TarjetaRepository tarjetaRepository;

    // Obtener todas las tarjetas
    public List<Tarjeta> obtenerTodas() {
        logger.info("Obteniendo todas las tarjetas de estudio");
        return tarjetaRepository.findAll();
    }

    // Obtener una tarjeta por ID
    public Tarjeta obtenerPorId(Long id) {
        return tarjetaRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Tarjeta con ID {} no encontrada", id);
                    return new TarjetaNoEncontradaException("Tarjeta con ID " + id + " no encontrada");
                });
    }

    // Guardar una nueva tarjeta o actualizar una existente
    @Transactional
    public Tarjeta guardarTarjeta(Tarjeta tarjeta) {
        boolean esNueva = tarjeta.getId() == null;
        Tarjeta guardada = tarjetaRepository.save(tarjeta);

        if (esNueva) {
            logger.info("Nueva tarjeta creada con ID {}", guardada.getId());
        } else {
            logger.info("Tarjeta con ID {} actualizada", guardada.getId());
        }

        return guardada;
    }

    // Eliminar una tarjeta por ID (con verificación previa)
    @Transactional
    public void eliminarTarjeta(Long id) {
        if (!tarjetaRepository.existsById(id)) {
            logger.error("Intento de eliminar tarjeta con ID {} que no existe", id);
            throw new TarjetaNoEncontradaException("No se puede eliminar: Tarjeta con ID " + id + " no encontrada");
        }
        tarjetaRepository.deleteById(id);
        logger.info("Tarjeta con ID {} eliminada correctamente", id);
    }
}
