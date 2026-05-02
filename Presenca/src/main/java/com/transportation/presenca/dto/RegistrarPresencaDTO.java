package com.transportation.presenca.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.transportation.presenca.model.StatusPresenca;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrarPresencaDTO {
    
    @NotNull(message = "O aluno e obrigatorio")
    @Positive(message = "O aluno deve ser maior que zero")
    private Long alunoId;
    
    @NotNull(message = "O curso e obrigatorio")
    @Positive(message = "O curso deve ser maior que zero")
    private Long cursoId;
    
    @NotNull(message = "A data da presenca e obrigatoria")
    @PastOrPresent(message = "A data da presenca nao pode ser futura")
    private LocalDate dataPresenca;
    
    @NotNull(message = "O status da presenca e obrigatorio")
    private StatusPresenca status;
    
    private LocalDateTime horaEntrada;
    
    private LocalDateTime horaSaida;
    
    private String motivoFalta;
    
    private String justificativa;
    
    private String observacoes;
    
    @PositiveOrZero(message = "As horas comparecidas nao podem ser negativas")
    private Integer horasAulaComparecidas;
    
    @Positive(message = "O total de horas-aula deve ser maior que zero")
    private Integer horasAulaTotal;
    
    @Positive(message = "O professor deve ser maior que zero")
    private Long professorId;

    @JsonIgnore
    @AssertTrue(message = "A hora de saída deve ser posterior a hora de entrada")
    public boolean isHorarioValido() {
        return horaEntrada == null || horaSaida == null || horaSaida.isAfter(horaEntrada);
    }

    @JsonIgnore
    @AssertTrue(message = "As horas comparecidas nao podem ser maiores que o total de horas-aula")
    public boolean isCargaHorariaValida() {
        return horasAulaComparecidas == null
                || horasAulaTotal == null
                || horasAulaComparecidas <= horasAulaTotal;
    }
}
