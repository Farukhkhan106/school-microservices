package com.successacademy.contactservice.controller;

import com.successacademy.contactservice.model.Contact;
import com.successacademy.contactservice.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contact")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ContactController {

    private final ContactService service;

    // PUBLIC: Submit contact message
    @PostMapping("/submit")
    public ResponseEntity<Contact> submit(@RequestBody Contact contact) {
        return ResponseEntity.ok(service.submit(contact));
    }

    // ADMIN: Get all contact messages
    @GetMapping("/all")
    public ResponseEntity<List<Contact>> all() {
        return ResponseEntity.ok(service.getAllMessages());
    }

    // ADMIN: Get only pending (not resolved) messages
    @GetMapping("/pending")
    public ResponseEntity<List<Contact>> pending() {
        return ResponseEntity.ok(service.getPendingMessages());
    }

    // ADMIN: Mark as resolved / unresolved
    @PutMapping("/resolve/{id}")
    public ResponseEntity<Contact> resolve(
            @PathVariable Long id,
            @RequestParam boolean status
    ) {
        return ResponseEntity.ok(service.markResolved(id, status));
    }

    // ADMIN: Delete a message
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        service.deleteMessage(id);
        return ResponseEntity.ok("Message deleted");
    }
}
