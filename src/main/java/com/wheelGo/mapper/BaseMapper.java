package com.wheelGo.mapper;

import java.util.List;

public interface BaseMapper<D, E> {
    D toResponse(E entity);
    List<D> toResponseList(List<E> entities);
}