package com.transportation.presenca.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ALUNO_EXCHANGE = "aluno.events";
    public static final String ALUNO_CADASTRADO_QUEUE = "presenca.aluno.cadastrado";
    public static final String ALUNO_ATUALIZADO_QUEUE = "presenca.aluno.atualizado";

    public static final String PRESENCA_EXCHANGE = "presenca.events";
    public static final String PRESENCA_REGISTRADA_ROUTING_KEY = "presenca.registrada";

    @Bean
    public TopicExchange alunoExchange() {
        return new TopicExchange(ALUNO_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange presencaExchange() {
        return new TopicExchange(PRESENCA_EXCHANGE, true, false);
    }

    @Bean
    public Queue alunoCadastradoQueue() {
        return new Queue(ALUNO_CADASTRADO_QUEUE, true);
    }

    @Bean
    public Queue alunoAtualizadoQueue() {
        return new Queue(ALUNO_ATUALIZADO_QUEUE, true);
    }

    @Bean
    public Binding bindingAlunoCadastrado() {
        return BindingBuilder
            .bind(alunoCadastradoQueue())
            .to(alunoExchange())
            .with("aluno.cadastrado");
    }

    @Bean
    public Binding bindingAlunoAtualizado() {
        return BindingBuilder
            .bind(alunoAtualizadoQueue())
            .to(alunoExchange())
            .with("aluno.atualizado");
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