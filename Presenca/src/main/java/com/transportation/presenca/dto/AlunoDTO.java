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
public class AlunoDTO {
    
    private Long id;
    
    private String matricula;
    
    private String nome;
    
    private String email;
    
    private String telefone;
    
    private String rotaTransporte;
    
    private Long cursoId;
    
    private String nomeCurso;
    
    private String faculdade;
    
    private Boolean ativo;
    
    private LocalDateTime criadoEm;
    
    private LocalDateTime atualizadoEm;
}
