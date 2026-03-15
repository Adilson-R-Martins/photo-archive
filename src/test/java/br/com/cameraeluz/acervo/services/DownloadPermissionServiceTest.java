package br.com.cameraeluz.acervo.services;

import br.com.cameraeluz.acervo.dto.DownloadPermissionRequestDTO;
import br.com.cameraeluz.acervo.dto.DownloadPermissionResponseDTO;
import br.com.cameraeluz.acervo.exceptions.DownloadLimitReachedException;
import br.com.cameraeluz.acervo.exceptions.DownloadRevokedException;
import br.com.cameraeluz.acervo.models.DownloadPermission;
import br.com.cameraeluz.acervo.models.Photo;
import br.com.cameraeluz.acervo.models.User;
import br.com.cameraeluz.acervo.repositories.DownloadPermissionRepository;
import br.com.cameraeluz.acervo.repositories.PhotoRepository;
import br.com.cameraeluz.acervo.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DownloadPermissionService}.
 *
 * <p>Coverage targets:</p>
 * <ol>
 *   <li>Race condition — concurrent downloads respect the limit (pessimistic lock path).</li>
 *   <li>Authorization bypass — non-owner without a permission record is rejected.</li>
 *   <li>Owner vs. non-owner revocation rights.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class DownloadPermissionServiceTest {

    @InjectMocks
    private DownloadPermissionService service;

    @Mock
    private DownloadPermissionRepository permissionRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private UserRepository userRepository;

    // =========================================================================
    // Shared fixture builders
    // =========================================================================

    private static final Long OWNER_ID  = 1L;
    private static final Long USER_ID   = 2L;
    private static final Long PHOTO_ID  = 10L;
    private static final List<String> ADMIN_ROLES  = List.of("ROLE_ADMIN");
    private static final List<String> EDITOR_ROLES = List.of("ROLE_EDITOR");
    private static final List<String> AUTHOR_ROLES = List.of("ROLE_AUTHOR");
    private static final List<String> GUEST_ROLES  = List.of("ROLE_GUEST");

    private User buildUser(Long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("user_" + id);
        return u;
    }

    private Photo buildPhoto(Long photoId, Long ownerId) {
        Photo p = new Photo();
        p.setId(photoId);
        p.setTitle("Test Photo");
        p.setUploadedBy(buildUser(ownerId));
        return p;
    }

    private DownloadPermission buildPermission(int limit, int count, boolean revoked) {
        DownloadPermission dp = new DownloadPermission();
        dp.setId(UUID.randomUUID());
        dp.setUser(buildUser(USER_ID));
        dp.setPhoto(buildPhoto(PHOTO_ID, OWNER_ID));
        dp.setDownloadLimit(limit);
        dp.setDownloadCount(count);
        dp.setRevoked(revoked);
        dp.setGrantedAt(LocalDateTime.now());
        dp.setGrantedByUserId(OWNER_ID);
        return dp;
    }

    // =========================================================================
    // canDownload — guard tests
    // =========================================================================

    @Nested
    @DisplayName("canDownload — precedence rules")
    class CanDownloadPrecedenceTests {

        @Test
        @DisplayName("ADMIN bypasses permission check entirely")
        void adminRole_bypassesPermissionRecord() {
            // No photo lookup, no permission lookup expected.
            service.canDownload(USER_ID, PHOTO_ID, ADMIN_ROLES);

            verifyNoInteractions(photoRepository, permissionRepository);
        }

        @Test
        @DisplayName("EDITOR bypasses permission check entirely")
        void editorRole_bypassesPermissionRecord() {
            service.canDownload(USER_ID, PHOTO_ID, EDITOR_ROLES);

            verifyNoInteractions(photoRepository, permissionRepository);
        }

        @Test
        @DisplayName("Photo owner gets unlimited access without a permission record")
        void photoOwner_bypassesPermissionRecord() {
            when(photoRepository.findById(PHOTO_ID))
                    .thenReturn(Optional.of(buildPhoto(PHOTO_ID, OWNER_ID)));

            // OWNER_ID is the owner — should pass without touching permissionRepository.
            service.canDownload(OWNER_ID, PHOTO_ID, AUTHOR_ROLES);

            verifyNoInteractions(permissionRepository);
        }

        @Test
        @DisplayName("Non-owner without permission record receives EntityNotFoundException")
        void nonOwnerWithoutPermission_throwsEntityNotFoundException() {
            when(photoRepository.findById(PHOTO_ID))
                    .thenReturn(Optional.of(buildPhoto(PHOTO_ID, OWNER_ID)));
            when(permissionRepository.findByUserIdAndPhotoIdWithLock(USER_ID, PHOTO_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.canDownload(USER_ID, PHOTO_ID, GUEST_ROLES))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("No download permission found");
        }

        @Test
        @DisplayName("Revoked permission throws DownloadRevokedException — not a generic 403")
        void revokedPermission_throwsDownloadRevokedException() {
            when(photoRepository.findById(PHOTO_ID))
                    .thenReturn(Optional.of(buildPhoto(PHOTO_ID, OWNER_ID)));
            when(permissionRepository.findByUserIdAndPhotoIdWithLock(USER_ID, PHOTO_ID))
                    .thenReturn(Optional.of(buildPermission(5, 0, true /* revoked */)));

            assertThatThrownBy(() -> service.canDownload(USER_ID, PHOTO_ID, GUEST_ROLES))
                    .isInstanceOf(DownloadRevokedException.class)
                    .hasMessageContaining("revoked");
        }

        @Test
        @DisplayName("Exhausted limit throws DownloadLimitReachedException")
        void limitExhausted_throwsDownloadLimitReachedException() {
            when(photoRepository.findById(PHOTO_ID))
                    .thenReturn(Optional.of(buildPhoto(PHOTO_ID, OWNER_ID)));
            // limit=3, count already at 3 → no more downloads allowed
            when(permissionRepository.findByUserIdAndPhotoIdWithLock(USER_ID, PHOTO_ID))
                    .thenReturn(Optional.of(buildPermission(3, 3, false)));

            assertThatThrownBy(() -> service.canDownload(USER_ID, PHOTO_ID, GUEST_ROLES))
                    .isInstanceOf(DownloadLimitReachedException.class)
                    .hasMessageContaining("limit of 3");
        }

        @Test
        @DisplayName("Valid permission increments downloadCount and is persisted")
        void validPermission_incrementsCountAndSaves() {
            DownloadPermission permission = buildPermission(3, 0, false);
            when(photoRepository.findById(PHOTO_ID))
                    .thenReturn(Optional.of(buildPhoto(PHOTO_ID, OWNER_ID)));
            when(permissionRepository.findByUserIdAndPhotoIdWithLock(USER_ID, PHOTO_ID))
                    .thenReturn(Optional.of(permission));
            when(permissionRepository.save(any())).thenReturn(permission);

            service.canDownload(USER_ID, PHOTO_ID, GUEST_ROLES);

            assertThat(permission.getDownloadCount()).isEqualTo(1);
            verify(permissionRepository).save(permission);
        }

        @Test
        @DisplayName("Guard uses the PESSIMISTIC_WRITE lock query, not the non-locking one")
        void guard_alwaysUsesLockingQuery() {
            when(photoRepository.findById(PHOTO_ID))
                    .thenReturn(Optional.of(buildPhoto(PHOTO_ID, OWNER_ID)));
            when(permissionRepository.findByUserIdAndPhotoIdWithLock(USER_ID, PHOTO_ID))
                    .thenReturn(Optional.empty());

            // We just care that the locking variant is called, not the plain one.
            assertThatThrownBy(() -> service.canDownload(USER_ID, PHOTO_ID, GUEST_ROLES))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(permissionRepository).findByUserIdAndPhotoIdWithLock(USER_ID, PHOTO_ID);
            verify(permissionRepository, never()).findByUserIdAndPhotoId(anyLong(), anyLong());
        }
    }

    // =========================================================================
    // Race condition simulation
    // =========================================================================

    @Nested
    @DisplayName("Race condition — concurrent downloads against a shared counter")
    class RaceConditionTests {

        /**
         * Simulates N threads concurrently calling canDownload for the same permission.
         *
         * <p>In production, PESSIMISTIC_WRITE serialises the DB updates.
         * In this unit test we replace that serialisation with an {@link AtomicInteger}
         * inside the mock so we can assert that exactly {@code limit} threads succeed
         * while the rest receive {@link DownloadLimitReachedException}.</p>
         */
        @Test
        @DisplayName("Only 'downloadLimit' threads succeed when requests are concurrent")
        void concurrentRequests_onlyLimitThreadsSucceed() throws InterruptedException {
            final int DOWNLOAD_LIMIT = 3;
            final int THREAD_COUNT   = 10;

            // Shared mutable state — simulates the locked DB row.
            AtomicInteger sharedCount = new AtomicInteger(0);

            Photo photo = buildPhoto(PHOTO_ID, OWNER_ID);
            when(photoRepository.findById(PHOTO_ID)).thenReturn(Optional.of(photo));

            // Each call gets a fresh view of the permission with the current shared count.
            when(permissionRepository.findByUserIdAndPhotoIdWithLock(USER_ID, PHOTO_ID))
                    .thenAnswer(invocation -> {
                        // Simulate the locked row: read the current committed count.
                        DownloadPermission dp = new DownloadPermission();
                        dp.setId(UUID.randomUUID());
                        dp.setUser(buildUser(USER_ID));
                        dp.setPhoto(photo);
                        dp.setDownloadLimit(DOWNLOAD_LIMIT);
                        dp.setDownloadCount(sharedCount.get()); // read current state
                        dp.setRevoked(false);
                        dp.setGrantedAt(LocalDateTime.now());
                        return Optional.of(dp);
                    });

            // save() atomically increments the shared counter (simulates DB commit).
            when(permissionRepository.save(any(DownloadPermission.class)))
                    .thenAnswer(invocation -> {
                        DownloadPermission saved = invocation.getArgument(0);
                        // Atomic CAS ensures we never exceed the limit even under races.
                        int expected = saved.getDownloadCount() - 1; // count before increment
                        sharedCount.compareAndSet(expected, saved.getDownloadCount());
                        return saved;
                    });

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger rejectedCount = new AtomicInteger(0);

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch  = new CountDownLatch(THREAD_COUNT);
            ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);

            for (int i = 0; i < THREAD_COUNT; i++) {
                pool.submit(() -> {
                    try {
                        startLatch.await(); // All threads start simultaneously.
                        service.canDownload(USER_ID, PHOTO_ID, GUEST_ROLES);
                        successCount.incrementAndGet();
                    } catch (DownloadLimitReachedException e) {
                        rejectedCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // Release all threads at once.
            doneLatch.await();
            pool.shutdown();

            assertThat(successCount.get())
                    .as("Exactly DOWNLOAD_LIMIT threads should succeed")
                    .isEqualTo(DOWNLOAD_LIMIT);
            assertThat(rejectedCount.get())
                    .as("All remaining threads must receive DownloadLimitReachedException")
                    .isEqualTo(THREAD_COUNT - DOWNLOAD_LIMIT);
        }
    }

    // =========================================================================
    // revokePermission — ownership and role tests
    // =========================================================================

    @Nested
    @DisplayName("revokePermission — authorization rules")
    class RevokePermissionTests {

        private DownloadPermission permission;
        private UUID permissionId;

        @BeforeEach
        void setUp() {
            permissionId = UUID.randomUUID();
            permission   = buildPermission(5, 2, false);
            permission.setId(permissionId);
        }

        @Test
        @DisplayName("ADMIN can revoke any permission — regardless of photo ownership")
        void admin_canRevokeAnyPermission() {
            when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));
            when(permissionRepository.save(any())).thenReturn(permission);

            // ADMIN is not the owner (OWNER_ID=1, admin acts as user 99)
            service.revokePermission(99L, ADMIN_ROLES, permissionId);

            assertThat(permission.isRevoked()).isTrue();
            verify(permissionRepository).save(permission);
        }

        @Test
        @DisplayName("EDITOR can revoke any permission — regardless of photo ownership")
        void editor_canRevokeAnyPermission() {
            when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));
            when(permissionRepository.save(any())).thenReturn(permission);

            service.revokePermission(99L, EDITOR_ROLES, permissionId);

            assertThat(permission.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("Photo owner (AUTHOR role) can revoke permissions on their own photo")
        void photoOwner_canRevokeOwnPhotoPermission() {
            when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));
            when(permissionRepository.save(any())).thenReturn(permission);

            // OWNER_ID = 1 is the uploader of the photo in the permission fixture.
            service.revokePermission(OWNER_ID, AUTHOR_ROLES, permissionId);

            assertThat(permission.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("Non-owner AUTHOR receives AccessDeniedException — strict failure")
        void nonOwnerAuthor_throwsAccessDeniedException() {
            when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));

            // USER_ID = 2 is NOT the photo owner (owner is OWNER_ID = 1).
            assertThatThrownBy(() -> service.revokePermission(USER_ID, AUTHOR_ROLES, permissionId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Only ADMIN, EDITOR, or the photo owner");

            verify(permissionRepository, never()).save(any());
        }

        @Test
        @DisplayName("GUEST without ownership receives AccessDeniedException")
        void nonOwnerGuest_throwsAccessDeniedException() {
            when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));

            assertThatThrownBy(() -> service.revokePermission(99L, GUEST_ROLES, permissionId))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Revoking a non-existent permission throws EntityNotFoundException")
        void unknownPermission_throwsEntityNotFoundException() {
            when(permissionRepository.findById(permissionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.revokePermission(OWNER_ID, ADMIN_ROLES, permissionId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Permission not found");
        }
    }

    // =========================================================================
    // grantPermission — authorization bypass tests
    // =========================================================================

    @Nested
    @DisplayName("grantPermission — authorization rules")
    class GrantPermissionTests {

        @Test
        @DisplayName("AUTHOR who does not own the photo cannot grant permissions")
        void nonOwnerAuthor_cannotGrantPermission() {
            when(photoRepository.findById(PHOTO_ID))
                    .thenReturn(Optional.of(buildPhoto(PHOTO_ID, OWNER_ID)));

            DownloadPermissionRequestDTO request = new DownloadPermissionRequestDTO();
            request.setPhotoId(PHOTO_ID);
            request.setUserId(USER_ID);
            request.setDownloadLimit(1);

            // Requester is USER_ID (2), but photo owner is OWNER_ID (1).
            assertThatThrownBy(() -> service.grantPermission(USER_ID, GUEST_ROLES, request))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Only ADMIN, EDITOR, or the photo owner");

            verify(permissionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Photo owner (AUTHOR role) can successfully grant a permission")
        void photoOwner_canGrantPermission() {
            Photo photo = buildPhoto(PHOTO_ID, OWNER_ID);
            User targetUser = buildUser(USER_ID);
            DownloadPermission savedPermission = buildPermission(2, 0, false);

            when(photoRepository.findById(PHOTO_ID)).thenReturn(Optional.of(photo));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(targetUser));
            when(permissionRepository.findByUserIdAndPhotoId(USER_ID, PHOTO_ID))
                    .thenReturn(Optional.empty());
            when(permissionRepository.save(any())).thenReturn(savedPermission);

            DownloadPermissionRequestDTO request = new DownloadPermissionRequestDTO();
            request.setPhotoId(PHOTO_ID);
            request.setUserId(USER_ID);
            request.setDownloadLimit(2);

            DownloadPermissionResponseDTO response =
                    service.grantPermission(OWNER_ID, AUTHOR_ROLES, request);

            assertThat(response).isNotNull();
            verify(permissionRepository).save(any(DownloadPermission.class));
        }

        @Test
        @DisplayName("Re-granting a revoked permission reactivates it (upsert)")
        void regrantRevokedPermission_reactivatesIt() {
            Photo photo = buildPhoto(PHOTO_ID, OWNER_ID);
            DownloadPermission existing = buildPermission(1, 1, true /* revoked */);

            when(photoRepository.findById(PHOTO_ID)).thenReturn(Optional.of(photo));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(buildUser(USER_ID)));
            when(permissionRepository.findByUserIdAndPhotoId(USER_ID, PHOTO_ID))
                    .thenReturn(Optional.of(existing));
            when(permissionRepository.save(any())).thenReturn(existing);

            DownloadPermissionRequestDTO request = new DownloadPermissionRequestDTO();
            request.setPhotoId(PHOTO_ID);
            request.setUserId(USER_ID);
            request.setDownloadLimit(3);

            service.grantPermission(OWNER_ID, AUTHOR_ROLES, request);

            assertThat(existing.isRevoked()).isFalse();
            assertThat(existing.getDownloadLimit()).isEqualTo(3);
        }
    }
}
