package com.transporte.escolar.controller;

import com.transporte.escolar.model.Aluno;
import com.transporte.escolar.service.AlunoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/alunos")
public class AlunoController {

    private final AlunoService service;

    public AlunoController(AlunoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Aluno> listar() {
        return service.listar();
    }

    @PostMapping
    public Aluno criar(@RequestBody Aluno aluno) {
        return service.salvar(aluno);
    }

    @PutMapping("/{id}/presenca")
    public Aluno atualizarPresenca(@PathVariable Long id, @RequestParam boolean presente) {
        Aluno aluno = service.buscarPorId(id);
        aluno.setConfirmouPresenca(presente);
        return service.salvar(aluno);
    }
}
