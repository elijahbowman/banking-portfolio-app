package com.portfolio.banking.bankingservice.repository;

import com.portfolio.banking.bankingservice.domain.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {
}