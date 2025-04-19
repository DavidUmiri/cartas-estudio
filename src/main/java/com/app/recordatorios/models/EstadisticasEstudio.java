package com.app.recordatorios.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EstadisticasEstudio {
    private Long totalTarjetas;
    private Long tarjetasEstudiadasHoy;
    private Double promedioNivelDominio;
    private Long tarjetasDominadas;
    private Double porcentajeDominado;
}