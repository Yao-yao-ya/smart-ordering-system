package com.sky.controller.user;

import com.sky.dto.AiChatDTO;
import com.sky.result.Result;
import com.sky.service.AiOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userAiOrderController")
@RequestMapping("/user/ai")
@Slf4j
@Api(tags = "C端-AI智能点餐接口")
public class AiOrderController {

    @Autowired
    private AiOrderService aiOrderService;

    @PostMapping("/chat")
    @ApiOperation("智能点餐对话")
    public Result<String> chat(@RequestBody AiChatDTO aiChatDTO) {
        log.info("AI点餐请求：{}", aiChatDTO.getMessage());
        String reply = aiOrderService.chatWithAi(aiChatDTO.getMessage());
        return Result.success(reply);
    }

}
