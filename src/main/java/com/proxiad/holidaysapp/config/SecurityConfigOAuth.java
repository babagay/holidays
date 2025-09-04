package com.proxiad.holidaysapp.config;

import com.proxiad.holidaysapp.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth2 authentication config - use github as Auth server to log in into App
 * OAuth2 Login + Session Cookie (Spring Security дефолт).
 *
 * Если фронтенд на React, и все запросы идут через AJAX/Fetch, CSRF обычно не нужен для API.
 * Альтернатива: Spring Security выдаёт CSRF-токен на первую загрузку страницы (X-CSRF-TOKEN в cookie/hidden field).
 * Фронтенд должен добавлять его в заголовок X-CSRF-TOKEN при всех изменяющих запросах.
 *
 * (!) Для реализации аутентификации без состояния (JWT токен)
 * нужно дописать слой, который после GitHub логина выдает токен и переводит систему в token-based auth.
 * В простом варианте одно и то же приложение выступает как в роли Authorization Server (генерирует токен),
 * так и в роли Resource Server (получает JWT, получает JWK (публичный ключ), Проверяет подпись JWT, валидирует GitHub access token, отдает токен на UI)
 */
@Configuration
@EnableWebSecurity
@Profile("main")
@RequiredArgsConstructor
public class SecurityConfigOAuth {

    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/login**", "/error**", "/h2/**").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable) // полностью отключаем CSRF
//                .csrf(csrf -> csrf
//                        .ignoringRequestMatchers("/h2/**") // отключаем CSRF для H2
//                )
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin) // разрешаем фреймы для H2
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler((request, response, authentication) -> {
                            response.sendRedirect("/");
                        })
                )
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .logout(logout -> logout
                        .logoutSuccessUrl("/").permitAll()
                );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
