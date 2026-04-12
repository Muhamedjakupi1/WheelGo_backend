package com.wheelGo.model.invoices;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
public class InvoiceResponse {
    private UUID id;
    private String invoiceNumber;
    private String pdfUrl;
    private LocalDateTime issuedAt;
    private LocalDateTime dueAt;

  public static InvoiceResponse from(Invoice i){
      InvoiceResponse r = new InvoiceResponse();
      r.setId(i.getId());
      r.setInvoiceNumber(i.getInvoiceNumber());
      r.setPdfUrl(i.getPdfUrl());
      r.setIssuedAt(i.getIssuedAt());
      r.setDueAt(i.getDueAt());
      return r;
  }
}
