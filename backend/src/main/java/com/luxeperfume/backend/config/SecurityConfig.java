package com.luxeperfume.backend.config;

import com.luxeperfume.backend.auth.JwtAuthenticationFilter;
import com.luxeperfume.backend.auth.OAuth2LoginSuccessHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
            ObjectProvider<ClientRegistrationRepository> registrations,
            ObjectProvider<OAuth2LoginSuccessHandler> oauthSuccessHandler,
            @Value("${app.security.oauth-success-redirect}") String oauthRedirect) throws Exception {
        boolean googleOAuthEnabled = registrations.getIfAvailable() != null;
        SessionCreationPolicy sessionPolicy = googleOAuthEnabled
                ? SessionCreationPolicy.IF_REQUIRED : SessionCreationPolicy.STATELESS;
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
            .sessionManagement(session -> session.sessionCreationPolicy(sessionPolicy))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/api/v1/health", "/actuator/health", "/actuator/health/**",
                    "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                    "/api/v1/auth/csrf", "/api/v1/auth/register", "/api/v1/auth/login",
                    "/api/v1/auth/refresh", "/api/v1/auth/logout", "/api/v1/auth/forgot-password",
                    "/api/v1/auth/reset-password", "/oauth2/**", "/login/oauth2/**",
                    "/api/v1/catalog/**", "/api/v1/contact", "/api/v1/newsletter/**"
                ).permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/staff/catalog/products/**").hasAuthority("PERM_products.write")
                .requestMatchers("/api/v1/staff/catalog/variants/*/inventory").hasAuthority("PERM_inventory.write")
                .requestMatchers("/api/v1/staff/catalog/variants/**").hasAuthority("PERM_products.write")
                .requestMatchers("/api/v1/staff/catalog/images/**").hasAuthority("PERM_products.write")
                .requestMatchers("/api/v1/staff/catalog/cloudinary/**").hasAuthority("PERM_products.write")
                .requestMatchers("/api/v1/staff/catalog/categories/**", "/api/v1/staff/catalog/brands/**", "/api/v1/staff/catalog/collections/**").hasAuthority("PERM_catalog.write")
                .requestMatchers("/api/v1/staff/catalog/**").hasAnyAuthority("PERM_products.write", "PERM_catalog.write")
                .requestMatchers("/api/v1/staff/products/**").hasAuthority("PERM_products.write")
                .requestMatchers(HttpMethod.GET, "/api/v1/staff/orders", "/api/v1/staff/orders/**").hasAuthority("PERM_orders.read")
                .requestMatchers("/api/v1/staff/orders/**").hasAuthority("PERM_orders.write")
                .requestMatchers(HttpMethod.GET, "/api/v1/staff/inventory/**").hasAuthority("PERM_inventory.read")
                .requestMatchers("/api/v1/staff/inventory/**").hasAuthority("PERM_inventory.write")
                .requestMatchers("/api/v1/staff/payments/**").hasAuthority("PERM_payments.read")
                .requestMatchers("/api/v1/staff/**").hasAnyRole("ADMIN", "MANAGER", "INVENTORY_STAFF")
                .anyRequest().authenticated())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable())
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) -> response.sendError(HttpStatus.UNAUTHORIZED.value()))
                .accessDeniedHandler((request, response, exception) -> response.sendError(HttpStatus.FORBIDDEN.value())))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        if (googleOAuthEnabled) {
            http.oauth2Login(oauth -> oauth
                    .successHandler(oauthSuccessHandler.getIfAvailable())
                    .failureHandler((request, response, exception) ->
                            response.sendRedirect(oauthRedirect + "?error=google_auth_failed")));
        }
        return http.build();
    }
}
