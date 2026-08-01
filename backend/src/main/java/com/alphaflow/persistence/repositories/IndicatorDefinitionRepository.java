package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.IndicatorDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IndicatorDefinitionRepository extends JpaRepository<IndicatorDefinition, Long> {}
