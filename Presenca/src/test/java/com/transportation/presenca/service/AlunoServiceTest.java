package com.transportation.presenca.service;

import com.transportation.presenca.dto.AlunoDTO;
import com.transportation.presenca.model.Aluno;
import com.transportation.presenca.repository.AlunoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AlunoServiceTest {
    
    @Mock
    private AlunoRepository alunoRepository;
    
    @InjectMocks
    private AlunoService alunoService;
    
    @Test
    public void testObterAluno() {
        // Arrange
        Aluno aluno = new Aluno();
        aluno.setId(1L);
        aluno.setMatricula("2024001");
        aluno.setNome("João Silva");
        
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));
        
        // Act
        AlunoDTO dto = alunoService.obterAluno(1L);
        
        // Assert
        assertEquals(1L, dto.getId());
        assertEquals("2024001", dto.getMatricula());
        assertEquals("João Silva", dto.getNome());
    }
    
    @Test
    public void testObterAlunoNotFound() {
        // Arrange
        when(alunoRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            alunoService.obterAluno(999L);
        });
    }
}
