package com.transportation.presenca.repository;

import com.transportation.presenca.model.Disciplina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DisciplinaRepository extends JpaRepository<Disciplina, Long> {
    
    // Buscar disciplina por código
    Optional<Disciplina> findByCodigo(String codigo);
    
    // Listar disciplinas ativas
    List<Disciplina> findByAtivaTrue();
    
    // Buscar disciplinas de um professor
    List<Disciplina> findByProfessorIdAndAtivaTrue(Long professorId);
    
    // Verificar se disciplina existe e está ativa
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM Disciplina d WHERE d.id = :id AND d.ativa = true")
    Boolean isAtivaById(@Param("id") Long id);
    
    // Buscar disciplina por nome (parcial)
    @Query("SELECT d FROM Disciplina d WHERE LOWER(d.nome) LIKE LOWER(CONCAT('%', :nome, '%')) AND d.ativa = true")
    List<Disciplina> findByNomeContains(@Param("nome") String nome);
}
