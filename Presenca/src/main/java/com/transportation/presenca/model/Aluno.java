package com.transportation.presenca.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "alunos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Aluno {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "matricula", nullable = false, unique = true, length = 50)
    private String matricula;
    
    @Column(name = "nome", nullable = false, length = 255)
    private String nome;
    
    @Column(name = "email", unique = true, length = 100)
    private String email;
    
    @Column(name = "telefone", length = 20)
    private String telefone;
    
    @Column(name = "rota_transporte", length = 100)
    private String rotaTransporte;
    
    @Column(name = "curso_id")
    private Long cursoId;
    
    @Column(name = "nome_curso", length = 255)
    private String nomeCurso;
    
    @Column(name = "faculdade", length = 255)
    private String faculdade;
    
    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;
    
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
    
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
    
    @PrePersist
    protected void onCreate() {
        criadoEm = LocalDateTime.now();
        atualizadoEm = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        atualizadoEm = LocalDateTime.now();
    }
}
