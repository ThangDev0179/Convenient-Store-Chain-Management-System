package com.retail.shift;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftMetricsRepository extends JpaRepository<ShiftMetrics, Long> {
}
