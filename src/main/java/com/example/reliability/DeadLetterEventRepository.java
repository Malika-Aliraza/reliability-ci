package com.example.reliability;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, Long> {
    Optional<DeadLetterEvent> findFirstByEventKeyOrderByIdDesc(String eventKey);
}