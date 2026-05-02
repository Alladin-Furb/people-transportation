package com.transportation.presenca.config;

import com.transportation.presenca.model.Aluno;
import com.transportation.presenca.repository.AlunoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlunoEventConsumer {

    private final AlunoRepository alunoRepository;

    @RabbitListener(queues = RabbitMQConfig.ALUNO_CADASTRADO_QUEUE)
    public void consumirAlunoCadastrado(Aluno aluno) {
        log.info("Aluno cadastrado recebido: {}", aluno.getNome());
        alunoRepository.findByMatricula(aluno.getMatricula())
            .ifPresentOrElse(
                existente -> log.info("Aluno {} já existe, ignorando.", aluno.getMatricula()),
                () -> alunoRepository.save(aluno)
            );
    }

    @RabbitListener(queues = RabbitMQConfig.ALUNO_ATUALIZADO_QUEUE)
    public void consumirAlunoAtualizado(Aluno aluno) {
        log.info("Aluno atualizado recebido: {}", aluno.getNome());
        alunoRepository.findByMatricula(aluno.getMatricula())
            .ifPresentOrElse(
                existente -> {
                    existente.setNome(aluno.getNome());
                    existente.setEmail(aluno.getEmail());
                    existente.setTelefone(aluno.getTelefone());
                    existente.setRotaTransporte(aluno.getRotaTransporte());
                    alunoRepository.save(existente);
                },
                () -> alunoRepository.save(aluno)
            );
    }
}