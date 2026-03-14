package com.transportation.presenca.controller;

import com.transportation.presenca.dto.PresencaDTO;
import com.transportation.presenca.dto.RelatorioPresencaDTO;
import com.transportation.presenca.dto.RegistrarPresencaDTO;
import com.transportation.presenca.model.StatusPresenca;
import com.transportation.presenca.service.PresencaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/presencas")
@RequiredArgsConstructor
public class PresencaController {
    
    private final PresencaService presencaService;
    
    /**
     * Registrar nova presença
     */
    @PostMapping
    public ResponseEntity<PresencaDTO> registrarPresenca(@RequestBody RegistrarPresencaDTO dto) {
        PresencaDTO presenca = presencaService.registrarPresenca(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(presenca);
    }
    
    /**
     * Confirmar presença para hoje
     */
    @PostMapping("/aluno/{alunoId}/curso/{cursoId}/confirmar-hoje")
    public ResponseEntity<PresencaDTO> confirmarPresencaHoje(
            @PathVariable Long alunoId,
            @PathVariable Long cursoId,
            @RequestParam StatusPresenca status) {
        PresencaDTO presenca = presencaService.confirmarPresencaHoje(alunoId, cursoId, status);
        return ResponseEntity.status(HttpStatus.CREATED).body(presenca);
    }
    
    /**
     * Confirmar presença em data específica
     */
    @PostMapping("/aluno/{alunoId}/curso/{cursoId}/confirmar")
    public ResponseEntity<PresencaDTO> confirmarPresenca(
            @PathVariable Long alunoId,
            @PathVariable Long cursoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam StatusPresenca status) {
        PresencaDTO presenca = presencaService.confirmarPresenca(alunoId, cursoId, data, status);
        return ResponseEntity.status(HttpStatus.CREATED).body(presenca);
    }
    
    /**
     * Registrar ausência
     */
    @PostMapping("/aluno/{alunoId}/curso/{cursoId}/ausencia")
    public ResponseEntity<PresencaDTO> registrarAusencia(
            @PathVariable Long alunoId,
            @PathVariable Long cursoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam String motivo,
            @RequestParam(defaultValue = "false") boolean justificado) {
        PresencaDTO presenca = presencaService.registrarAusencia(alunoId, cursoId, data, motivo, justificado);
        return ResponseEntity.status(HttpStatus.CREATED).body(presenca);
    }
    
    /**
     * Justificar ausência
     */
    @PutMapping("/{id}/justificar")
    public ResponseEntity<PresencaDTO> justificarAusencia(
            @PathVariable Long id,
            @RequestParam String justificativa) {
        PresencaDTO presenca = presencaService.justificarAusencia(id, justificativa);
        return ResponseEntity.ok(presenca);
    }
    
    /**
     * Obter presença por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PresencaDTO> obterPresenca(@PathVariable Long id) {
        PresencaDTO presenca = presencaService.obterPresenca(id);
        return ResponseEntity.ok(presenca);
    }
    
    /**
     * Obter presença de um aluno em uma data
     */
    @GetMapping("/aluno/{alunoId}/curso/{cursoId}")
    public ResponseEntity<PresencaDTO> obterPresencaPorData(
            @PathVariable Long alunoId,
            @PathVariable Long cursoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        PresencaDTO presenca = presencaService.obterPresencaPorData(alunoId, cursoId, data);
        return ResponseEntity.ok(presenca);
    }
    
    /**
     * Listar presenças de um aluno em um período
     */
    @GetMapping("/aluno/{alunoId}/periodo")
    public ResponseEntity<List<PresencaDTO>> listarPresencasAluno(
            @PathVariable Long alunoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        List<PresencaDTO> presenças = presencaService.listarPresencasAluno(alunoId, dataInicio, dataFim);
        return ResponseEntity.ok(presenças);
    }
    
    /**
     * Listar presenças de um curso em um período
     */
    @GetMapping("/curso/{cursoId}/periodo")
    public ResponseEntity<List<PresencaDTO>> listarPresencasCurso(
            @PathVariable Long cursoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        List<PresencaDTO> presenças = presencaService.listarPresencasCurso(cursoId, dataInicio, dataFim);
        return ResponseEntity.ok(presenças);
    }
    
    /**
     * Gerar relatório de presença
     */
    @GetMapping("/relatorio/aluno/{alunoId}/curso/{cursoId}")
    public ResponseEntity<RelatorioPresencaDTO> gerarRelatorio(
            @PathVariable Long alunoId,
            @PathVariable Long cursoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        RelatorioPresencaDTO relatorio = presencaService.gerarRelatorio(alunoId, cursoId, dataInicio, dataFim);
        return ResponseEntity.ok(relatorio);
    }
    
    /**
     * Listar faltas não justificadas
     */
    @GetMapping("/aluno/{alunoId}/faltas-nao-justificadas")
    public ResponseEntity<List<PresencaDTO>> listarFaltasNaoJustificadas(@PathVariable Long alunoId) {
        List<PresencaDTO> faltas = presencaService.listarFaltasNaoJustificadas(alunoId);
        return ResponseEntity.ok(faltas);
    }
    
    /**
     * Contar faltas não justificadas
     */
    @GetMapping("/aluno/{alunoId}/contar-faltas")
    public ResponseEntity<Integer> contarFaltasNaoJustificadas(
            @PathVariable Long alunoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        Integer faltas = presencaService.contarFaltasNaoJustificadas(alunoId, dataInicio, dataFim);
        return ResponseEntity.ok(faltas);
    }
}
