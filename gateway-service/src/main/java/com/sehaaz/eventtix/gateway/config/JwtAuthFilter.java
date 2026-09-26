package com.sehaaz.eventtix.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.Optional;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";
    public static final String USER_EMAIL_HEADER = "X-User-Email";

    private static final String BEARER_PREFIX = "Bearer ";
    private static final PathPattern AUTH_PATHS = PathPatternParser.defaultInstance.parse("/api/auth/**");
    private static final PathPattern EVENT_PATHS = PathPatternParser.defaultInstance.parse("/api/events/**");

    private final JwtVerifier verifier;

    public JwtAuthFilter(@Value("${jwt.secret}") String secret, ObjectMapper mapper) {
        this.verifier = new JwtVerifier(secret, mapper, Clock.systemUTC());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // İstemcinin gönderdiği kimlik header'larına asla güvenme.
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove(USER_ID_HEADER);
                    h.remove(USER_ROLE_HEADER);
                    h.remove(USER_EMAIL_HEADER);
                })
                .build();
        PathContainer path = request.getPath().pathWithinApplication();
        boolean isGet = HttpMethod.GET.equals(request.getMethod());

        if (AUTH_PATHS.matches(path) || (isGet && EVENT_PATHS.matches(path))) {
            return chain.filter(exchange.mutate().request(request).build());
        }

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        Optional<JwtVerifier.Claims> claims = authorization != null && authorization.startsWith(BEARER_PREFIX)
                ? verifier.verify(authorization.substring(BEARER_PREFIX.length()))
                : Optional.empty();
        if (claims.isEmpty()) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }
        if (EVENT_PATHS.matches(path) && !"ADMIN".equals(claims.get().role())) {
            return reject(exchange, HttpStatus.FORBIDDEN);
        }

        ServerHttpRequest authenticated = request.mutate()
                .header(USER_ID_HEADER, claims.get().userId())
                .header(USER_ROLE_HEADER, claims.get().role())
                .header(USER_EMAIL_HEADER, claims.get().email())
                .build();
        return chain.filter(exchange.mutate().request(authenticated).build());
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
