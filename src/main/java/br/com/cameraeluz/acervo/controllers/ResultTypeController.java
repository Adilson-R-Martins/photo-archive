package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.models.ResultType;
import br.com.cameraeluz.acervo.services.ResultTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
public class ResultTypeController {

    private final ResultTypeService resultTypeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('GUEST', 'AUTHOR', 'EDITOR', 'ADMIN')")
    public List<ResultType> getAllResultTypes() {
        return resultTypeService.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResultType> createResultType(@RequestBody ResultType resultType) {
        return ResponseEntity.ok(resultTypeService.create(resultType));
    }
}