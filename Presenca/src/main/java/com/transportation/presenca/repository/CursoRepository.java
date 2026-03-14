package com.transportation.presenca.repository;

import com.transportation.presenca.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CursoRepository extends JpaRepository<Curso, Long> {
    
    // Buscar curso por código
    Optional<Curso> findByCodigo(String codigo);
    
    // Listar cursos ativos
    List<Curso> findByAtivoTrue();
    
    // Buscar cursos de uma faculdade
    List<Curso> findByFaculdadeAndAtivoTrue(String faculdade);
    
    // Buscar cursos por campus
    List<Curso> findByCampusAndAtivoTrue(String campus);
    
    // Verificar se curso existe e está ativo
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Curso c WHERE c.id = :id AND c.ativo = true")
    Boolean isAtivoById(@Param("id") Long id);
    
    // Buscar curso por nome (parcial)
    @Query("SELECT c FROM Curso c WHERE LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%')) AND c.ativo = true")
    List<Curso> findByNomeContains(@Param("nome") String nome);
    
    // Listar cursos de uma faculdade e campus
    @Query("SELECT c FROM Curso c WHERE c.faculdade = :faculdade AND c.campus = :campus AND c.ativo = true")
    List<Curso> findByFaculdadeAndCampus(@Param("faculdade") String faculdade, @Param("campus") String campus);
}
