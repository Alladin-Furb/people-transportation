package com.transportation.presenca.service;

import com.transportation.presenca.dto.CursoDTO;
import com.transportation.presenca.model.Curso;
import com.transportation.presenca.repository.CursoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CursoService {
    
    private final CursoRepository cursoRepository;
    
    /**
     * Criar novo curso
     */
    @Transactional
    public CursoDTO criarCurso(CursoDTO dto) {
        // Validar se código já existe
        cursoRepository.findByCodigo(dto.getCodigo())
                .ifPresent(c -> {
                    throw new IllegalArgumentException("Código de curso já cadastrado");
                });
        
        Curso curso = new Curso();
        curso.setCodigo(dto.getCodigo());
        curso.setNome(dto.getNome());
        curso.setDescricao(dto.getDescricao());
        curso.setFaculdade(dto.getFaculdade());
        curso.setCampus(dto.getCampus());
        curso.setCargaHoraria(dto.getCargaHoraria());
        curso.setPeriodo(dto.getPeriodo());
        curso.setAtivo(true);
        
        cursoRepository.save(curso);
        return toDTO(curso);
    }
    
    /**
     * Obter curso por ID
     */
    public CursoDTO obterCurso(Long id) {
        var curso = cursoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Curso não encontrado"));
        return toDTO(curso);
    }
    
    /**
     * Obter curso por código
     */
    public CursoDTO obterCursoPorCodigo(String codigo) {
        var curso = cursoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Curso não encontrado"));
        return toDTO(curso);
    }
    
    /**
     * Listar cursos ativos
     */
    public List<CursoDTO> listarCursos() {
        return cursoRepository.findByAtivoTrue()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Buscar cursos por nome
     */
    public List<CursoDTO> buscarCursos(String nome) {
        return cursoRepository.findByNomeContains(nome)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Listar cursos de uma faculdade
     */
    public List<CursoDTO> listarCursosPorFaculdade(String faculdade) {
        return cursoRepository.findByFaculdadeAndAtivoTrue(faculdade)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Listar cursos de um campus
     */
    public List<CursoDTO> listarCursosPorCampus(String campus) {
        return cursoRepository.findByCampusAndAtivoTrue(campus)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Listar cursos de uma faculdade e campus
     */
    public List<CursoDTO> listarCursosPorFaculdadeECampus(String faculdade, String campus) {
        return cursoRepository.findByFaculdadeAndCampus(faculdade, campus)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Atualizar curso
     */
    @Transactional
    public CursoDTO atualizarCurso(Long id, CursoDTO dto) {
        var curso = cursoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Curso não encontrado"));
        
        curso.setNome(dto.getNome());
        curso.setDescricao(dto.getDescricao());
        curso.setFaculdade(dto.getFaculdade());
        curso.setCampus(dto.getCampus());
        curso.setCargaHoraria(dto.getCargaHoraria());
        curso.setPeriodo(dto.getPeriodo());
        
        cursoRepository.save(curso);
        return toDTO(curso);
    }
    
    /**
     * Desativar curso
     */
    @Transactional
    public void desativarCurso(Long id) {
        var curso = cursoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Curso não encontrado"));
        curso.setAtivo(false);
        cursoRepository.save(curso);
    }
    
    /**
     * Mapear Curso para DTO
     */
    private CursoDTO toDTO(Curso curso) {
        return CursoDTO.builder()
                .id(curso.getId())
                .codigo(curso.getCodigo())
                .nome(curso.getNome())
                .descricao(curso.getDescricao())
                .faculdade(curso.getFaculdade())
                .campus(curso.getCampus())
                .cargaHoraria(curso.getCargaHoraria())
                .periodo(curso.getPeriodo())
                .ativo(curso.getAtivo())
                .criadoEm(curso.getCriadoEm())
                .atualizadoEm(curso.getAtualizadoEm())
                .build();
    }
}
