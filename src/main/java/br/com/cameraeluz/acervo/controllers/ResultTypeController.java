package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.models.ResultType;
import br.com.cameraeluz.acervo.services.ResultTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing award/result types.
 *
 * <p>Uses the declared role hierarchy (ADMIN &gt; EDITOR &gt; AUTHOR &gt; GUEST),
 * so {@code hasRole('GUEST')} matches all authenticated roles.</p>
 */
@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
public class ResultTypeController {

    private final ResultTypeService resultTypeService;

    /** Returns all result types. Accessible to any authenticated user. */
    @GetMapping
    @PreAuthorize("hasRole('GUEST')")
    public List<ResultType> getAllResultTypes() {
        return resultTypeService.findAll();
    }

    /** Creates a new result type. Restricted to ADMIN. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResultType> createResultType(@RequestBody ResultType resultType) {
        return ResponseEntity.ok(resultTypeService.create(resultType));
    }
}