package com.wheelGo.mapper;

import com.wheelGo.model.payments.Payment;
import com.wheelGo.model.payments.PaymentResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper extends BaseMapper<PaymentResponse, Payment> {
}
