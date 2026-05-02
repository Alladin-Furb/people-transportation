package com.transporte.escolar.service;

import com.transporte.escolar.config.AlunoEventProducer;
import com.transporte.escolar.model.Aluno;
import com.transporte.escolar.model.Motorista;
import com.transporte.escolar.model.Veiculo;
import com.transporte.escolar.repository.AlunoRepository;
import com.transporte.escolar.repository.MotoristaRepository;
import com.transporte.escolar.repository.VeiculoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    private final AlunoRepository alunoRepository;
    private final MotoristaRepository motoristaRepository;
    private final VeiculoRepository veiculoRepository;
    private final AlunoEventProducer alunoEventProducer;

    public AdminService(AlunoRepository alunoRepository,
                        MotoristaRepository motoristaRepository,
                        VeiculoRepository veiculoRepository,
                        AlunoEventProducer alunoEventProducer) {
        this.alunoRepository = alunoRepository;
        this.motoristaRepository = motoristaRepository;
        this.veiculoRepository = veiculoRepository;
        this.alunoEventProducer = alunoEventProducer;
    }

    public Aluno salvarAluno(Aluno aluno) {
        boolean isNovo = aluno.getId() == null;
        Aluno salvo = alunoRepository.save(aluno);
        if (isNovo) {
            alunoEventProducer.publicarAlunoCadastrado(salvo);
        } else {
            alunoEventProducer.publicarAlunoAtualizado(salvo);
        }
        return salvo;
    }

    public List<Aluno> listarAlunos() {
        return alunoRepository.findAll();
    }

    public Motorista salvarMotorista(Motorista motorista) {
        return motoristaRepository.save(motorista);
    }

    public List<Motorista> listarMotoristas() {
        return motoristaRepository.findAll();
    }

    public Veiculo salvarVeiculo(Veiculo veiculo) {
        return veiculoRepository.save(veiculo);
    }

    public List<Veiculo> listarVeiculos() {
        return veiculoRepository.findAll();
    }
}