package com.wheelGo.mapper;

import com.wheelGo.model.chatMessages.ChatMessage;
import com.wheelGo.model.chatMessages.ChatMessageResponse;

import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface ChatMessageMapper extends BaseMapper<ChatMessageResponse, ChatMessage> {}