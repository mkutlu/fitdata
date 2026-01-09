package com.aarw.fitdata.oauth.token;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FitbitTokenRepository extends JpaRepository<FitbitTokenEntity, Long> {
    Optional<FitbitTokenEntity> findByFitbitUserId(String fitbitUserId);
}