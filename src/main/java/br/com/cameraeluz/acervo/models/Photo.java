package br.com.cameraeluz.acervo.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import static jakarta.persistence.FetchType.LAZY;

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
    // FetchType.LAZY: prevents the associated User from being loaded on every Photo query.
    // The uploader is only needed for ownership checks and is rarely required in list views.
    @ManyToOne(fetch = FetchType.LAZY)
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

    @Column(name = "original_file_name")
    private String originalFileName;

    /** Relative path to the original high-resolution file within the upload directory. */
    @Column(name = "storage_path")
    private String storagePath;

    /** Relative path to the web-optimised (max 1280×1280, q=0.8) version of the file. */
    @Column(name = "web_optimized_path")
    private String webOptimizedPath;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Whether this photo is active. Inactive photos are soft-deleted and hidden from all queries. */
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    /**
     * Automatically sets the creation timestamp before saving to database.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}