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
 * Servlet filter that enforces a per-IP token-bucket rate limit on the sign-in endpoint.
 *
 * <p>Each unique client IP is allowed {@value #MAX_REQUESTS_PER_MINUTE} login attempts per minute.
 * Excess requests receive {@code 429 Too Many Requests} without reaching the authentication layer,
 * preventing credential-stuffing and brute-force attacks.</p>
 *
 * <p>Client IP resolution prefers the {@code X-Forwarded-For} header (first entry) so that
 * deployments behind a reverse proxy use the real client address. Ensure only your trusted
 * proxy populates this header in production; otherwise clients can spoof it to bypass limiting.</p>
 *
 * <p>Bucket entries are automatically evicted from memory after one hour of inactivity,
 * preventing unbounded heap growth under sustained enumeration attempts.</p>
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/signin";
    private static final int MAX_REQUESTS_PER_MINUTE = 10;

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(request.getMethod()) && LOGIN_PATH.equals(request.getRequestURI())) {
            String ip = resolveClientIp(request);
            Bucket bucket = buckets.get(ip, key -> newBucket());

            if (!bucket.tryConsume(1)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                        "{\"status\":429,\"error\":\"Too Many Requests\","
                        + "\"message\":\"Too many login attempts. Please wait before trying again.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(MAX_REQUESTS_PER_MINUTE)
                        .refillIntervally(MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
