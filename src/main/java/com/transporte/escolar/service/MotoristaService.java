package com.transporte.escolar.service;

import com.transporte.escolar.model.Motorista;
import com.transporte.escolar.repository.MotoristaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MotoristaService {

    private final MotoristaRepository repository;

    public MotoristaService(MotoristaRepository repository) {
        this.repository = repository;
    }

    public List<Motorista> listar() {
        return repository.findAll();
    }

    public Motorista salvar(Motorista motorista) {
        return repository.save(motorista);
    }
    public void remover(Long id) {
    repository.deleteById(id);
}
}
