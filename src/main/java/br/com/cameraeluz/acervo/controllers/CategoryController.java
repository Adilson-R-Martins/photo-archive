package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.models.Category;
import br.com.cameraeluz.acervo.repositories.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Category management.
 * Implements Role-Based Access Control (RBAC) to secure sensitive operations.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    CategoryRepository categoryRepository;

    /**
     * Retrieves all registered categories.
     * Access: Permitted for any authenticated user.
     *
     * @return List of Category entities
     */
    @GetMapping
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Creates a new category in the system.
     * Access: Restricted to users with ROLE_ADMIN privileges.
     *
     * @param category The category object to be persisted
     * @return ResponseEntity with the created category or error message
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // APENAS Administradores podem criar
    public ResponseEntity<?> createCategory(@RequestBody Category category) {
        if (categoryRepository.findByName(category.getName()).isPresent()) {
            return ResponseEntity.badRequest().body("Erro: Categoria já existe!");
        }
        return ResponseEntity.ok(categoryRepository.save(category));
    }
}