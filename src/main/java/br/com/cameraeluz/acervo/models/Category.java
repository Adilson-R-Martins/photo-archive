package br.com.cameraeluz.acervo.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a taxonomic category for organizing photographs within the archive.
 * Examples include "Nature", "Portrait", or "Architecture".
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}