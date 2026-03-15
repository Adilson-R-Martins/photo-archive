package br.com.cameraeluz.acervo.models;

import br.com.cameraeluz.acervo.models.enums.EventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;

/**
 * Represents a photography event, such as a contest or exhibition.
 */
@BatchSize(size = 10)
@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /**
     * Type of event (Contest or Exhibition).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    private String complement;
    private String affiliation;
    private String category;

    // Geographic info as plain strings, as per architectural decision
    private String city;
    private String state;
    private String country;

    /**
     * The official date of the event.
     * Allows filtering by year or specific month.
     */
    @Column(name = "event_date")
    private LocalDate eventDate;
}