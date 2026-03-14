package com.transportation.presenca.service;

import com.transportation.presenca.dto.PresencaDTO;
import com.transportation.presenca.dto.RelatorioPresencaDTO;
import com.transportation.presenca.dto.RegistrarPresencaDTO;
import com.transportation.presenca.model.Presenca;
import com.transportation.presenca.model.StatusPresenca;
import com.transportation.presenca.repository.AlunoRepository;
import com.transportation.presenca.repository.CursoRepository;
import com.transportation.presenca.repository.PresencaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PresencaService {
    
    private final PresencaRepository presencaRepository;
    private final AlunoRepository alunoRepository;
    private final CursoRepository cursoRepository;
    
    /**
     * Registrar presença de um aluno
     */
    @Transactional
    public PresencaDTO registrarPresenca(RegistrarPresencaDTO dto) {
        // Validações
        var aluno = alunoRepository.findById(dto.getAlunoId())
                .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado"));
        
        var curso = cursoRepository.findById(dto.getCursoId())
                .orElseThrow(() -> new IllegalArgumentException("Curso não encontrado"));
        
        // Verificar se já existe registro nessa data
        var presencaExistente = presencaRepository.findByAlunoIdAndCursoIdAndDataPresenca(
                dto.getAlunoId(), dto.getCursoId(), dto.getDataPresenca());
        
        if (presencaExistente.isPresent()) {
            throw new IllegalArgumentException("Já existe registro de presença para este aluno nesta data e curso");
        }
        
        // Criar nova presença
        Presenca presenca = new Presenca();
        presenca.setAlunoId(dto.getAlunoId());
        presenca.setAlunoMatricula(aluno.getMatricula());
        presenca.setAlunoNome(aluno.getNome());
        presenca.setCursoId(dto.getCursoId());
        presenca.setCursoNome(curso.getNome());
        presenca.setAlunoCurso(aluno.getNomeCurso());
        presenca.setFaculdade(curso.getFaculdade());
        presenca.setDataPresenca(dto.getDataPresenca());
        presenca.setStatus(dto.getStatus());
        presenca.setHoraEntrada(dto.getHoraEntrada());
        presenca.setHoraSaida(dto.getHoraSaida());
        presenca.setMotivoFalda(dto.getMotivoFalda());
        presenca.setJustificativa(dto.getJustificativa());
        presenca.setObservacoes(dto.getObservacoes());
        presenca.setHorasAulaComparecidas(dto.getHorasAulaComparecidas());
        presenca.setHorasAulaTotal(dto.getHorasAulaTotal());
        presenca.setProfessorId(dto.getProfessorId());
        presenca.setJustificado(false);
        
        presencaRepository.save(presenca);
        return toDTO(presenca);
    }
    
    /**
     * Confirmar presença para hoje
     */
    @Transactional
    public PresencaDTO confirmarPresencaHoje(Long alunoId, Long cursoId, StatusPresenca status) {
        return confirmarPresenca(alunoId, cursoId, LocalDate.now(), status);
    }
    
    /**
     * Confirmar presença em data específica
     */
    @Transactional
    public PresencaDTO confirmarPresenca(Long alunoId, Long cursoId, LocalDate data, StatusPresenca status) {
        var aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado"));
        
        var curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new IllegalArgumentException("Curso não encontrado"));
        
        var presenca = presencaRepository.findByAlunoIdAndCursoIdAndDataPresenca(alunoId, cursoId, data)
                .orElse(new Presenca());
        
        presenca.setAlunoId(alunoId);
        presenca.setAlunoMatricula(aluno.getMatricula());
        presenca.setAlunoNome(aluno.getNome());
        presenca.setCursoId(cursoId);
        presenca.setCursoNome(curso.getNome());
        presenca.setAlunoCurso(aluno.getNomeCurso());
        presenca.setFaculdade(curso.getFaculdade());
        presenca.setDataPresenca(data);
        presenca.setStatus(status);
        
        if (presenca.getHoraEntrada() == null) {
            presenca.setHoraEntrada(LocalDateTime.now());
        }
        
        presencaRepository.save(presenca);
        return toDTO(presenca);
    }
    
    /**
     * Registrar ausência com motivo
     */
    @Transactional
    public PresencaDTO registrarAusencia(Long alunoId, Long cursoId, LocalDate data, String motivo, boolean justificado) {
        var presenca = presencaRepository.findByAlunoIdAndCursoIdAndDataPresenca(alunoId, cursoId, data)
                .orElse(new Presenca());
        
        var aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado"));
        
        var curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new IllegalArgumentException("Curso não encontrado"));
        
        presenca.setAlunoId(alunoId);
        presenca.setAlunoMatricula(aluno.getMatricula());
        presenca.setAlunoNome(aluno.getNome());
        presenca.setCursoId(cursoId);
        presenca.setCursoNome(curso.getNome());
        presenca.setAlunoCurso(aluno.getNomeCurso());
        presenca.setFaculdade(curso.getFaculdade());
        presenca.setDataPresenca(data);
        presenca.setStatus(justificado ? StatusPresenca.FALTA_JUSTIFICADA : StatusPresenca.FALTA_NAO_JUSTIFICADA);
        presenca.setMotivoFalda(motivo);
        presenca.setJustificado(justificado);
        
        presencaRepository.save(presenca);
        return toDTO(presenca);
    }
    
    /**
     * Justificar ausência
     */
    @Transactional
    public PresencaDTO justificarAusencia(Long presencaId, String justificativa) {
        var presenca = presencaRepository.findById(presencaId)
                .orElseThrow(() -> new IllegalArgumentException("Registro de presença não encontrado"));
        
        presenca.setJustificativa(justificativa);
        presenca.setJustificado(true);
        presenca.setStatus(StatusPresenca.FALTA_JUSTIFICADA);
        
        presencaRepository.save(presenca);
        return toDTO(presenca);
    }
    
    /**
     * Obter presença por ID
     */
    public PresencaDTO obterPresenca(Long id) {
        var presenca = presencaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro de presença não encontrado"));
        return toDTO(presenca);
    }
    
    /**
     * Obter presença de um aluno em uma data
     */
    public PresencaDTO obterPresencaPorData(Long alunoId, Long cursoId, LocalDate data) {
        var presenca = presencaRepository.findByAlunoIdAndCursoIdAndDataPresenca(alunoId, cursoId, data)
                .orElseThrow(() -> new IllegalArgumentException("Nenhum registro de presença encontrado"));
        return toDTO(presenca);
    }
    
    /**
     * Listar presenças de um aluno em um período
     */
    public List<PresencaDTO> listarPresencasAluno(Long alunoId, LocalDate dataInicio, LocalDate dataFim) {
        return presencaRepository.findByAlunoIdAndPeriodo(alunoId, dataInicio, dataFim)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Listar presenças de um curso em um período
     */
    public List<PresencaDTO> listarPresencasCurso(Long cursoId, LocalDate dataInicio, LocalDate dataFim) {
        return presencaRepository.findByCursoIdAndPeriodo(cursoId, dataInicio, dataFim)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Gerar relatório de presença de um aluno em um curso
     */
    public RelatorioPresencaDTO gerarRelatorio(Long alunoId, Long cursoId, LocalDate dataInicio, LocalDate dataFim) {
        var aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado"));
        
        var curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new IllegalArgumentException("Curso não encontrado"));
        
        var presenças = presencaRepository.findByAlunoIdAndPeriodo(alunoId, dataInicio, dataFim);
        
        // Filtrar apenas pelo curso especificado
        presenças = presenças.stream()
                .filter(p -> p.getCursoId().equals(cursoId))
                .collect(Collectors.toList());
        
        int totalAulas = presenças.size();
        int presentes = (int) presenças.stream().filter(p -> p.getStatus() == StatusPresenca.PRESENTE).count();
        int ausentes = (int) presenças.stream().filter(p -> p.getStatus() == StatusPresenca.AUSENTE).count();
        int atrasados = (int) presenças.stream().filter(p -> p.getStatus() == StatusPresenca.ATRASADO).count();
        int faltasJustificadas = (int) presenças.stream().filter(p -> p.getStatus() == StatusPresenca.FALTA_JUSTIFICADA).count();
        int faltasNaoJustificadas = (int) presenças.stream().filter(p -> p.getStatus() == StatusPresenca.FALTA_NAO_JUSTIFICADA).count();
        int saidasAntecipadas = (int) presenças.stream().filter(p -> p.getStatus() == StatusPresenca.SAIDA_ANTECIPADA).count();
        
        double frequencia = totalAulas > 0 ? ((presentes + atrasados + faltasJustificadas) * 100.0) / totalAulas : 0;
        String statusFrequencia = getStatusFrequencia(frequencia);
        
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
                .statusFrequencia(statusFrequencia)
                .build();
    }
    
    /**
     * Listar faltas não justificadas de um aluno
     */
    public List<PresencaDTO> listarFaltasNaoJustificadas(Long alunoId) {
        return presencaRepository.findFaltasNaoJustificadas(alunoId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Contar faltas não justificadas em um período
     */
    public Integer contarFaltasNaoJustificadas(Long alunoId, LocalDate dataInicio, LocalDate dataFim) {
        return presencaRepository.countFaltasNaoJustificadas(alunoId, dataInicio, dataFim);
    }
    
    /**
     * Mapear Presença para DTO
     */
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
                .motivoFalda(presenca.getMotivoFalda())
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
    
    /**
     * Determinar status de frequência
     */
    private String getStatusFrequencia(double frequencia) {
        if (frequencia >= 75) {
            return "OK";
        } else if (frequencia >= 50) {
            return "AVISO";
        } else {
            return "CRÍTICO";
        }
    }
}
