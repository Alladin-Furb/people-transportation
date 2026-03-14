package com.transportation.presenca.controller;

import com.transportation.presenca.dto.AlunoDTO;
import com.transportation.presenca.service.AlunoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alunos")
@RequiredArgsConstructor
public class AlunoController {
    
    private final AlunoService alunoService;
    
    /**
     * Criar novo aluno
     */
    @PostMapping
    public ResponseEntity<AlunoDTO> criarAluno(@RequestBody AlunoDTO dto) {
        AlunoDTO novoAluno = alunoService.criarAluno(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoAluno);
    }
    
    /**
     * Obter aluno por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AlunoDTO> obterAluno(@PathVariable Long id) {
        AlunoDTO aluno = alunoService.obterAluno(id);
        return ResponseEntity.ok(aluno);
    }
    
    /**
     * Obter aluno por matrícula
     */
    @GetMapping("/matricula/{matricula}")
    public ResponseEntity<AlunoDTO> obterAlunoPorMatricula(@PathVariable String matricula) {
        AlunoDTO aluno = alunoService.obterAlunoPorMatricula(matricula);
        return ResponseEntity.ok(aluno);
    }
    
    /**
     * Listar alunos ativos
     */
    @GetMapping
    public ResponseEntity<List<AlunoDTO>> listarAlunos() {
        List<AlunoDTO> alunos = alunoService.listarAlunos();
        return ResponseEntity.ok(alunos);
    }
    
    /**
     * Listar alunos por rota de transporte
     */
    @GetMapping("/rota/{rota}")
    public ResponseEntity<List<AlunoDTO>> listarAlunosPorRota(@PathVariable String rota) {
        List<AlunoDTO> alunos = alunoService.listarAlunosPorRota(rota);
        return ResponseEntity.ok(alunos);
    }
    
    /**
     * Atualizar aluno
     */
    @PutMapping("/{id}")
    public ResponseEntity<AlunoDTO> atualizarAluno(@PathVariable Long id, @RequestBody AlunoDTO dto) {
        AlunoDTO alunoAtualizado = alunoService.atualizarAluno(id, dto);
        return ResponseEntity.ok(alunoAtualizado);
    }
    
    /**
     * Desativar aluno
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativarAluno(@PathVariable Long id) {
        alunoService.desativarAluno(id);
        return ResponseEntity.noContent().build();
    }
}
