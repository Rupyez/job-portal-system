package com.jobportal.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailService userDetailService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        //Continue normally when no Bearer token is supplied.
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);
        try{
            //only access tokens should authenticate API request
            if(!jwtProvider.isTokenValid(token, JwtProvider.TokenType.ACCESS)){
                filterChain.doFilter(request, response);
            }


            //Do not overwrite authentication already established
            if(SecurityContextHolder.getContext().getAuthentication() == null){
                String email = jwtProvider.extractSubject(token);

                UserDetails userDetails = userDetailService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }catch (JwtException | IllegalArgumentException ex){

            //invalid or expired token: do not authenticate this request,
            SecurityContextHolder.clearContext();
        }catch (org.springframework.security.core.userdetails.UsernameNotFoundException ex){
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
        //Do not overwrite authentication already established
    }
}
