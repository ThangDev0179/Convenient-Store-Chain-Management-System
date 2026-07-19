package com.retail.repository;
import com.retail.entity.Supplier;
import com.retail.entity.SupplierStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Integer> {
    List<Supplier> findByStatus(SupplierStatus status);

    boolean existsBySupplierName(String supplierName);
    boolean existsBySupplierNameAndSupplierIdNot(String supplierName, Integer supplierId);

    boolean existsByContactEmail(String email);
    boolean existsByContactEmailAndSupplierIdNot(String email, Integer supplierId);
    boolean existsByContactPhone(String phone);
    boolean existsByContactPhoneAndSupplierIdNot(String phone, Integer supplierId);

    @Query("SELECT s FROM Supplier s WHERE " +
           "(:keyword IS NULL OR LOWER(s.supplierName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(s.contactPhone) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(s.contactEmail) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:status IS NULL OR s.status = :status)")
    Page<Supplier> searchSuppliers(@Param("keyword") String keyword, 
                                   @Param("status") SupplierStatus status, 
                                   Pageable pageable);
}