package com.winter.redtodb.controller.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@ApiModel("ArticleCount")
@Data
public class PostArticleViewsRequest {

    @ApiModelProperty(value = "UserId")
    @NotNull
    private String customerId;

    @ApiModelProperty(value = "ArticleId")
    @NotNull
    private String articleNo;

    public PostArticleViewsRequest() {
    }
}

