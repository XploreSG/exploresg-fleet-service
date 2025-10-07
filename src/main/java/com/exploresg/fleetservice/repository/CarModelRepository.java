package com.exploresg.fleetservice.repository;

import com.exploresg.fleetservice.model.CarModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the CarModel entity.
 * By extending JpaRepository, Spring will automatically provide implementations
 * for standard CRUD (Create, Read, Update, Delete) operations.
 */
@Repository
public interface CarModelRepository extends JpaRepository<CarModel, Long> {
    // Spring Data JPA will automatically implement methods like:
    // - save(CarModel entity)
    // - findById(Long id)
    // - findAll()
    // - deleteById(Long id)
    // ... and many more, just by extending the interface.
}