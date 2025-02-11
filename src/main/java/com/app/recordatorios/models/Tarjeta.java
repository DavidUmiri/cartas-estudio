package com.app.recordatorios.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tarjetas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tarjeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String pregunta;

    @Column(nullable = false)
    private String respuesta;
}
