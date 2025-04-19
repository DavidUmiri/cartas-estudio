package com.app.recordatorios.repositories;

import com.app.recordatorios.models.Tarjeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TarjetaRepository extends JpaRepository<Tarjeta, Long> {
    List<Tarjeta> findByUsuarioId(Long usuarioId);
    
    Optional<Tarjeta> findByIdAndUsuarioId(Long id, Long usuarioId);
    
    List<Tarjeta> findByUsuarioIdAndUltimaVezEstudiadaAfter(Long usuarioId, LocalDateTime fecha);
    
    List<Tarjeta> findByUsuarioIdAndNivelDominio(Long usuarioId, int nivelDominio);
    
    @Query("SELECT t FROM Tarjeta t WHERE t.usuario.id = ?1 ORDER BY " +
           "CASE WHEN t.ultimaVezEstudiada IS NULL THEN 0 ELSE 1 END, " +
           "t.ultimaVezEstudiada ASC, t.nivelDominio ASC")
    List<Tarjeta> findTarjetasParaEstudiar(Long usuarioId);
    
    @Query("SELECT COUNT(t) FROM Tarjeta t WHERE t.usuario.id = ?1 AND t.ultimaVezEstudiada >= ?2")
    Long countTarjetasEstudiadasHoy(Long usuarioId, LocalDateTime inicioDia);
    
    @Query("SELECT t FROM Tarjeta t WHERE t.usuario.id = ?1 AND " +
           "(t.ultimaVezEstudiada IS NULL OR " +
           "CASE t.nivelDominio " +
           "  WHEN 0 THEN DATEDIFF(CURRENT_DATE, t.ultimaVezEstudiada) >= 1 " +
           "  WHEN 1 THEN DATEDIFF(CURRENT_DATE, t.ultimaVezEstudiada) >= 2 " +
           "  WHEN 2 THEN DATEDIFF(CURRENT_DATE, t.ultimaVezEstudiada) >= 4 " +
           "  WHEN 3 THEN DATEDIFF(CURRENT_DATE, t.ultimaVezEstudiada) >= 7 " +
           "  WHEN 4 THEN DATEDIFF(CURRENT_DATE, t.ultimaVezEstudiada) >= 14 " +
           "  ELSE DATEDIFF(CURRENT_DATE, t.ultimaVezEstudiada) >= 30 " +
           "END)")
    List<Tarjeta> findTarjetasParaRepasar(Long usuarioId);
    
    @Query("SELECT AVG(t.nivelDominio) FROM Tarjeta t WHERE t.usuario.id = ?1")
    Double findPromedioNivelDominio(Long usuarioId);
    
    @Query("SELECT COUNT(t) FROM Tarjeta t WHERE t.usuario.id = ?1 AND t.nivelDominio >= 4")
    Long countTarjetasDominadas(Long usuarioId);
}
