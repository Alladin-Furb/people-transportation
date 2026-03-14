package com.transportation.presenca.model;

public enum StatusPresenca {
    PRESENTE("Presente"),
    AUSENTE("Ausente"),
    ATRASADO("Atrasado"),
    SAIDA_ANTECIPADA("Saída Antecipada"),
    FALTA_JUSTIFICADA("Falta Justificada"),
    FALTA_NAO_JUSTIFICADA("Falta Não Justificada"),
    PENDENTE("Pendente"),
    CANCELADO("Cancelado");
    
    private final String descricao;
    
    StatusPresenca(String descricao) {
        this.descricao = descricao;
    }
    
    public String getDescricao() {
        return descricao;
    }
}
