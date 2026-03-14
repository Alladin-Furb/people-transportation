package com.transportation.presenca.repository;

import com.transportation.presenca.model.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlunoRepository extends JpaRepository<Aluno, Long> {
    
    // Buscar aluno por matrícula
    Optional<Aluno> findByMatricula(String matricula);
    
    // Buscar aluno por email
    Optional<Aluno> findByEmail(String email);
    
    // Listar alunos ativos
    List<Aluno> findByAtivoTrue();
    
    // Listar alunos por rota de transporte
    List<Aluno> findByRotaTransporteAndAtivoTrue(String rotaTransporte);
    
    // Verificar se aluno existe e está ativo
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Aluno a WHERE a.id = :id AND a.ativo = true")
    Boolean isAtivoById(@Param("id") Long id);
    
    // Contar alunos ativos por rota
    @Query("SELECT COUNT(a) FROM Aluno a WHERE a.rotaTransporte = :rota AND a.ativo = true")
    Integer countByRota(@Param("rota") String rota);
}
