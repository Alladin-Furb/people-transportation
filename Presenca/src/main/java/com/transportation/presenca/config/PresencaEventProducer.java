package com.transportation.presenca.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.transportation.presenca.model.Presenca;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PresencaEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publicarPresencaRegistrada(Presenca presenca) {
    try {
        log.info("Publicando presenca.registrada para aluno {}", presenca.getAlunoId());
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.PRESENCA_EXCHANGE,
            RabbitMQConfig.PRESENCA_REGISTRADA_ROUTING_KEY,
            presenca
        );
    } catch (Exception e) {
        log.warn("Falha ao publicar evento de presenca: {}", e.getMessage());
    }
}
}