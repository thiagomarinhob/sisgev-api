package com.jettch.sisgev.users.repository;

import com.jettch.sisgev.users.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);

    Page<User> findAllByDeletedAtIsNull(Pageable pageable);

    Page<User> findAllByMunicipalityIdAndDeletedAtIsNull(UUID municipalityId, Pageable pageable);

    boolean existsByEmailAndDeletedAtIsNullAndIdNot(String email, UUID id);
}
