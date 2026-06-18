package org.example.expert.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.user.enums.UserRole;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String url = request.getRequestURI();
		return url.startsWith("/auth");
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String bearerJwt = request.getHeader("Authorization");

		if (!StringUtils.hasText(bearerJwt)) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			String jwt = jwtUtil.substringToken(bearerJwt);
			Claims claims = jwtUtil.extractClaims(jwt);

			Long userId = Long.parseLong(claims.getSubject());
			String email = claims.get("email", String.class);
			UserRole userRole = UserRole.valueOf(claims.get("userRole", String.class));

			AuthUser authUser = new AuthUser(userId, email, userRole);

			UsernamePasswordAuthenticationToken authentication =
				new UsernamePasswordAuthenticationToken(
					authUser,
					null,
					List.of(new SimpleGrantedAuthority("ROLE_" + userRole.name()))
				);

			SecurityContextHolder.getContext().setAuthentication(authentication);

			filterChain.doFilter(request, response);
		} catch (SecurityException | MalformedJwtException e) {
			SecurityContextHolder.clearContext();
			log.error("Invalid JWT signature, 유효하지 않는 JWT 서명 입니다.", e);
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않는 JWT 서명입니다.");
		} catch (ExpiredJwtException e) {
			SecurityContextHolder.clearContext();
			log.error("Expired JWT token, 만료된 JWT token 입니다.", e);
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "만료된 JWT 토큰입니다.");
		} catch (UnsupportedJwtException e) {
			SecurityContextHolder.clearContext();
			log.error("Unsupported JWT token, 지원되지 않는 JWT 토큰 입니다.", e);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "지원되지 않는 JWT 토큰입니다.");
		} catch (Exception e) {
			SecurityContextHolder.clearContext();
			log.error("Internal server error", e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
}
