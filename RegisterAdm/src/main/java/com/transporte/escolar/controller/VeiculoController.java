package com.transporte.escolar.controller;

import com.transporte.escolar.model.Veiculo;
import com.transporte.escolar.service.VeiculoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/veiculos")
public class VeiculoController {

    private final VeiculoService service;

    public VeiculoController(VeiculoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Veiculo> listar() {
        return service.listar();
    }

    @PostMapping
    public Veiculo criar(@RequestBody Veiculo veiculo) {
        return service.salvar(veiculo);
    }

    @DeleteMapping("/{id}")
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }

    // Atualizar capacidade (opcional, se precisar para o trabalho)
    @PutMapping("/{id}/capacidade")
    public Veiculo atualizarCapacidade(@PathVariable Long id, @RequestParam int capacidade) {
        Veiculo v = service.buscarPorId(id);
        v.setCapacidade(capacidade);
        return service.salvar(v);
    }
}
