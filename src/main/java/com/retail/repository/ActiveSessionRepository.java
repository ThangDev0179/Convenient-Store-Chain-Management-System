package com.retail.repository;

import com.retail.entity.ActiveSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface ActiveSessionRepository extends JpaRepository<ActiveSession, Long> {

    @Modifying
    @Query(value = "MERGE INTO ActiveSession AS target " +
            "USING (SELECT :employeeId AS EmployeeId) AS source " +
            "ON target.EmployeeId = source.EmployeeId " +
            "WHEN MATCHED THEN " +
            "  UPDATE SET SessionToken = :sessionToken, DeviceId = :deviceId, IpAddress = :ipAddress, LoginAt = :loginAt, ExpiresAt = :expiresAt " +
            "WHEN NOT MATCHED THEN " +
            "  INSERT (EmployeeId, SessionToken, DeviceId, IpAddress, LoginAt, ExpiresAt) " +
            "  VALUES (source.EmployeeId, :sessionToken, :deviceId, :ipAddress, :loginAt, :expiresAt);", nativeQuery = true)
    void upsertSession(@Param("employeeId") Long employeeId,
                       @Param("sessionToken") String sessionToken,
                       @Param("deviceId") String deviceId,
                       @Param("ipAddress") String ipAddress,
                       @Param("loginAt") LocalDateTime loginAt,
                       @Param("expiresAt") LocalDateTime expiresAt);
}
