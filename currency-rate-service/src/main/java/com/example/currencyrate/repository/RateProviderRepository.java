package com.example.currencyrate.repository;

import com.example.currencyrate.entity.RateProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RateProviderRepository extends JpaRepository<RateProvider, String> {

    List<RateProvider> findAllByActiveTrueOrderByPriorityAsc();

    Optional<RateProvider> findByProviderCodeAndActiveTrue(String providerCode);
}