package com.transporte.escolar.controller;

import com.transporte.escolar.service.ElectionService; // <-- IMPORT NECESSÁRIO
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/eleicao")
public class ElectionController {
    private final ElectionService service;

    public ElectionController(ElectionService service) {
        this.service = service;
    }

    @GetMapping("/iniciar")
    public String iniciar() {
        service.iniciarEleicao();
        return "Eleição realizada. Coordenador: " + service.getCoordinator();
    }
}
