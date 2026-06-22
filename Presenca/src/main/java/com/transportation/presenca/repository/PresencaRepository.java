package com.transportation.presenca.repository;

import com.transportation.presenca.model.Presenca;
import com.transportation.presenca.model.StatusPresenca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PresencaRepository extends JpaRepository<Presenca, Long> {
    
    // Buscar presença de um aluno em uma data específica
    Optional<Presenca> findByAlunoIdAndDataPresenca(Long alunoId, LocalDate dataPresenca);
    
    // Buscar presença de um aluno em um curso e data
    Optional<Presenca> findByAlunoIdAndCursoIdAndDataPresenca(Long alunoId, Long cursoId, LocalDate dataPresenca);
    
    // Listar presenças de um aluno em um período
    @Query("SELECT p FROM Presenca p WHERE p.alunoId = :alunoId AND p.dataPresenca BETWEEN :dataInicio AND :dataFim ORDER BY p.dataPresenca DESC")
    List<Presenca> findByAlunoIdAndPeriodo(@Param("alunoId") Long alunoId, 
                                           @Param("dataInicio") LocalDate dataInicio,
                                           @Param("dataFim") LocalDate dataFim);
    
    // Listar presenças de um curso em um período
    @Query("SELECT p FROM Presenca p WHERE p.cursoId = :cursoId AND p.dataPresenca BETWEEN :dataInicio AND :dataFim ORDER BY p.dataPresenca DESC")
    List<Presenca> findByCursoIdAndPeriodo(@Param("cursoId") Long cursoId,
                                           @Param("dataInicio") LocalDate dataInicio,
                                           @Param("dataFim") LocalDate dataFim);
    
    // Contar faltas não justificadas de um aluno
    @Query("SELECT COUNT(p) FROM Presenca p WHERE p.alunoId = :alunoId AND p.status IN ('AUSENTE', 'FALTA_NAO_JUSTIFICADA') AND p.dataPresenca BETWEEN :dataInicio AND :dataFim")
    Integer countFaltasNaoJustificadas(@Param("alunoId") Long alunoId,
                                       @Param("dataInicio") LocalDate dataInicio,
                                       @Param("dataFim") LocalDate dataFim);
    
    // Listar presenças de uma data específica
    List<Presenca> findByDataPresenca(LocalDate dataPresenca);
    
    // Listar presenças não justificadas de um aluno
    @Query("SELECT p FROM Presenca p WHERE p.alunoId = :alunoId AND p.justificado = false AND p.status IN ('AUSENTE', 'FALTA_NAO_JUSTIFICADA') ORDER BY p.dataPresenca DESC")
    List<Presenca> findFaltasNaoJustificadas(@Param("alunoId") Long alunoId);
    
    // Listar presenças com status específico
    List<Presenca> findByStatusAndDataPresenca(StatusPresenca status, LocalDate dataPresenca);
}
