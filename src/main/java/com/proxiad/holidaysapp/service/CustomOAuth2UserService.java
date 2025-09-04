package com.proxiad.holidaysapp.service;

import com.proxiad.holidaysapp.entity.AppUser;
import com.proxiad.holidaysapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.UUID.randomUUID;

/**
 * Works with github
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    // [1]
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        // базовая загрузка пользователя через стандартный сервис
        OAuth2User oAuth2User = super.loadUser(userRequest);
        var provider = userRequest.getClientRegistration().getRegistrationId();

        // имя провайдера (google, github и т.д.)
        var registrationId = userRequest.getClientRegistration().getRegistrationId();

        // атрибуты, которые вернул провайдер - скопировали, чтобы можно было изменять
        var attributes = new HashMap<>(oAuth2User.getAttributes());

        // в Google, например, это "sub", в GitHub — "id"
        var userIdAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        var oauthId = attributes.get(userIdAttributeName).toString();
        var email = getEmailFromAttributes(userRequest, attributes, provider);
        var name = getNameFromAttributes(attributes, provider);

        // поиск или создание пользователя
        val user = userRepository.findByOauthIdAndProvider(oauthId, registrationId)
                .orElseGet(() -> {
                    AppUser newUser = new AppUser();
                    newUser.setOauthId(oauthId);
                    newUser.setProvider(registrationId);
                    newUser.setEmail(email);
                    newUser.setName(name != null ? name : attributes.get("login").toString());
                    newUser.setPassword("oauth2user-" + randomUUID());
                    return userRepository.save(newUser);
                });
        log.info("User found : [{}] {} : {}", user.getId(), user.getEmail(), user.getName());

        // возвращаем пользователя, обернутого в DefaultOAuth2User
        return new DefaultOAuth2User(
                oAuth2User.getAuthorities(),
                attributes,
                userIdAttributeName
        );
    }

    // [2]
    public OAuth2User loadUser2(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();

        // Получаем атрибуты из ответа OAuth2 провайдера
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // Извлекаем email и name в зависимости от провайдера
        var email = getEmailFromAttributes(userRequest, attributes, provider);
        var name = getNameFromAttributes(attributes, provider);

        // Ищем пользователя по email
        val existingUser = userRepository.findByEmail(email);

        AppUser user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            user.setName(name); // Обновляем имя
            user.setProvider(provider); // Обновляем провайдер
        } else {
            // Создаем нового пользователя
            user = AppUser.builder()
                    .email(email)
                    .name(name)
                    .provider(provider)
                    .password("") // Пароль не используется при OAuth2 аутентификации
                    .build();
        }

        userRepository.save(user);
        return oAuth2User;
    }

    private String getEmailFromAttributes(OAuth2UserRequest userRequest, Map<String, Object> attributes, String provider) {
        var email = "";
        if ("google".equals(provider)) {
            email = (String) attributes.get("email");
        } else if ("github".equals(provider)) {
            email = (String) attributes.get("email");
            if (email == null) {
                email = fetchEmailForGithub(userRequest, attributes);
            }
        }

        // Добавьте обработку для других провайдеров

        return email;
    }

    private String getNameFromAttributes(Map<String, Object> attributes, String provider) {
        if ("google".equals(provider)) {
            return (String) attributes.get("name");
        } else if ("github".equals(provider)) {
            return (String) attributes.get("name");
        }
        // Добавьте обработку для других провайдеров
        return (String) attributes.get("name");
    }

    private String fetchEmailForGithub(OAuth2UserRequest userRequest, Map<String, Object> attributes) {
        // Дополнительный запрос на https://api.github.com/user/emails
        var email = "";
        var token = userRequest.getAccessToken().getTokenValue();
        var headers = new HttpHeaders();
        log.info("Github token : {}", token);
        headers.add("Authorization", "Bearer " + token);
        var entity = new HttpEntity<>("", headers);

        ResponseEntity<List<Map<String, Object>>> response =
                restTemplate.exchange("https://api.github.com/user/emails",
                        HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
                        });

        if (response.getBody() != null) {
            email = (String)response.getBody().stream()
                    .filter(t -> Boolean.TRUE.equals(t.get("primary")))
                    .map(t -> t.get("email"))
                    .findAny()
                    .orElse(null);
            attributes.put("email", email);
        }

        // fallback — если email не нашли
        if (email == null) {
            email = attributes.get("login") + "@github.local";
        }
        return email;
    }

//    @Override
//    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
//        OAuth2User oAuth2User = super.loadUser(userRequest);
//        String provider = userRequest.getClientRegistration().getRegistrationId();
//        String email = oAuth2User.getAttribute("email");
//        String name = oAuth2User.getAttribute("name");
//
//        User user = userRepository.findByEmail(email)
//                .map(existingUser -> existingUser.updateName(name))
//                .orElseGet(() -> User.builder()
//                        .email(email)
//                        .name(name)
//                        .provider(provider)
//                        .build());
//
//        userRepository.save(user);
//        return oAuth2User;
//    }
}
