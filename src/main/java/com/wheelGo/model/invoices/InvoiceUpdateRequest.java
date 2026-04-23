package com.wheelGo.model.invoices;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class InvoiceUpdateRequest {

    private LocalDateTime dueAt;

    private String pdfUrl;

}
