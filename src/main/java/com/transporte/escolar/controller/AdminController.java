package com.transporte.escolar.controller;

import com.transporte.escolar.model.Aluno;
import com.transporte.escolar.model.Motorista;
import com.transporte.escolar.model.Veiculo;
import com.transporte.escolar.service.AdminService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @PostMapping("/alunos")
    public Aluno cadastrarAluno(@RequestBody Aluno aluno) {
        return service.salvarAluno(aluno);
    }

    @GetMapping("/alunos")
    public List<Aluno> listarAlunos() {
        return service.listarAlunos();
    }

    @PostMapping("/motoristas")
    public Motorista cadastrarMotorista(@RequestBody Motorista motorista) {
        return service.salvarMotorista(motorista);
    }

    @GetMapping("/motoristas")
    public List<Motorista> listarMotoristas() {
        return service.listarMotoristas();
    }

    @PostMapping("/veiculos")
    public Veiculo cadastrarVeiculo(@RequestBody Veiculo veiculo) {
        return service.salvarVeiculo(veiculo);
    }

    @GetMapping("/veiculos")
    public List<Veiculo> listarVeiculos() {
        return service.listarVeiculos();
    }
}
