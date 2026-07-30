package com.healthtrack.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notifications.email.enabled", havingValue = "true", matchIfMissing = false)
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.notifications.email.from}")
    private String fromAddress;

    @Async
    @io.github.resilience4j.bulkhead.annotation.Bulkhead(name = "external", type = io.github.resilience4j.bulkhead.annotation.Bulkhead.Type.THREADPOOL)
    @org.springframework.retry.annotation.Retryable(
            retryFor = {MailException.class, MessagingException.class},
            maxAttempts = 5,
            backoff = @org.springframework.retry.annotation.Backoff(delay = 2000, multiplier = 2.0)
    )
    public void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (MailException | MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Email sending failed", e);
        }
    }

    public String buildAppointmentConfirmation(String patientName, String doctorName, String date, String time) {
        return """
            <!DOCTYPE html>
            <html><body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background: #2563eb; color: white; padding: 20px; text-align: center;">
                    <h1>Appointment Confirmed</h1>
                </div>
                <div style="padding: 20px; background: #f8fafc;">
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Your appointment has been confirmed:</p>
                    <table style="width: 100%%; border-collapse: collapse;">
                        <tr><td style="padding: 8px; border: 1px solid #e2e8f0;"><strong>Doctor</strong></td>
                            <td style="padding: 8px; border: 1px solid #e2e8f0;">%s</td></tr>
                        <tr><td style="padding: 8px; border: 1px solid #e2e8f0;"><strong>Date</strong></td>
                            <td style="padding: 8px; border: 1px solid #e2e8f0;">%s</td></tr>
                        <tr><td style="padding: 8px; border: 1px solid #e2e8f0;"><strong>Time</strong></td>
                            <td style="padding: 8px; border: 1px solid #e2e8f0;">%s</td></tr>
                    </table>
                    <p style="margin-top: 20px; color: #64748b;">Please arrive 15 minutes early.</p>
                </div>
            </body></html>
            """.formatted(patientName, doctorName, date, time);
    }

    public String buildBillNotification(String patientName, String billAmount, String billId) {
        return """
            <!DOCTYPE html>
            <html><body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background: #059669; color: white; padding: 20px; text-align: center;">
                    <h1>Bill Generated</h1>
                </div>
                <div style="padding: 20px; background: #f8fafc;">
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Your bill has been generated:</p>
                    <table style="width: 100%%; border-collapse: collapse;">
                        <tr><td style="padding: 8px; border: 1px solid #e2e8f0;"><strong>Bill ID</strong></td>
                            <td style="padding: 8px; border: 1px solid #e2e8f0;">#%s</td></tr>
                        <tr><td style="padding: 8px; border: 1px solid #e2e8f0;"><strong>Amount</strong></td>
                            <td style="padding: 8px; border: 1px solid #e2e8f0; font-size: 18px; font-weight: bold;">$%s</td></tr>
                    </table>
                </div>
            </body></html>
            """.formatted(patientName, billId, billAmount);
    }

    public String buildLabResultNotification(String patientName, String testName) {
        return """
            <!DOCTYPE html>
            <html><body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background: #7c3aed; color: white; padding: 20px; text-align: center;">
                    <h1>Lab Results Ready</h1>
                </div>
                <div style="padding: 20px; background: #f8fafc;">
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Your <strong>%s</strong> results are ready for review.</p>
                    <p style="margin-top: 20px; color: #64748b;">Please consult your doctor to discuss the results.</p>
                </div>
            </body></html>
            """.formatted(patientName, testName);
    }
}
