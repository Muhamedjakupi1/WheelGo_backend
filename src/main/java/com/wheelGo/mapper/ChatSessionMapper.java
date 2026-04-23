package com.wheelGo.mapper;

import com.wheelGo.model.chatsessions.ChatSession;
import com.wheelGo.model.chatsessions.ChatSessionResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatSessionMapper extends BaseMapper<ChatSessionResponse, ChatSession> {}