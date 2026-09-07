package com.successacademy.contactservice.service;

import com.successacademy.contactservice.model.Contact;
import com.successacademy.contactservice.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactRepository repository;
    private final EmailService emailService;

    @Override
    public Contact submit(Contact contact) {
        contact.setStatus("Pending");
        contact.setSubmittedAt(java.time.LocalDateTime.now());
        if (contact.getEnquiryType() == null || contact.getEnquiryType().isBlank()) {
            contact.setEnquiryType("General");
        }
        Contact saved = repository.save(contact);

        // 🔔 Send a real email notification to the admin inbox.
        //    Async + try/catch inside — never blocks or breaks the submission.
        emailService.sendNewEnquiryNotification(saved);

        return saved;
    }

    @Override
    public List<Contact> getAllMessages() {
        return repository.findAll()
                .stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .toList();
    }

    @Override
    public List<Contact> getPendingMessages() {
        // Returns Pending + In Progress (everything that's not Resolved)
        return repository.findByStatusNot("Resolved")
                .stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .toList();
    }

    @Override
    public Contact updateStatus(Long id, String status) {
        Contact contact = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact message not found with id: " + id));

        // Validate allowed statuses
        if (!status.equals("Pending") && !status.equals("In Progress") && !status.equals("Resolved")) {
            throw new RuntimeException("Invalid status. Allowed: Pending, In Progress, Resolved");
        }

        contact.setStatus(status);
        return repository.save(contact);
    }

    @Override
    public Contact updateNotes(Long id, String notes) {
        Contact contact = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact message not found with id: " + id));
        contact.setNotes(notes);
        return repository.save(contact);
    }

    @Override
    public void deleteMessage(Long id) {
        repository.deleteById(id);
    }
}
