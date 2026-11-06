package com.vetsync.infrastructure.persistence;

import com.vetsync.domain.model.OwnerRegistration;
import com.vetsync.domain.repository.OwnerRegistrationRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class OwnerRegistrationRepositoryAdapter implements OwnerRegistrationRepository {
    private final SpringDataOwnerRegistrationRepository registrations;

    public OwnerRegistrationRepositoryAdapter(SpringDataOwnerRegistrationRepository registrations) {
        this.registrations = registrations;
    }

    @Override
    public OwnerRegistration save(OwnerRegistration request) {
        return registrations.save(request);
    }

    @Override
    public List<OwnerRegistration> findPending() {
        return registrations.findAllByOrderByCreatedAtAsc();
    }

    @Override
    public Optional<OwnerRegistration> findByIdForUpdate(Long id) {
        return registrations.findByIdForUpdate(id);
    }

    @Override
    public void delete(OwnerRegistration request) {
        registrations.delete(request);
    }
}
