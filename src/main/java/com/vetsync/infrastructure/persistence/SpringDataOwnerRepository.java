package com.vetsync.infrastructure.persistence;

import com.vetsync.domain.model.Owner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataOwnerRepository extends JpaRepository<Owner, Long> {
}
