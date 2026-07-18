package com.retail.procurement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoodsReceiptNoteDetailRepository extends JpaRepository<GoodsReceiptNoteDetail, Long> {
}
