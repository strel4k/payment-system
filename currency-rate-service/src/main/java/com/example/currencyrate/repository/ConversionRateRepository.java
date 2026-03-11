package com.example.currencyrate.repository;

import com.example.currencyrate.entity.ConversionRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ConversionRateRepository extends JpaRepository<ConversionRate, Long> {

    @Query("""
            SELECT cr FROM ConversionRate cr
            WHERE cr.sourceCode = :sourceCode
              AND cr.destinationCode = :destinationCode
              AND cr.rateBeginTime <= :timestamp
              AND cr.rateEndTime > :timestamp
            ORDER BY cr.rateBeginTime DESC
            LIMIT 1
            """)
    Optional<ConversionRate> findRateAtTimestamp(
            @Param("sourceCode") String sourceCode,
            @Param("destinationCode") String destinationCode,
            @Param("timestamp") LocalDateTime timestamp
    );

    @Query("""
            SELECT cr FROM ConversionRate cr
            WHERE cr.sourceCode = :sourceCode
              AND cr.destinationCode = :destinationCode
            ORDER BY cr.rateBeginTime DESC
            LIMIT 1
            """)
    Optional<ConversionRate> findLatestRate(
            @Param("sourceCode") String sourceCode,
            @Param("destinationCode") String destinationCode
    );

    @Modifying
    @Query("""
            UPDATE ConversionRate cr
            SET cr.rateEndTime = :now
            WHERE cr.sourceCode = :sourceCode
              AND cr.destinationCode = :destinationCode
              AND cr.rateEndTime > :now
            """)
    int expireActiveRates(
            @Param("sourceCode") String sourceCode,
            @Param("destinationCode") String destinationCode,
            @Param("now") LocalDateTime now
    );
}