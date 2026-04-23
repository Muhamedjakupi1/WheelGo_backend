package com.wheelGo.mapper;

import com.wheelGo.model.booking_addons.BookingAddon;
import com.wheelGo.model.booking_addons.BookingAddonResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookingAddonMapper extends BaseMapper<BookingAddonResponse, BookingAddon> {}
