package br.com.cameraeluz.acervo.services;

import br.com.cameraeluz.acervo.models.Category;
import br.com.cameraeluz.acervo.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing photo categories.
 *
 * <p>Provides retrieval of all existing categories and creation of new ones,
 * enforcing uniqueness by category name.</p>
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Returns all categories in the system.
     *
     * @return a list of all {@link Category} entities; empty if none exist.
     */
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    /**
     * Creates a new category, rejecting duplicates by name.
     *
     * @param category the category to persist.
     * @return the saved {@link Category} entity.
     * @throws IllegalArgumentException if a category with the same name already exists.
     */
    public Category create(Category category) {
        if (categoryRepository.findByName(category.getName()).isPresent()) {
            throw new IllegalArgumentException(
                    "Category '" + category.getName() + "' already exists. Use a unique category name.");
        }
        return categoryRepository.save(category);
    }
}
