package br.com.cameraeluz.acervo.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a photography event, such as a contest or exhibition.
 */
@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /**
     * Type of event (e.g., Contest, Exhibition).
     */
    @Column(nullable = false)
    private String type;

    private String complement;
    private String affiliation;
    private String category;

    // Geographic info as plain strings, as per architectural decision
    private String city;
    private String state;
    private String country;
}