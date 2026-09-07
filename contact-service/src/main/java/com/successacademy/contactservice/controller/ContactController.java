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
public class ContactController {

    private final ContactService service;

    // ── PUBLIC (whitelisted in gateway — no auth needed) ────────

    @PostMapping("/submit")
    public ResponseEntity<Contact> submit(@RequestBody Contact contact) {
        return ResponseEntity.ok(service.submit(contact));
    }

    // ── ADMIN ────────────────────────────────────────────────────

    // Get all messages
    @GetMapping("/all")
    public ResponseEntity<List<Contact>> all() {
        return ResponseEntity.ok(service.getAllMessages());
    }

    // Get pending + in-progress (not resolved)
    @GetMapping("/pending")
    public ResponseEntity<List<Contact>> pending() {
        return ResponseEntity.ok(service.getPendingMessages());
    }

    // Update status: Pending | In Progress | Resolved
    @PutMapping("/status/{id}")
    public ResponseEntity<Contact> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(service.updateStatus(id, status));
    }

    // Update admin notes
    @PutMapping("/notes/{id}")
    public ResponseEntity<Contact> updateNotes(
            @PathVariable Long id,
            @RequestParam String notes) {
        return ResponseEntity.ok(service.updateNotes(id, notes));
    }

    // Delete message
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        service.deleteMessage(id);
        return ResponseEntity.ok("Message deleted");
    }
}
