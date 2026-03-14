package com.transportation.presenca.controller;

import com.transportation.presenca.dto.CursoDTO;
import com.transportation.presenca.service.CursoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cursos")
@RequiredArgsConstructor
public class CursoController {
    
    private final CursoService cursoService;
    
    /**
     * Criar novo curso
     */
    @PostMapping
    public ResponseEntity<CursoDTO> criarCurso(@RequestBody CursoDTO dto) {
        CursoDTO novoCurso = cursoService.criarCurso(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoCurso);
    }
    
    /**
     * Obter curso por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CursoDTO> obterCurso(@PathVariable Long id) {
        CursoDTO curso = cursoService.obterCurso(id);
        return ResponseEntity.ok(curso);
    }
    
    /**
     * Obter curso por código
     */
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<CursoDTO> obterCursoPorCodigo(@PathVariable String codigo) {
        CursoDTO curso = cursoService.obterCursoPorCodigo(codigo);
        return ResponseEntity.ok(curso);
    }
    
    /**
     * Listar cursos ativos
     */
    @GetMapping
    public ResponseEntity<List<CursoDTO>> listarCursos() {
        List<CursoDTO> cursos = cursoService.listarCursos();
        return ResponseEntity.ok(cursos);
    }
    
    /**
     * Buscar cursos por nome
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<CursoDTO>> buscarCursos(@RequestParam String nome) {
        List<CursoDTO> cursos = cursoService.buscarCursos(nome);
        return ResponseEntity.ok(cursos);
    }
    
    /**
     * Listar cursos de uma faculdade
     */
    @GetMapping("/faculdade/{faculdade}")
    public ResponseEntity<List<CursoDTO>> listarCursosPorFaculdade(@PathVariable String faculdade) {
        List<CursoDTO> cursos = cursoService.listarCursosPorFaculdade(faculdade);
        return ResponseEntity.ok(cursos);
    }
    
    /**
     * Listar cursos de um campus
     */
    @GetMapping("/campus/{campus}")
    public ResponseEntity<List<CursoDTO>> listarCursosPorCampus(@PathVariable String campus) {
        List<CursoDTO> cursos = cursoService.listarCursosPorCampus(campus);
        return ResponseEntity.ok(cursos);
    }
    
    /**
     * Listar cursos de uma faculdade e campus
     */
    @GetMapping("/faculdade/{faculdade}/campus/{campus}")
    public ResponseEntity<List<CursoDTO>> listarCursosPorFaculdadeECampus(
            @PathVariable String faculdade,
            @PathVariable String campus) {
        List<CursoDTO> cursos = cursoService.listarCursosPorFaculdadeECampus(faculdade, campus);
        return ResponseEntity.ok(cursos);
    }
    
    /**
     * Atualizar curso
     */
    @PutMapping("/{id}")
    public ResponseEntity<CursoDTO> atualizarCurso(@PathVariable Long id, @RequestBody CursoDTO dto) {
        CursoDTO cursoAtualizado = cursoService.atualizarCurso(id, dto);
        return ResponseEntity.ok(cursoAtualizado);
    }
    
    /**
     * Desativar curso
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativarCurso(@PathVariable Long id) {
        cursoService.desativarCurso(id);
        return ResponseEntity.noContent().build();
    }
}
