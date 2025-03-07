package com.gu.localai.action;

/**
 * @description: todo
 * @author: guhuanqi
 * @create: 2025-03-06 16:40
 **/
public class ChatMessage {

    final String role;
    String content;

    ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
