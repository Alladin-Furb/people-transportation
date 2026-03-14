package com.transporte.escolar.service;

import org.springframework.stereotype.Service;
import java.util.List; // <-- IMPORT NECESSÁRIO

@Service
public class ElectionService {

    private int processId = 2;
    private int coordinator = -1;
    private List<Integer> processos = List.of(1, 2, 3, 4);

    public void iniciarEleicao() {
        int maior = processId;
        for (int id : processos) {
            if (id > maior) {
                maior = id;
            }
        }
        coordinator = maior;
        System.out.println("Novo coordenador: " + coordinator);
    }

    public int getCoordinator() {
        return coordinator;
    }
}
