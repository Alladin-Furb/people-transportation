package com.transporte.escolar.service;

import com.transporte.escolar.model.Aluno;
import com.transporte.escolar.model.Motorista;
import com.transporte.escolar.model.Veiculo;
import com.transporte.escolar.repository.MotoristaRepository;
import com.transporte.escolar.repository.VeiculoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    private final AlunoService alunoService;
    private final MotoristaRepository motoristaRepository;
    private final VeiculoRepository veiculoRepository;

    public AdminService(AlunoService alunoService,
                        MotoristaRepository motoristaRepository,
                        VeiculoRepository veiculoRepository) {
        this.alunoService = alunoService;
        this.motoristaRepository = motoristaRepository;
        this.veiculoRepository = veiculoRepository;
    }

    public Aluno salvarAluno(Aluno aluno) {
        return alunoService.salvar(aluno);
    }

    public List<Aluno> listarAlunos() {
        return alunoService.listar();
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