package com.successacademy.contactservice.service;

import com.successacademy.contactservice.model.Contact;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends a real email notification to the admin inbox every time a new
 * enquiry is submitted on the website (contact form or event enquiry).
 *
 * - Runs asynchronously (@Async) so the visitor's form is never slowed down.
 * - Email failure NEVER blocks the enquiry from being saved (try/catch).
 * - Configure via application.properties → spring.mail.* and app.mail.*
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:}")
    private String from;

    @Value("${app.mail.to:}")
    private String to;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    @Async
    public void sendNewEnquiryNotification(Contact contact) {
        if (!enabled || to.isBlank() || from.isBlank()) {
            log.warn("Email notification SKIPPED — set spring.mail.username / app.mail.to / app.mail.enabled in application.properties");
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from);
            // Allows several admin emails, comma separated
            helper.setTo(to.split("\\s*,\\s*"));
            helper.setSubject(buildSubject(contact));
            helper.setText(buildHtmlBody(contact), true);   // true = send as HTML

            mailSender.send(message);
            log.info(">>> Enquiry notification email sent to admin for contact id {}", contact.getId());
        } catch (Exception e) {
            // Never let an email problem break the enquiry flow
            log.error("Failed to send enquiry notification email: {}", e.getMessage());
        }
    }

    private String buildSubject(Contact c) {
        if ("Event Enquiry".equalsIgnoreCase(c.getEnquiryType())) {
            return "[EVENT ENQUIRY] " + c.getEventTitle() + " — from " + c.getName();
        }
        return "[NEW ENQUIRY] Website query from " + c.getName();
    }

    private String buildHtmlBody(Contact c) {
        boolean isEvent = "Event Enquiry".equalsIgnoreCase(c.getEnquiryType());

        String extraRow = "";
        if (isEvent && c.getEventTitle() != null && !c.getEventTitle().isBlank()) {
            extraRow = row("🎪 Event", escape(c.getEventTitle()));
        } else if (c.getStudentName() != null && !c.getStudentName().isBlank()) {
            extraRow = row("🎓 Student", escape(c.getStudentName()));
        }

        return """
                <div style="font-family:Arial,Helvetica,sans-serif;background:#f4f5f7;padding:24px;">
                  <div style="max-width:560px;margin:auto;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb;">
                    <div style="background:#1e3a8a;padding:20px 24px;">
                      <h2 style="color:#ffffff;margin:0;font-size:18px;">Success Academy — New %s</h2>
                    </div>
                    <div style="padding:24px;">
                      <p style="margin:0 0 16px;color:#374151;font-size:14px;">
                        You have received a new enquiry through the school website.
                      </p>
                      <table style="width:100%%;border-collapse:collapse;font-size:14px;color:#374151;">
                        %s
                        %s
                        %s
                        %s
                        %s
                        %s
                      </table>
                      <p style="margin:20px 0 0;color:#6b7280;font-size:12px;">
                        Submitted: %s &nbsp;|&nbsp; Enquiry ID: %s<br/>
                        Login to the admin dashboard to manage this enquiry.
                      </p>
                    </div>
                  </div>
                </div>
                """.formatted(
                isEvent ? "Event Enquiry" : "Enquiry",
                row("👤 Name", escape(c.getName())),
                extraRow,
                row("📧 Email", escape(c.getEmail())),
                row("📱 Phone", escape(c.getPhone())),
                row("💬 Message", escape(c.getMessage()).replace("\n", "<br/>")),
                row("📌 Status", escape(c.getStatus())),
                String.valueOf(c.getSubmittedAt()),
                c.getId() != null ? c.getId().toString() : "-"
        );
    }

    private String row(String label, String value) {
        return """
                <tr>
                  <td style="padding:8px 12px;background:#f9fafb;font-weight:bold;width:110px;vertical-align:top;">%s</td>
                  <td style="padding:8px 12px;">%s</td>
                </tr>
                """.formatted(label, value);
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}