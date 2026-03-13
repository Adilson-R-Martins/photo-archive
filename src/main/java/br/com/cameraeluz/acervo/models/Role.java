package br.com.cameraeluz.acervo.models;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing the access roles within the system.
 * Defined roles: ADMIN, EDITOR, AUTHOR, GUEST.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;
}