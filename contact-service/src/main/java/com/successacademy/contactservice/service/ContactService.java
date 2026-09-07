package com.successacademy.contactservice.service;

import com.successacademy.contactservice.model.Contact;

import java.util.List;

public interface ContactService {

    Contact submit(Contact contact);

    List<Contact> getAllMessages();

    // Pending messages (status != Resolved)
    List<Contact> getPendingMessages();

    // Update status: Pending | In Progress | Resolved
    Contact updateStatus(Long id, String status);

    // Admin adds/updates internal notes
    Contact updateNotes(Long id, String notes);

    void deleteMessage(Long id);
}
