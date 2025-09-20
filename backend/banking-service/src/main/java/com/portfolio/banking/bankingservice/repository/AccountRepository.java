package com.portfolio.banking.bankingservice.repository;

import com.portfolio.banking.bankingservice.domain.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account> findByAccountNumber(String accountNumber);

    @Query("SELECT a FROM Account a WHERE a.balance >= :minBalance")
    List<Account> findAccountsWithMinimumBalance(@Param("minBalance") BigDecimal minBalance);

    @Query("SELECT SUM(a.balance) FROM Account a")
    BigDecimal getTotalSystemBalance();

    @Query("SELECT COUNT(a) FROM Account a")
    long countActiveAccounts();
}