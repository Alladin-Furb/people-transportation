package com.transportation.presenca.service;

import com.transportation.presenca.config.PresencaEventProducer;
import com.transportation.presenca.dto.PresencaDTO;
import com.transportation.presenca.dto.RelatorioPresencaDTO;
import com.transportation.presenca.dto.RegistrarPresencaDTO;
import com.transportation.presenca.model.Aluno;
import com.transportation.presenca.model.Curso;
import com.transportation.presenca.model.Presenca;
import com.transportation.presenca.model.StatusPresenca;
import com.transportation.presenca.repository.AlunoRepository;
import com.transportation.presenca.repository.CursoRepository;
import com.transportation.presenca.repository.PresencaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PresencaService {

    private final PresencaRepository presencaRepository;
    private final AlunoRepository alunoRepository;
    private final CursoRepository cursoRepository;
    private final PresencaEventProducer presencaEventProducer;

    public PresencaService(PresencaRepository presencaRepository,
                           AlunoRepository alunoRepository,
                           CursoRepository cursoRepository,
                           PresencaEventProducer presencaEventProducer) {
        this.presencaRepository = presencaRepository;
        this.alunoRepository = alunoRepository;
        this.cursoRepository = cursoRepository;
        this.presencaEventProducer = presencaEventProducer;
    }

    @Transactional
    public PresencaDTO registrarPresenca(RegistrarPresencaDTO dto) {
        var aluno = alunoRepository.findById(dto.getAlunoId())
                .orElseThrow(() -> new IllegalArgumentException("Aluno nao encontrado"));
        var curso = cursoRepository.findById(dto.getCursoId())
                .orElseThrow(() -> new IllegalArgumentException("Curso nao encontrado"));

        presencaRepository.findByAlunoIdAndCursoIdAndDataPresenca(
                        dto.getAlunoId(), dto.getCursoId(), dto.getDataPresenca())
                .ifPresent(p -> {
                    throw new IllegalArgumentException("Ja existe registro de presenca para este aluno nesta data e curso");
                });

        Presenca presenca = new Presenca();
        preencherDadosAlunoCurso(presenca, aluno, curso);
        presenca.setDataPresenca(dto.getDataPresenca());
        presenca.setStatus(dto.getStatus());
        presenca.setHoraEntrada(dto.getHoraEntrada());
        presenca.setHoraSaida(dto.getHoraSaida());
        presenca.setMotivoFalta(dto.getMotivoFalta());
        presenca.setJustificativa(dto.getJustificativa());
        presenca.setObservacoes(dto.getObservacoes());
        presenca.setHorasAulaComparecidas(dto.getHorasAulaComparecidas());
        presenca.setHorasAulaTotal(dto.getHorasAulaTotal());
        presenca.setProfessorId(dto.getProfessorId());
        presenca.setJustificado(false);

        presencaRepository.save(presenca);
        presencaEventProducer.publicarPresencaRegistrada(presenca);
        return toDTO(presenca);
    }

    @Transactional
    public PresencaDTO confirmarPresencaHoje(Long alunoId, Long cursoId, StatusPresenca status) {
        return confirmarPresenca(alunoId, cursoId, LocalDate.now(), status);
    }

    @Transactional
    public PresencaDTO confirmarPresenca(Long alunoId, Long cursoId, LocalDate data, StatusPresenca status) {
        var aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new IllegalArgumentException("Aluno nao encontrado"));
        var curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new IllegalArgumentException("Curso nao encontrado"));

        var presenca = presencaRepository.findByAlunoIdAndCursoIdAndDataPresenca(alunoId, cursoId, data)
                .orElse(new Presenca());

        preencherDadosAlunoCurso(presenca, aluno, curso);
        presenca.setDataPresenca(data);
        presenca.setStatus(status);

        if (presenca.getHoraEntrada() == null) {
            presenca.setHoraEntrada(LocalDateTime.now());
        }

        presencaRepository.save(presenca);
        presencaEventProducer.publicarPresencaRegistrada(presenca);
        return toDTO(presenca);
    }

    @Transactional
    public PresencaDTO registrarAusencia(Long alunoId, Long cursoId, LocalDate data, String motivo, boolean justificado) {
        var aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new IllegalArgumentException("Aluno nao encontrado"));
        var curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new IllegalArgumentException("Curso nao encontrado"));

        var presenca = presencaRepository.findByAlunoIdAndCursoIdAndDataPresenca(alunoId, cursoId, data)
                .orElse(new Presenca());

        preencherDadosAlunoCurso(presenca, aluno, curso);
        presenca.setDataPresenca(data);
        presenca.setStatus(justificado ? StatusPresenca.FALTA_JUSTIFICADA : StatusPresenca.FALTA_NAO_JUSTIFICADA);
        presenca.setMotivoFalta(motivo);
        presenca.setJustificado(justificado);

        presencaRepository.save(presenca);
        presencaEventProducer.publicarPresencaRegistrada(presenca);
        return toDTO(presenca);
    }

    @Transactional
    public PresencaDTO justificarAusencia(Long presencaId, String justificativa) {
        var presenca = presencaRepository.findById(presencaId)
                .orElseThrow(() -> new IllegalArgumentException("Registro de presenca nao encontrado"));

        presenca.setJustificativa(justificativa);
        presenca.setJustificado(true);
        presenca.setStatus(StatusPresenca.FALTA_JUSTIFICADA);

        presencaRepository.save(presenca);
        presencaEventProducer.publicarPresencaRegistrada(presenca);
        return toDTO(presenca);
    }

    public PresencaDTO obterPresenca(Long id) {
        var presenca = presencaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro de presenca nao encontrado"));
        return toDTO(presenca);
    }

    public PresencaDTO obterPresencaPorData(Long alunoId, Long cursoId, LocalDate data) {
        var presenca = presencaRepository.findByAlunoIdAndCursoIdAndDataPresenca(alunoId, cursoId, data)
                .orElseThrow(() -> new IllegalArgumentException("Nenhum registro de presenca encontrado"));
        return toDTO(presenca);
    }

    public List<PresencaDTO> listarPresencasAluno(Long alunoId, LocalDate dataInicio, LocalDate dataFim) {
        return presencaRepository.findByAlunoIdAndPeriodo(alunoId, dataInicio, dataFim)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<PresencaDTO> listarPresencasCurso(Long cursoId, LocalDate dataInicio, LocalDate dataFim) {
        return presencaRepository.findByCursoIdAndPeriodo(cursoId, dataInicio, dataFim)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public RelatorioPresencaDTO gerarRelatorio(Long alunoId, Long cursoId, LocalDate dataInicio, LocalDate dataFim) {
        var aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new IllegalArgumentException("Aluno nao encontrado"));
        var curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new IllegalArgumentException("Curso nao encontrado"));

        var presencas = presencaRepository.findByAlunoIdAndPeriodo(alunoId, dataInicio, dataFim)
                .stream()
                .filter(p -> cursoId.equals(p.getCursoId()))
                .collect(Collectors.toList());

        int totalAulas = presencas.size();
        int presentes = contarPorStatus(presencas, StatusPresenca.PRESENTE);
        int ausentes = contarPorStatus(presencas, StatusPresenca.AUSENTE);
        int atrasados = contarPorStatus(presencas, StatusPresenca.ATRASADO);
        int faltasJustificadas = contarPorStatus(presencas, StatusPresenca.FALTA_JUSTIFICADA);
        int faltasNaoJustificadas = contarPorStatus(presencas, StatusPresenca.FALTA_NAO_JUSTIFICADA);
        int saidasAntecipadas = contarPorStatus(presencas, StatusPresenca.SAIDA_ANTECIPADA);

        double frequencia = totalAulas > 0
                ? ((presentes + atrasados + faltasJustificadas) * 100.0) / totalAulas
                : 0;

        return RelatorioPresencaDTO.builder()
                .alunoId(alunoId)
                .alunoMatricula(aluno.getMatricula())
                .alunoNome(aluno.getNome())
                .cursoId(cursoId)
                .cursoNome(curso.getNome())
                .totalAulas(totalAulas)
                .presentes(presentes)
                .ausentes(ausentes)
                .atrasados(atrasados)
                .faltasJustificadas(faltasJustificadas)
                .faltasNaoJustificadas(faltasNaoJustificadas)
                .saidasAntecipadas(saidasAntecipadas)
                .frequencia(frequencia)
                .statusFrequencia(getStatusFrequencia(frequencia))
                .build();
    }

    public List<PresencaDTO> listarFaltasNaoJustificadas(Long alunoId) {
        return presencaRepository.findFaltasNaoJustificadas(alunoId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Integer contarFaltasNaoJustificadas(Long alunoId, LocalDate dataInicio, LocalDate dataFim) {
        return presencaRepository.countFaltasNaoJustificadas(alunoId, dataInicio, dataFim);
    }

    private PresencaDTO toDTO(Presenca presenca) {
        return PresencaDTO.builder()
                .id(presenca.getId())
                .alunoId(presenca.getAlunoId())
                .alunoMatricula(presenca.getAlunoMatricula())
                .alunoNome(presenca.getAlunoNome())
                .cursoId(presenca.getCursoId())
                .cursoNome(presenca.getCursoNome())
                .alunoCurso(presenca.getAlunoCurso())
                .faculdade(presenca.getFaculdade())
                .dataPresenca(presenca.getDataPresenca())
                .status(presenca.getStatus())
                .horaEntrada(presenca.getHoraEntrada())
                .horaSaida(presenca.getHoraSaida())
                .motivoFalta(presenca.getMotivoFalta())
                .justificativa(presenca.getJustificativa())
                .observacoes(presenca.getObservacoes())
                .horasAulaComparecidas(presenca.getHorasAulaComparecidas())
                .horasAulaTotal(presenca.getHorasAulaTotal())
                .professorId(presenca.getProfessorId())
                .professorNome(presenca.getProfessorNome())
                .justificado(presenca.getJustificado())
                .criadoEm(presenca.getCriadoEm())
                .atualizadoEm(presenca.getAtualizadoEm())
                .build();
    }

    private void preencherDadosAlunoCurso(Presenca presenca, Aluno aluno, Curso curso) {
        presenca.setAlunoId(aluno.getId());
        presenca.setAlunoMatricula(aluno.getMatricula());
        presenca.setAlunoNome(aluno.getNome());
        presenca.setCursoId(curso.getId());
        presenca.setCursoNome(curso.getNome());
        presenca.setAlunoCurso(aluno.getNomeCurso());
        presenca.setFaculdade(curso.getFaculdade());
    }

    private int contarPorStatus(List<Presenca> presencas, StatusPresenca status) {
        return (int) presencas.stream()
                .filter(p -> p.getStatus() == status)
                .count();
    }

    private String getStatusFrequencia(double frequencia) {
        if (frequencia >= 75) {
            return "OK";
        }

        if (frequencia >= 50) {
            return "AVISO";
        }

        return "CRITICO";
    }
}
