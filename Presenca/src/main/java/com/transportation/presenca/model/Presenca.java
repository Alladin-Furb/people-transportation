package com.transportation.presenca.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "presencas", indexes = {
        @Index(name = "idx_aluno_data", columnList = "aluno_id,data_presenca"),
        @Index(name = "idx_curso", columnList = "curso_id"),
        @Index(name = "idx_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Presenca {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "aluno_id", nullable = false)
    private Long alunoId;
    
    @Column(name = "aluno_matricula", nullable = false)
    private String alunoMatricula;
    
    @Column(name = "aluno_nome", nullable = false)
    private String alunoNome;
    
    @Column(name = "curso_id", nullable = false)
    private Long cursoId;
    
    @Column(name = "curso_nome", nullable = false)
    private String cursoNome;
    
    @Column(name = "aluno_curso", nullable = false)
    private String alunoCurso;
    
    @Column(name = "faculdade", nullable = false)
    private String faculdade;
    
    @Column(name = "data_presenca", nullable = false)
    private LocalDate dataPresenca;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusPresenca status;
    
    @Column(name = "hora_entrada")
    private LocalDateTime horaEntrada;
    
    @Column(name = "hora_saida")
    private LocalDateTime horaSaida;
    
    @Column(name = "motivo_falta")
    private String motivoFalta;
    
    @Column(name = "justificativa", length = 500)
    private String justificativa;
    
    @Column(name = "observacoes", length = 500)
    private String observacoes;
    
    @Column(name = "horas_aula_comparecidas")
    private Integer horasAulaComparecidas;
    
    @Column(name = "horas_aula_total")
    private Integer horasAulaTotal;
    
    @Column(name = "professor_id")
    private Long professorId;
    
    @Column(name = "professor_nome")
    private String professorNome;
    
    @Column(name = "justificado")
    private Boolean justificado = false;
    
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
