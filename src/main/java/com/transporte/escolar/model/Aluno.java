package com.transporte.escolar.model;

import jakarta.persistence.*;

@Entity
public class Aluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private boolean confirmouPresenca;

    // Construtor vazio (necessário para JPA)
    public Aluno() {}

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public boolean isConfirmouPresenca() {
        return confirmouPresenca;
    }

    public void setConfirmouPresenca(boolean confirmouPresenca) {
        this.confirmouPresenca = confirmouPresenca;
    }
}
