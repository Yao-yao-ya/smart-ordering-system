package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.mapper.DishMapper;
import com.sky.properties.AiProperties;
import com.sky.service.AiOrderService;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiOrderServiceImpl implements AiOrderService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private AiProperties aiProperties;

    private static final String MENU_CACHE_KEY = "sky:ai:menu";
    private static final long MENU_CACHE_TTL = 1;

    /**
     * 从 Redis 获取菜单 JSON，未命中则查数据库并缓存
     */
    private String getMenuJson() {
        String cached = (String) redisTemplate.opsForValue().get(MENU_CACHE_KEY);
        if (cached != null) {
            return cached;
        }

        Dish query = Dish.builder().status(StatusConstant.ENABLE).build();
        List<Dish> dishes = dishMapper.list(query);

        List<Map<String, Object>> menuItems = dishes.stream().map(dish -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", dish.getName());
            item.put("price", dish.getPrice());
            return item;
        }).collect(Collectors.toList());

        String menuJson = JSON.toJSONString(menuItems);

        redisTemplate.opsForValue().set(MENU_CACHE_KEY, menuJson, MENU_CACHE_TTL, TimeUnit.HOURS);
        log.info("菜单缓存已更新，共{}道菜品", dishes.size());

        return menuJson;
    }

    @Override
    public String chatWithAi(String userMessage) {
        String menuJson = getMenuJson();

        // 构建 System Prompt
        String systemPrompt = "你是苍穹外卖的智能点餐助手。以下是我们的实时菜单：" + menuJson
                + "。请根据用户的需求，只推荐菜单上有的菜，并计算总价。";

        // 构建 OpenAI 兼容格式的请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-v4-pro");

        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);

        requestBody.put("messages", Arrays.asList(systemMsg, userMsg));

        String url = aiProperties.getBaseUrl() + "/chat/completions";

        try {
            HttpResponse response = HttpRequest.post(url)
                    .header("Authorization", "Bearer " + aiProperties.getApiKey())
                    .header("Content-Type", "application/json")
                    .body(JSON.toJSONString(requestBody))
                    .timeout(60000)
                    .execute();

            JSONObject jsonObject = JSON.parseObject(response.body());

            return jsonObject.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        } catch (Exception e) {
            log.error("AI接口调用失败", e);
            return "抱歉，AI服务暂时不可用，请稍后再试。";
        }
    }

}
