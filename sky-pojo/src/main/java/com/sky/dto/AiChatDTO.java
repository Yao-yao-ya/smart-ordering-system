package com.sky.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "AI点餐对话请求")
public class AiChatDTO implements Serializable {

    @ApiModelProperty("用户聊天内容")
    private String message;

}
