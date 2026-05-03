package com.transportation.presenca.dto;

public record PresencaEventDto(
    Long id,
    Long alunoId,
    String alunoNome,
    String dataPresenca,
    String status
) {}