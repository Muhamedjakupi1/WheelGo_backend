package com.wheelGo.service;

import com.wheelGo.model.invoices.InvoiceEmailRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class InvoicePdfService {
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.ENGLISH);

    public byte[] generateInvoicePdf(InvoiceEmailRequest request) {
        List<String> lines = List.of(
                "WheelGo Invoice",
                "Invoice number: " + safe(request.invoiceNumber()),
                "Booking id: " + request.bookingId(),
                "Customer email: " + safe(request.recipientEmail()),
                "Booking start: " + formatDateTime(request.bookingStart()),
                "Booking end: " + formatDateTime(request.bookingEnd()),
                "Paid at: " + formatDateTime(request.paidAt()),
                "Amount paid: " + formatAmount(request.amount(), request.currency()),
                "Thank you for choosing WheelGo."
        );

        String content = buildPageContent(lines);
        List<byte[]> objects = new ArrayList<>();
        objects.add(pdfObject("<< /Type /Catalog /Pages 2 0 R >>"));
        objects.add(pdfObject("<< /Type /Pages /Kids [3 0 R] /Count 1 >>"));
        objects.add(pdfObject("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>"));
        objects.add(pdfObject("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));
        objects.add(pdfObject("<< /Length " + content.getBytes(StandardCharsets.ISO_8859_1).length + " >>\nstream\n" + content + "endstream"));

        return buildPdf(objects);
    }

    private String buildPageContent(List<String> lines) {
        StringBuilder content = new StringBuilder();
        content.append("BT\n/F1 18 Tf\n72 760 Td\n")
                .append("(").append(escapePdf(lines.get(0))).append(") Tj\n")
                .append("/F1 11 Tf\n0 -34 Td\n");

        for (int i = 1; i < lines.size(); i++) {
            content.append("(").append(escapePdf(lines.get(i))).append(") Tj\n0 -20 Td\n");
        }

        content.append("ET\n");
        return content.toString();
    }

    private byte[] buildPdf(List<byte[]> objects) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, "%PDF-1.4\n");

        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(out.size());
            write(out, (i + 1) + " 0 obj\n");
            out.writeBytes(objects.get(i));
            write(out, "\nendobj\n");
        }

        int xrefOffset = out.size();
        write(out, "xref\n0 " + (objects.size() + 1) + "\n");
        write(out, "0000000000 65535 f \n");
        for (Integer offset : offsets) {
            write(out, String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        }
        write(out, "trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\n");
        write(out, "startxref\n" + xrefOffset + "\n%%EOF\n");
        return out.toByteArray();
    }

    private byte[] pdfObject(String value) {
        return value.getBytes(StandardCharsets.ISO_8859_1);
    }

    private void write(ByteArrayOutputStream out, String value) {
        out.writeBytes(value.getBytes(StandardCharsets.ISO_8859_1));
    }

    private String formatAmount(BigDecimal amount, String currency) {
        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        String safeCurrency = currency == null || currency.isBlank() ? "EUR" : currency;
        return safeAmount + " " + safeCurrency;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE_TIME_FORMAT.format(dateTime);
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }

    private String escapePdf(String value) {
        return safe(value)
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replaceAll("[^\\x20-\\x7E]", "?");
    }
}
