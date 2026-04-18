package com.wheelGo.mapper;

import com.wheelGo.model.bookings.Booking;
import com.wheelGo.model.bookings.BookingResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookingMapper extends BaseMapper<BookingResponse, Booking> {}