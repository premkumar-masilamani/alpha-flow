package com.prem.ta.repositories;

import com.prem.ta.entities.Interval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IntervalRepository extends JpaRepository<Interval, Long> {

    // Lookup by label like "1m", "1h"
    Optional<Interval> findByLabel(String label);

    // Ensure uniqueness
    boolean existsByLabel(String label);
}
