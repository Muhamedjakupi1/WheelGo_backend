package com.wheelGo.mapper;

import com.wheelGo.model.bookingaddons.BookingAddon;
import com.wheelGo.model.bookingaddons.BookingAddonResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookingAddonMapper extends BaseMapper<BookingAddonResponse, BookingAddon> {}
