package com.transportation.presenca.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelatorioPresencaDTO {
    
    private Long alunoId;
    
    private String alunoMatricula;
    
    private String alunoNome;
    
    private Long cursoId;
    
    private String cursoNome;
    
    private Integer totalAulas;
    
    private Integer presentes;
    
    private Integer ausentes;
    
    private Integer atrasados;
    
    private Integer faltasJustificadas;
    
    private Integer faltasNaoJustificadas;
    
    private Integer saidasAntecipadas;
    
    private Double frequencia;
    
    private String statusFrequencia; // "OK", "AVISO", "CRÍTICO"
}
