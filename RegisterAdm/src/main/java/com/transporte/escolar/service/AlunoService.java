package com.transporte.escolar.service;

import com.transporte.escolar.config.AlunoEventProducer;
import com.transporte.escolar.model.Aluno;
import com.transporte.escolar.repository.AlunoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AlunoService {

    private final AlunoRepository repository;
    private final AlunoEventProducer alunoEventProducer;

    public AlunoService(AlunoRepository repository, AlunoEventProducer alunoEventProducer) {
        this.repository = repository;
        this.alunoEventProducer = alunoEventProducer;
    }

    public List<Aluno> listar() {
        return repository.findAll();
    }

    public Aluno buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno nao encontrado"));
    }

    public Aluno salvar(Aluno aluno) {
        boolean isNovo = aluno.getId() == null;
        Aluno salvo = repository.save(aluno);
        if (isNovo) {
            alunoEventProducer.publicarAlunoCadastrado(salvo);
        } else {
            alunoEventProducer.publicarAlunoAtualizado(salvo);
        }
        return salvo;
    }

    public void remover(Long id) {
        repository.deleteById(id);
    }
}
