package com.wheelGo.mapper;

import com.wheelGo.model.chatSessions.ChatSession;
import com.wheelGo.model.chatSessions.ChatSessionResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatSessionMapper extends BaseMapper<ChatSessionResponse, ChatSession> {}