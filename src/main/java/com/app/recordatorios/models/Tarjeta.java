package com.app.recordatorios.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "tarjetas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tarjeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La pregunta no puede estar vacía")
    @Size(min = 3, max = 500, message = "La pregunta debe tener entre 3 y 500 caracteres")
    @Column(nullable = false, length = 500)
    private String pregunta;

    @NotBlank(message = "La respuesta no puede estar vacía")
    @Size(min = 3, max = 1000, message = "La respuesta debe tener entre 3 y 1000 caracteres")
    @Column(nullable = false, length = 1000)
    private String respuesta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "veces_estudiada")
    @Builder.Default
    private int vecesEstudiada = 0;

    @Column(name = "ultima_vez_estudiada")
    private LocalDateTime ultimaVezEstudiada;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "nivel_dominio")
    @Builder.Default
    private int nivelDominio = 0; // 0-5, donde 5 es el máximo dominio

    public void incrementarEstudio() {
        this.vecesEstudiada++;
        this.ultimaVezEstudiada = LocalDateTime.now();
    }

    public void actualizarNivelDominio(int nuevoNivel) {
        if (nuevoNivel < 0 || nuevoNivel > 5) {
            throw new IllegalArgumentException("El nivel de dominio debe estar entre 0 y 5");
        }
        this.nivelDominio = nuevoNivel;
    }

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
        if (vecesEstudiada < 0) vecesEstudiada = 0;
        if (nivelDominio < 0) nivelDominio = 0;
        if (nivelDominio > 5) nivelDominio = 5;
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
        if (vecesEstudiada < 0) vecesEstudiada = 0;
        if (nivelDominio < 0) nivelDominio = 0;
        if (nivelDominio > 5) nivelDominio = 5;
    }

    public double calcularPorcentajeDominio() {
        return (double) nivelDominio * 20; // Convierte nivel 0-5 a porcentaje 0-100
    }

    public boolean necesitaRepaso() {
        if (ultimaVezEstudiada == null) return true;
        
        // Calcula días desde último estudio basado en nivel de dominio
        long diasParaRepaso = switch(nivelDominio) {
            case 0 -> 1;  // Repasar al día siguiente
            case 1 -> 2;  // Repasar en 2 días
            case 2 -> 4;  // Repasar en 4 días
            case 3 -> 7;  // Repasar en 1 semana
            case 4 -> 14; // Repasar en 2 semanas
            case 5 -> 30; // Repasar en 1 mes
            default -> 1;
        };

        return ChronoUnit.DAYS.between(ultimaVezEstudiada, LocalDateTime.now()) >= diasParaRepaso;
    }
}
