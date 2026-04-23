package com.wheelGo.mapper;

import com.wheelGo.model.chat_messages.ChatMessage;
import com.wheelGo.model.chat_messages.ChatMessageResponse;

import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface ChatMessageMapper extends BaseMapper<ChatMessageResponse, ChatMessage> {}