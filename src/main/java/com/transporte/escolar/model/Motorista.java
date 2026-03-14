package com.transporte.escolar.model;

import jakarta.persistence.*;

@Entity
public class Motorista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private String carteiraMotorista;

    public Motorista() {}

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCarteiraMotorista() {
        return carteiraMotorista;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setCarteiraMotorista(String carteiraMotorista) {
        this.carteiraMotorista = carteiraMotorista;
    }
}
