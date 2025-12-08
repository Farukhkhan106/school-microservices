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

    @Override
    public Contact submit(Contact contact) {
        return repository.save(contact);
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
        return repository.findByResolved(false)
                .stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .toList();
    }

    @Override
    public Contact markResolved(Long id, boolean resolved) {
        Contact contact = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact message not found"));
        contact.setResolved(resolved);
        return repository.save(contact);
    }

    @Override
    public void deleteMessage(Long id) {
        repository.deleteById(id);
    }
}
