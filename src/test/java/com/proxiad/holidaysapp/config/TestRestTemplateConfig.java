package com.proxiad.holidaysapp.config;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.client.RestTemplateBuilderConfigurer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@TestConfiguration
@Profile({"test", "test2"})
@Slf4j
public class TestRestTemplateConfig {

    @Value("${test.user}")
    String userName;

    @Value("${test.pass}")
    String password;

    @Value("${rest.template.rootUrl}")
    private String root; // get from application.yml

    // [1] configuration via rest template builder
    @Bean
    RestTemplateBuilder restTemplateBuilder(RestTemplateBuilderConfigurer configurer) {

        assert root != null;
        log.info("Test RestTemplate root : {}", root);

        val builder = configurer.configure(new RestTemplateBuilder());
        val uriBuilderFactory = new DefaultUriBuilderFactory(root);
        // but this root does not matter. could be any string. Override during the tests

        return builder.basicAuthentication(userName, password);
                //.uriTemplateHandler(uriBuilderFactory);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    // [2] configuration via rest template itself
//    @Bean // ("testRestTemplate")
//    public RestTemplate restTemplate() {
//        var restTemplate = new RestTemplate();
//
//        restTemplate.getInterceptors().add((request, body, execution) -> {
//            var auth = userName + ":" + password;
//            var encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
//            request.getHeaders().add(AUTHORIZATION, "Basic " + encodedAuth);
//            log.info("Using Basic Auth credentials: user={} pass={}", userName, password);
//            return execution.execute(request, body);
//        });
//        return restTemplate;
//    }
}
