package com.vetsync.infrastructure.persistence;

import com.vetsync.domain.model.OwnerRegistration;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataOwnerRegistrationRepository extends JpaRepository<OwnerRegistration, Long> {
    List<OwnerRegistration> findAllByOrderByCreatedAtAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from OwnerRegistration r where r.id = :id")
    Optional<OwnerRegistration> findByIdForUpdate(@Param("id") Long id);
}
