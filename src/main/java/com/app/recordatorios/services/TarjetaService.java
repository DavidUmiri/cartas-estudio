package com.app.recordatorios.services;

import com.app.recordatorios.models.Tarjeta;
import com.app.recordatorios.models.EstadisticasEstudio;
import com.app.recordatorios.repositories.TarjetaRepository;
import com.app.recordatorios.exceptions.TarjetaNoEncontradaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TarjetaService {

    private static final Logger logger = LoggerFactory.getLogger(TarjetaService.class);

    @Autowired
    private TarjetaRepository tarjetaRepository;

    public List<Tarjeta> obtenerTodas() {
        logger.info("Obteniendo todas las tarjetas de estudio");
        return tarjetaRepository.findAll();
    }

    public Tarjeta obtenerPorId(Long id) {
        return tarjetaRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Tarjeta con ID {} no encontrada", id);
                    return new TarjetaNoEncontradaException("Tarjeta con ID " + id + " no encontrada");
                });
    }

    public List<Tarjeta> obtenerTarjetasDeUsuario(Long usuarioId) {
        logger.info("Obteniendo tarjetas para el usuario ID {}", usuarioId);
        return tarjetaRepository.findByUsuarioId(usuarioId);
    }

    public Tarjeta obtenerTarjetaDeUsuario(Long tarjetaId, Long usuarioId) {
        return tarjetaRepository.findByIdAndUsuarioId(tarjetaId, usuarioId)
                .orElseThrow(() -> {
                    logger.error("Tarjeta con ID {} no encontrada para el usuario {}", tarjetaId, usuarioId);
                    return new TarjetaNoEncontradaException("Tarjeta no encontrada o no pertenece al usuario");
                });
    }

    @Transactional
    public Tarjeta guardarTarjeta(Tarjeta tarjeta) {
        boolean esNueva = tarjeta.getId() == null;
        
        // Validaciones adicionales
        if (tarjeta.getPregunta() == null || tarjeta.getPregunta().trim().isEmpty()) {
            throw new IllegalArgumentException("La pregunta no puede estar vacía");
        }
        if (tarjeta.getRespuesta() == null || tarjeta.getRespuesta().trim().isEmpty()) {
            throw new IllegalArgumentException("La respuesta no puede estar vacía");
        }

        // Limpiar espacios en blanco
        tarjeta.setPregunta(tarjeta.getPregunta().trim());
        tarjeta.setRespuesta(tarjeta.getRespuesta().trim());

        Tarjeta guardada = tarjetaRepository.save(tarjeta);

        if (esNueva) {
            logger.info("Nueva tarjeta creada con ID {} para el usuario {}", 
                guardada.getId(), guardada.getUsuario().getId());
        } else {
            logger.info("Tarjeta con ID {} actualizada para el usuario {}", 
                guardada.getId(), guardada.getUsuario().getId());
        }

        return guardada;
    }

    @Transactional
    public void eliminarTarjeta(Long id) {
        if (!tarjetaRepository.existsById(id)) {
            logger.error("Intento de eliminar tarjeta con ID {} que no existe", id);
            throw new TarjetaNoEncontradaException("No se puede eliminar: Tarjeta con ID " + id + " no encontrada");
        }
        tarjetaRepository.deleteById(id);
        logger.info("Tarjeta con ID {} eliminada correctamente", id);
    }

    @Transactional
    public void eliminarTarjetaDeUsuario(Long tarjetaId, Long usuarioId) {
        Tarjeta tarjeta = obtenerTarjetaDeUsuario(tarjetaId, usuarioId);
        tarjetaRepository.delete(tarjeta);
        logger.info("Tarjeta con ID {} eliminada para el usuario {}", tarjetaId, usuarioId);
    }

    @Transactional
    public void registrarEstudio(Long tarjetaId, Long usuarioId, int nivelDominio) {
        if (nivelDominio < 0 || nivelDominio > 5) {
            logger.error("Nivel de dominio inválido: {}. Debe estar entre 0 y 5", nivelDominio);
            throw new IllegalArgumentException("El nivel de dominio debe estar entre 0 y 5");
        }

        Tarjeta tarjeta = obtenerTarjetaDeUsuario(tarjetaId, usuarioId);
        tarjeta.incrementarEstudio();
        tarjeta.setUltimaVezEstudiada(LocalDateTime.now());
        tarjeta.actualizarNivelDominio(nivelDominio);
        
        try {
            tarjetaRepository.save(tarjeta);
            logger.info("Registrado estudio de tarjeta ID {} para usuario {}. Nivel dominio: {}, Veces estudiada: {}", 
                tarjetaId, usuarioId, nivelDominio, tarjeta.getVecesEstudiada());
        } catch (Exception e) {
            logger.error("Error al registrar estudio de tarjeta ID {} para usuario {}: {}", 
                tarjetaId, usuarioId, e.getMessage());
            throw new RuntimeException("Error al registrar el estudio de la tarjeta", e);
        }
    }

    public List<Tarjeta> obtenerTarjetasEstudiadasHoy(Long usuarioId) {
        LocalDateTime inicioDia = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        return tarjetaRepository.findByUsuarioIdAndUltimaVezEstudiadaAfter(usuarioId, inicioDia);
    }

    public List<Tarjeta> obtenerTarjetasPorNivelDominio(Long usuarioId, int nivelDominio) {
        return tarjetaRepository.findByUsuarioIdAndNivelDominio(usuarioId, nivelDominio);
    }

    public List<Tarjeta> obtenerTarjetasParaEstudiar(Long usuarioId) {
        List<Tarjeta> tarjetas = tarjetaRepository.findTarjetasParaEstudiar(usuarioId);
        logger.info("Obtenidas {} tarjetas para estudiar del usuario {}", tarjetas.size(), usuarioId);
        return tarjetas;
    }

    public List<Tarjeta> obtenerTarjetasParaRepasar(Long usuarioId) {
        List<Tarjeta> tarjetas = tarjetaRepository.findTarjetasParaRepasar(usuarioId);
        logger.info("Obtenidas {} tarjetas para repasar del usuario {}", tarjetas.size(), usuarioId);
        return tarjetas;
    }

    public EstadisticasEstudio obtenerEstadisticas(Long usuarioId) {
        LocalDateTime inicioDia = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        Long estudiadasHoy = tarjetaRepository.countTarjetasEstudiadasHoy(usuarioId, inicioDia);
        Double promedioNivel = tarjetaRepository.findPromedioNivelDominio(usuarioId);
        Long tarjetasDominadas = tarjetaRepository.countTarjetasDominadas(usuarioId);
        List<Tarjeta> todasTarjetas = tarjetaRepository.findByUsuarioId(usuarioId);
        
        return EstadisticasEstudio.builder()
            .totalTarjetas((long) todasTarjetas.size())
            .tarjetasEstudiadasHoy(estudiadasHoy)
            .promedioNivelDominio(promedioNivel != null ? promedioNivel : 0.0)
            .tarjetasDominadas(tarjetasDominadas)
            .porcentajeDominado(calcularPorcentajeDominado(tarjetasDominadas, todasTarjetas.size()))
            .build();
    }

    private double calcularPorcentajeDominado(Long dominadas, int total) {
        if (total == 0) return 0.0;
        return Math.round((double) dominadas / total * 100);
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * *") // Ejecutar a medianoche todos los días
    public void resetearTarjetasEstudiadasHoy() {
        logger.info("Ejecutando reseteo diario de tarjetas estudiadas");
        // No necesitamos hacer nada aquí ya que el contador se basa en la fecha
        // y se resetea automáticamente al cambiar el día
    }
}
