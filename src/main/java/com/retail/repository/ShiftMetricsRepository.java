package com.retail.repository;

import com.retail.entity.ShiftMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftMetricsRepository extends JpaRepository<ShiftMetrics, Long> {
}
