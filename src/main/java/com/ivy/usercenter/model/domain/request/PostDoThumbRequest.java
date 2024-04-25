package com.ivy.usercenter.model.domain.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 点赞/取消点赞请求
 */
@Data
public class PostDoThumbRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 帖子id
     */
    private long postId;

}
