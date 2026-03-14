package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.models.ResultType;
import br.com.cameraeluz.acervo.repositories.ResultTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/results")
public class ResultTypeController {

    @Autowired
    private ResultTypeRepository resultTypeRepository;

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('EDITOR')")
    public List<ResultType> getAllResultTypes() {
        return resultTypeRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResultType> createResultType(@RequestBody ResultType resultType) {
        return ResponseEntity.ok(resultTypeRepository.save(resultType));
    }
}