package com.successacademy.contactservice.service;

import com.successacademy.contactservice.model.Contact;

import java.util.List;

public interface ContactService {

    Contact submit(Contact contact);

    List<Contact> getAllMessages();

    List<Contact> getPendingMessages();

    Contact markResolved(Long id, boolean resolved);

    void deleteMessage(Long id);
}
