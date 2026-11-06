package com.vetsync.domain.repository;

import com.vetsync.domain.model.OwnerRegistration;
import java.util.List;
import java.util.Optional;

public interface OwnerRegistrationRepository {
    OwnerRegistration save(OwnerRegistration request);

    List<OwnerRegistration> findPending();

    Optional<OwnerRegistration> findByIdForUpdate(Long id);

    void delete(OwnerRegistration request);
}
