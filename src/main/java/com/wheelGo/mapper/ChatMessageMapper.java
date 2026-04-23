package com.wheelGo.mapper;

import com.wheelGo.model.chatmessages.ChatMessage;
import com.wheelGo.model.chatmessages.ChatMessageResponse;

import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface ChatMessageMapper extends BaseMapper<ChatMessageResponse, ChatMessage> {}