package com.transportation.presenca.service;

import com.transportation.presenca.dto.DisciplinaDTO;
import com.transportation.presenca.model.Disciplina;
import com.transportation.presenca.repository.DisciplinaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DisciplinaService {
    
    private final DisciplinaRepository disciplinaRepository;
    
    /**
     * Criar nova disciplina
     */
    @Transactional
    public DisciplinaDTO criarDisciplina(DisciplinaDTO dto) {
        // Validar se código já existe
        disciplinaRepository.findByCodigo(dto.getCodigo())
                .ifPresent(d -> {
                    throw new IllegalArgumentException("Código de disciplina já cadastrado");
                });
        
        Disciplina disciplina = new Disciplina();
        disciplina.setCodigo(dto.getCodigo());
        disciplina.setNome(dto.getNome());
        disciplina.setDescricao(dto.getDescricao());
        disciplina.setProfessorId(dto.getProfessorId());
        disciplina.setProfessorNome(dto.getProfessorNome());
        disciplina.setCargaHoraria(dto.getCargaHoraria());
        disciplina.setPeriodo(dto.getPeriodo());
        disciplina.setAtiva(true);
        
        disciplinaRepository.save(disciplina);
        return toDTO(disciplina);
    }
    
    /**
     * Obter disciplina por ID
     */
    public DisciplinaDTO obterDisciplina(Long id) {
        var disciplina = disciplinaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disciplina não encontrada"));
        return toDTO(disciplina);
    }
    
    /**
     * Obter disciplina por código
     */
    public DisciplinaDTO obterDisciplinaPorCodigo(String codigo) {
        var disciplina = disciplinaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Disciplina não encontrada"));
        return toDTO(disciplina);
    }
    
    /**
     * Listar disciplinas ativas
     */
    public List<DisciplinaDTO> listarDisciplinas() {
        return disciplinaRepository.findByAtivaTrue()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Buscar disciplinas por nome
     */
    public List<DisciplinaDTO> buscarDisciplinas(String nome) {
        return disciplinaRepository.findByNomeContains(nome)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Listar disciplinas de um professor
     */
    public List<DisciplinaDTO> listarDisciplinasProfessor(Long professorId) {
        return disciplinaRepository.findByProfessorIdAndAtivaTrue(professorId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Atualizar disciplina
     */
    @Transactional
    public DisciplinaDTO atualizarDisciplina(Long id, DisciplinaDTO dto) {
        var disciplina = disciplinaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disciplina não encontrada"));
        
        disciplina.setNome(dto.getNome());
        disciplina.setDescricao(dto.getDescricao());
        disciplina.setProfessorId(dto.getProfessorId());
        disciplina.setProfessorNome(dto.getProfessorNome());
        disciplina.setCargaHoraria(dto.getCargaHoraria());
        disciplina.setPeriodo(dto.getPeriodo());
        
        disciplinaRepository.save(disciplina);
        return toDTO(disciplina);
    }
    
    /**
     * Desativar disciplina
     */
    @Transactional
    public void desativarDisciplina(Long id) {
        var disciplina = disciplinaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disciplina não encontrada"));
        disciplina.setAtiva(false);
        disciplinaRepository.save(disciplina);
    }
    
    /**
     * Mapear Disciplina para DTO
     */
    private DisciplinaDTO toDTO(Disciplina disciplina) {
        return DisciplinaDTO.builder()
                .id(disciplina.getId())
                .codigo(disciplina.getCodigo())
                .nome(disciplina.getNome())
                .descricao(disciplina.getDescricao())
                .professorId(disciplina.getProfessorId())
                .professorNome(disciplina.getProfessorNome())
                .cargaHoraria(disciplina.getCargaHoraria())
                .periodo(disciplina.getPeriodo())
                .ativa(disciplina.getAtiva())
                .criadoEm(disciplina.getCriadoEm())
                .atualizadoEm(disciplina.getAtualizadoEm())
                .build();
    }
}
