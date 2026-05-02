package com.transportation.presenca.service;

import com.transportation.presenca.dto.PresencaDTO;
import com.transportation.presenca.model.Presenca;
import com.transportation.presenca.model.StatusPresenca;
import com.transportation.presenca.repository.AlunoRepository;
import com.transportation.presenca.repository.PresencaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PresencaServiceTest {
    
    @Mock
    private PresencaRepository presencaRepository;
    
    @Mock
    private AlunoRepository alunoRepository;
    
    @InjectMocks
    private PresencaService presencaService;
    
    @BeforeEach
    public void setUp() {
        // Inicialização se necessário
    }
    
    @Test
    public void testObterPresenca() {
        // Arrange
        Presenca presenca = new Presenca();
        presenca.setId(1L);
        presenca.setAlunoId(1L); 
        presenca.setStatus(StatusPresenca.PRESENTE);
        
        when(presencaRepository.findById(1L)).thenReturn(Optional.of(presenca));
        
        // Act
        PresencaDTO dto = presencaService.obterPresenca(1L);
        
        // Assert
        assertEquals(1L, dto.getId());
        assertEquals(StatusPresenca.PRESENTE, dto.getStatus());
    }
    
    @Test
    public void testObterPresencaNotFound() {
        // Arrange
        when(presencaRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            presencaService.obterPresenca(999L);
        });
    }
}
