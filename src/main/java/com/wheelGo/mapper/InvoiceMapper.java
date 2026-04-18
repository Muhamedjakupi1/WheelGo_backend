package com.wheelGo.mapper;

import com.wheelGo.model.invoices.Invoice;
import com.wheelGo.model.invoices.InvoiceResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InvoiceMapper extends BaseMapper<InvoiceResponse, Invoice>{
}
