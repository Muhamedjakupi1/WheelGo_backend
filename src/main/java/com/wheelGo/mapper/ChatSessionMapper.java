package com.wheelGo.mapper;

import com.wheelGo.model.chat_sessions.ChatSession;
import com.wheelGo.model.chat_sessions.ChatSessionResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatSessionMapper extends BaseMapper<ChatSessionResponse, ChatSession> {}