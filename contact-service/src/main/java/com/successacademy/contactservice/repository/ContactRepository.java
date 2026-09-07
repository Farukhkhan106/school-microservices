package com.successacademy.contactservice.repository;

import com.successacademy.contactservice.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    // status-based queries replacing old boolean resolved
    List<Contact> findByStatus(String status);

    // pending = not resolved (status != "Resolved")
    List<Contact> findByStatusNot(String status);
}
