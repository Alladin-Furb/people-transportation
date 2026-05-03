package com.transportation.presenca.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.transportation.presenca.dto.PresencaEventDto;
import com.transportation.presenca.model.Presenca;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PresencaEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publicarPresencaRegistrada(Presenca presenca) {
        var dto = new PresencaEventDto(
            presenca.getId(),
            presenca.getAlunoId(),
            presenca.getAlunoNome(),
            presenca.getDataPresenca().toString(),
            presenca.getStatus().name()
        );
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.PRESENCA_EXCHANGE,
            RabbitMQConfig.PRESENCA_REGISTRADA_ROUTING_KEY,
            dto
        );
        log.info("Evento presenca.registrada publicado para aluno {}", presenca.getAlunoId());
    }
}