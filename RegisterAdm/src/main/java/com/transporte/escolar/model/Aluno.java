package com.transporte.escolar.model;

import jakarta.persistence.*;

@Entity
@Table(name = "alunos")
public class Aluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String matricula;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(name = "rota_transporte", length = 100)
    private String rotaTransporte;

    @Column(name = "curso_id")
    private Long cursoId;

    @Column(name = "nome_curso", length = 255)
    private String nomeCurso;

    @Column(length = 255)
    private String faculdade;

    @Column(name = "confirmou_presenca")
    private boolean confirmouPresenca;

    public Aluno() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getRotaTransporte() { return rotaTransporte; }
    public void setRotaTransporte(String rotaTransporte) { this.rotaTransporte = rotaTransporte; }
    public Long getCursoId() { return cursoId; }
    public void setCursoId(Long cursoId) { this.cursoId = cursoId; }
    public String getNomeCurso() { return nomeCurso; }
    public void setNomeCurso(String nomeCurso) { this.nomeCurso = nomeCurso; }
    public String getFaculdade() { return faculdade; }
    public void setFaculdade(String faculdade) { this.faculdade = faculdade; }
    public boolean isConfirmouPresenca() { return confirmouPresenca; }
    public void setConfirmouPresenca(boolean confirmouPresenca) { this.confirmouPresenca = confirmouPresenca; }
}