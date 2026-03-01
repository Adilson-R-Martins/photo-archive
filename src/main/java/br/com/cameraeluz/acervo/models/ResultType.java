package br.com.cameraeluz.acervo.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing the possible results a photo can achieve in an event.
 * Examples: 1st Place, 2nd Place, Acceptance, Honorable Mention.
 * This is managed by the administrator.
 */
@Entity
@Table(name = "result_types")
@Data
@NoArgsConstructor
public class ResultType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String description;
}