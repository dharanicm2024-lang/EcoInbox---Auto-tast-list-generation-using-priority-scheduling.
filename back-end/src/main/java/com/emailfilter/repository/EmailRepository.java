package com.emailfilter.repository;

import com.emailfilter.entity.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {

    Optional<Email> findByMessageId(String messageId);

    boolean existsByMessageId(String messageId);

    List<Email> findByIsProcessedFalse();

    List<Email> findAllByOrderByReceivedAtDesc();
}
