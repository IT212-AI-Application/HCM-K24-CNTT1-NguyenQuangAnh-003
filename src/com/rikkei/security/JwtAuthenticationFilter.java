package com.rikkei.security;
 
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
 
public class JwtAuthenticationFilter extends OncePerRequestFilter {
 
    private final String SECRET_KEY = "rikkei_secret_key_super_secure_do_not_share";
    private final JwtAuthenticationEntryPoint entryPoint;

    public JwtAuthenticationFilter(JwtAuthenticationEntryPoint entryPoint) {
        this.entryPoint = entryPoint;
    }
 
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
            
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
                String username = Jwts.parser()
                        .setSigningKey(SECRET_KEY)
                        .parseClaimsJws(token)
                        .getBody()
                        .getSubject();
                        
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // Logic set Authentication vào SecurityContext (đã rút gọn)
                }
            } catch (SignatureException e) {
                // Catch signature error and delegate to entryPoint
                entryPoint.commence(request, response, new JwtAuthenticationException("JWT signature does not match locally computed signature.", e));
                return; // Stop the filter chain execution on authentication failure
            } catch (ExpiredJwtException e) {
                entryPoint.commence(request, response, new JwtAuthenticationException("JWT token has expired.", e));
                return;
            } catch (MalformedJwtException e) {
                entryPoint.commence(request, response, new JwtAuthenticationException("JWT token is malformed.", e));
                return;
            } catch (Exception e) {
                entryPoint.commence(request, response, new JwtAuthenticationException("Authentication failed: " + e.getMessage(), e));
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
