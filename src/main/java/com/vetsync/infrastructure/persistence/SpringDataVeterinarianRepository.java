package com.vetsync.infrastructure.persistence;

import com.vetsync.domain.model.Veterinarian;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataVeterinarianRepository extends JpaRepository<Veterinarian, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Veterinarian v where v.id = :id")
    Optional<Veterinarian> findByIdForUpdate(@Param("id") Long id);
}
