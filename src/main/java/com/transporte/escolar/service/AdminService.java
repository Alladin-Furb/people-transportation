package com.transporte.escolar.service;

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

    public AdminService(AlunoRepository alunoRepository,
                        MotoristaRepository motoristaRepository,
                        VeiculoRepository veiculoRepository) {
        this.alunoRepository = alunoRepository;
        this.motoristaRepository = motoristaRepository;
        this.veiculoRepository = veiculoRepository;
    }

    // Alunos
    public Aluno salvarAluno(Aluno aluno) {
        return alunoRepository.save(aluno);
    }

    public List<Aluno> listarAlunos() {
        return alunoRepository.findAll();
    }

    // Motoristas
    public Motorista salvarMotorista(Motorista motorista) {
        return motoristaRepository.save(motorista);
    }

    public List<Motorista> listarMotoristas() {
        return motoristaRepository.findAll();
    }

    // Veículos
    public Veiculo salvarVeiculo(Veiculo veiculo) {
        return veiculoRepository.save(veiculo);
    }

    public List<Veiculo> listarVeiculos() {
        return veiculoRepository.findAll();
    }
}
