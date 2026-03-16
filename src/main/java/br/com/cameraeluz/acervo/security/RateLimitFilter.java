package br.com.cameraeluz.acervo.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Servlet filter that enforces per-IP token-bucket rate limits on authentication endpoints.
 *
 * <p>Two endpoints are protected:</p>
 * <ul>
 *   <li>{@code POST /api/auth/signin} — {@value #MAX_LOGIN_PER_MINUTE} requests per minute.
 *       Prevents credential-stuffing and brute-force attacks.</li>
 *   <li>{@code POST /api/auth/signup} — {@value #MAX_SIGNUP_PER_MINUTE} requests per minute.
 *       Prevents mass account creation and BCrypt resource exhaustion: password hashing is
 *       deliberately CPU-intensive, so bulk registrations can cause denial-of-service without
 *       this guard.</li>
 * </ul>
 *
 * <p>Each (IP, endpoint) pair has its own independent token bucket stored under the compound
 * cache key {@code "ip|path"}, so a burst of login attempts does not deplete the registration
 * quota and vice versa.</p>
 *
 * <p>Client IP resolution prefers the {@code X-Forwarded-For} header (first entry) so that
 * deployments behind a reverse proxy use the real client address. Ensure only your trusted
 * proxy populates this header in production; otherwise clients can spoof it to bypass limiting.</p>
 *
 * <p>Bucket entries are automatically evicted from memory after one hour of inactivity,
 * preventing unbounded heap growth under sustained enumeration attempts.</p>
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH  = "/api/auth/signin";
    private static final String SIGNUP_PATH = "/api/auth/signup";

    /** Maximum sign-in attempts allowed per IP per minute. */
    private static final int MAX_LOGIN_PER_MINUTE  = 10;

    /**
     * Maximum account-creation attempts allowed per IP per minute.
     *
     * <p>Lower than the login limit because BCrypt hashing is intentionally slow,
     * making bulk registration a vector for CPU exhaustion.</p>
     */
    private static final int MAX_SIGNUP_PER_MINUTE = 5;

    /**
     * Single cache for all buckets. The compound key {@code "ip|path"} gives each
     * (IP, endpoint) pair its own independent bucket with the correct per-endpoint limit.
     */
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            String uri = request.getRequestURI();
            if (LOGIN_PATH.equals(uri) || SIGNUP_PATH.equals(uri)) {
                String ip = resolveClientIp(request);
                Bucket bucket = buckets.get(ip + "|" + uri, key -> newBucketFor(uri));
                if (!bucket.tryConsume(1)) {
                    rejectWithTooManyRequests(response);
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Creates a token bucket calibrated for the given endpoint path.
     * Signup buckets are deliberately smaller than login buckets.
     *
     * @param path the request URI, used to select the appropriate capacity.
     * @return a new {@link Bucket} with the correct rate limit for the endpoint.
     */
    private Bucket newBucketFor(String path) {
        int capacity = SIGNUP_PATH.equals(path) ? MAX_SIGNUP_PER_MINUTE : MAX_LOGIN_PER_MINUTE;
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillIntervally(capacity, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private void rejectWithTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\","
                + "\"message\":\"Too many requests. Please wait before trying again.\"}");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
