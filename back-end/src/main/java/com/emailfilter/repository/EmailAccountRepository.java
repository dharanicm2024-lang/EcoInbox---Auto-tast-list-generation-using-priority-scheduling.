package com.emailfilter.repository;

import com.emailfilter.entity.EmailAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailAccountRepository extends JpaRepository<EmailAccount, Long> {

    List<EmailAccount> findByIsActiveTrue();

    Optional<EmailAccount> findByEmail(String email);

    boolean existsByEmail(String email);
}
