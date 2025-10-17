package com.portfolio.banking.accountservice.repository;

import com.portfolio.banking.accountservice.domain.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, String> {
}