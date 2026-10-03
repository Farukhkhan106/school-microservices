package com.successacademy.staffservice.repository;

import com.successacademy.staffservice.model.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {

    Optional<Shift> findByNameIgnoreCase(String name);

    List<Shift> findByStatusIgnoreCase(String status);
}
