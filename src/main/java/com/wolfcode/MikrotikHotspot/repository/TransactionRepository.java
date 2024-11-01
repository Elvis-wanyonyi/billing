package com.wolfcode.MikrotikHotspot.repository;

import com.wolfcode.MikrotikHotspot.entity.TransactionsInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionsInfo, Long> {
}
