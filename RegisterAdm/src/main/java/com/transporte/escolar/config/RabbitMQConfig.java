package com.transporte.escolar.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ALUNO_EXCHANGE = "aluno.events";
    public static final String ALUNO_CADASTRADO_ROUTING_KEY = "aluno.cadastrado";
    public static final String ALUNO_ATUALIZADO_ROUTING_KEY = "aluno.atualizado";

    @Bean
    public TopicExchange alunoExchange() {
        return new TopicExchange(ALUNO_EXCHANGE, true, false);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
