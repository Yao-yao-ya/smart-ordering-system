package com.sky.service;

public interface AiOrderService {

    /**
     * 与大模型对话
     * @param userMessage 用户消息
     * @return AI回复的纯文本
     */
    String chatWithAi(String userMessage);

}
