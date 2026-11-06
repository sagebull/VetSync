package com.vetsync.infrastructure.persistence;

import com.vetsync.domain.model.Pet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataPetRepository extends JpaRepository<Pet, Long> {
    boolean existsByOwner_Id(Long ownerId);

    boolean existsByIdAndOwner_Id(Long petId, Long ownerId);

    @Query("""
            select p from Pet p
            where (:name is null or lower(p.name) like lower(concat('%', :name, '%')))
              and (:ownerName is null or lower(p.owner.fullName) like lower(concat('%', :ownerName, '%')))
              and (:ownerId is null or p.owner.id = :ownerId)
              and (:petId is null or p.id = :petId)
            order by p.name
            """)
    List<Pet> search(@Param("name") String name, @Param("ownerName") String ownerName,
                     @Param("ownerId") Long ownerId, @Param("petId") Long petId);
}
