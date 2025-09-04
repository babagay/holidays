package com.proxiad.holidaysapp.repository;

import com.proxiad.holidaysapp.entity.AppUser;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends ListCrudRepository<AppUser,Integer> {

    Optional<AppUser> findByEmail(String email);
    Optional<AppUser> findByOauthIdAndProvider(String oauthId, String provider);
}
