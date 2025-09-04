package com.proxiad.holidaysapp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "USERS")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(nullable = false)
    String name;

    @Column(nullable = true)
    String password;

    @Column(nullable = true)
    String email;

    @Column(nullable = false)
    String oauthId;

    @Column(nullable = true)
    String provider;
}
