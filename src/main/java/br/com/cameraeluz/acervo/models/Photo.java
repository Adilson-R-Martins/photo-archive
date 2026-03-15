package br.com.cameraeluz.acervo.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Represents a photographic work in the collection.
 * This is the central entity of the system, containing technical metadata,
 * authorship information, and file storage references.
 */
@Entity
@Table(name = "photos")
@Getter
@Setter
@NoArgsConstructor
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    /**
     * The artistic name assigned by the photographer for this specific work.
     */
    @Column(nullable = false, name = "artistic_author_name")
    private String artisticAuthorName;

    /**
     * Reference to the system user (Author) who performed the upload.
     */
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User uploadedBy;

    /**
     * List of photographic genres/categories assigned to this image.
     */
    @ManyToMany
    @JoinTable(
            name = "photo_categories",
            joinColumns = @JoinColumn(name = "photo_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @BatchSize(size = 20)
    private Set<Category> categories;

    @JsonIgnore
    @OneToMany(mappedBy = "photo")
    @BatchSize(size = 20)
    private Set<PhotoEventTrack> eventTracks;

    @Embedded
    private ExifData exifData;

    // File Management Fields
    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "storage_path")
    private String storagePath; // Local or Cloud path for the original file

    @Column(name = "web_optimized_path")
    private String webOptimizedPath; // Path for the version resized for browser view

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean active = true; // Define como true por padrão para novas fotos

    /**
     * Automatically sets the creation timestamp before saving to database.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}