package br.com.cameraeluz.acervo.services;

import br.com.cameraeluz.acervo.models.Category;
import br.com.cameraeluz.acervo.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Category create(Category category) {
        if (categoryRepository.findByName(category.getName()).isPresent()) {
            throw new IllegalArgumentException("Erro: Categoria já existe!");
        }
        return categoryRepository.save(category);
    }
}