package com.wheelGo.service;

import com.wheelGo.model.invoices.InvoiceEmailRequest;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Async
public class InvoiceEmailJobService {

    private static final Logger log =
            LoggerFactory.getLogger(InvoiceEmailJobService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    private final InvoicePdfService invoicePdfService;

    @Value("${app.invoice.email.from:duazogu1@gmail.com}")
    private String fromEmail;

    @Async
    public CompletableFuture<Void> sendInvoiceEmail(
            InvoiceEmailRequest request
    ) {

        sendInvoiceEmailNow(request);

        return CompletableFuture.completedFuture(null);
    }

    public void sendInvoiceEmailNow(
            InvoiceEmailRequest request
    ) {

        System.out.println("EMAIL SERVICE STARTED");
        System.out.println("TO: " + request.recipientEmail());

        JavaMailSender mailSender =
                mailSenderProvider.getIfAvailable();

        if (mailSender == null) {
            throw new IllegalStateException(
                    "JavaMailSender is not configured"
            );
        }

        try {

            System.out.println("CREATING PDF");

            byte[] pdf =
                    invoicePdfService.generateInvoicePdf(request);

            System.out.println("PDF CREATED");
            System.out.println("PDF SIZE: " + pdf.length);

            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            "UTF-8"
                    );

            helper.setFrom(fromEmail);

            helper.setTo(request.recipientEmail());

            helper.setSubject(
                    "Your WheelGo Invoice - " +
                            request.invoiceNumber()
            );

            helper.setText("""
                    Thank you for your payment.

                    Your invoice PDF is attached.

                    WheelGo Team
                    """);

            helper.addAttachment(
                    request.invoiceNumber() + ".pdf",
                    new ByteArrayResource(pdf)
            );

            System.out.println("TRYING TO SEND EMAIL");

            mailSender.send(message);

            System.out.println(
                    "EMAIL WITH PDF SENT SUCCESSFULLY"
            );

            log.info(
                    "Invoice email sent successfully to {}",
                    request.recipientEmail()
            );

        } catch (Exception ex) {

            ex.printStackTrace();

            log.error(
                    "Failed to send invoice email",
                    ex
            );

            throw new IllegalStateException(
                    "Failed to send invoice email: " +
                            ex.getMessage(),
                    ex
            );
        }
    }
}