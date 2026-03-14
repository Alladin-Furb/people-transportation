package com.transportation.presenca.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CursoDTO {
    
    private Long id;
    
    private String codigo;
    
    private String nome;
    
    private String descricao;
    
    private String faculdade;
    
    private String campus;
    
    private Integer cargaHoraria;
    
    private String periodo;
    
    private Boolean ativa;
    
    private LocalDateTime criadoEm;
    
    private LocalDateTime atualizadoEm;
}
