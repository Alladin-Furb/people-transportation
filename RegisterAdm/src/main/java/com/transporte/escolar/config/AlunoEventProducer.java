package com.transporte.escolar.config;

import com.transporte.escolar.model.Aluno;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class AlunoEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public AlunoEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarAlunoCadastrado(Aluno aluno) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.ALUNO_EXCHANGE,
            RabbitMQConfig.ALUNO_CADASTRADO_ROUTING_KEY,
            aluno
        );
    }

    public void publicarAlunoAtualizado(Aluno aluno) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.ALUNO_EXCHANGE,
            RabbitMQConfig.ALUNO_ATUALIZADO_ROUTING_KEY,
            aluno
        );
    }
}
