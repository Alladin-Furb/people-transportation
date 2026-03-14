package com.transportation.presenca.controller;

import com.transportation.presenca.dto.DisciplinaDTO;
import com.transportation.presenca.service.DisciplinaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/disciplinas")
@RequiredArgsConstructor
public class DisciplinaController {
    
    private final DisciplinaService disciplinaService;
    
    /**
     * Criar nova disciplina
     */
    @PostMapping
    public ResponseEntity<DisciplinaDTO> criarDisciplina(@RequestBody DisciplinaDTO dto) {
        DisciplinaDTO novaDisciplina = disciplinaService.criarDisciplina(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaDisciplina);
    }
    
    /**
     * Obter disciplina por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<DisciplinaDTO> obterDisciplina(@PathVariable Long id) {
        DisciplinaDTO disciplina = disciplinaService.obterDisciplina(id);
        return ResponseEntity.ok(disciplina);
    }
    
    /**
     * Obter disciplina por código
     */
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<DisciplinaDTO> obterDisciplinaPorCodigo(@PathVariable String codigo) {
        DisciplinaDTO disciplina = disciplinaService.obterDisciplinaPorCodigo(codigo);
        return ResponseEntity.ok(disciplina);
    }
    
    /**
     * Listar disciplinas ativas
     */
    @GetMapping
    public ResponseEntity<List<DisciplinaDTO>> listarDisciplinas() {
        List<DisciplinaDTO> disciplinas = disciplinaService.listarDisciplinas();
        return ResponseEntity.ok(disciplinas);
    }
    
    /**
     * Buscar disciplinas por nome
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<DisciplinaDTO>> buscarDisciplinas(@RequestParam String nome) {
        List<DisciplinaDTO> disciplinas = disciplinaService.buscarDisciplinas(nome);
        return ResponseEntity.ok(disciplinas);
    }
    
    /**
     * Listar disciplinas de um professor
     */
    @GetMapping("/professor/{professorId}")
    public ResponseEntity<List<DisciplinaDTO>> listarDisciplinasProfessor(@PathVariable Long professorId) {
        List<DisciplinaDTO> disciplinas = disciplinaService.listarDisciplinasProfessor(professorId);
        return ResponseEntity.ok(disciplinas);
    }
    
    /**
     * Atualizar disciplina
     */
    @PutMapping("/{id}")
    public ResponseEntity<DisciplinaDTO> atualizarDisciplina(@PathVariable Long id, @RequestBody DisciplinaDTO dto) {
        DisciplinaDTO disciplinaAtualizada = disciplinaService.atualizarDisciplina(id, dto);
        return ResponseEntity.ok(disciplinaAtualizada);
    }
    
    /**
     * Desativar disciplina
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativarDisciplina(@PathVariable Long id) {
        disciplinaService.desativarDisciplina(id);
        return ResponseEntity.noContent().build();
    }
}
