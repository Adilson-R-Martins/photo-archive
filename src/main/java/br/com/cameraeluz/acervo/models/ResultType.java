package br.com.cameraeluz.acervo.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

/**
 * Entity representing the possible results a photo can achieve in an event.
 * Examples: 1st Place, 2nd Place, Acceptance, Honorable Mention.
 * This is managed by the administrator.
 */
@BatchSize(size = 10)
@Entity
@Table(name = "result_types")
@Getter
@Setter
@NoArgsConstructor
public class ResultType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String description;
}