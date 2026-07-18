package com.retail.repository;

import com.retail.entity.ActiveSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActiveSessionRepository extends JpaRepository<ActiveSession, Long> {
}
