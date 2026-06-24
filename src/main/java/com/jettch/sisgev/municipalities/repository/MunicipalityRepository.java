package com.jettch.sisgev.municipalities.repository;

import com.jettch.sisgev.municipalities.entity.Municipality;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MunicipalityRepository extends JpaRepository<Municipality, UUID> {

    Page<Municipality> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Municipality> findByIdAndDeletedAtIsNull(UUID id, Pageable pageable);

    Optional<Municipality> findByIdAndDeletedAtIsNull(UUID id);
}
