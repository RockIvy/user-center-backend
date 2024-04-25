package com.ivy.usercenter.model.domain.request;

import lombok.Data;

/**
 * @author ivy
 * @date 2024/4/24 13:21
 */
@Data
public class UserUpdateRequest {
    private Long id;
    private String userAccount;
    private String username;
    private String userPassword;// 你可以决定是否允许通过此API修改密码
    private Integer gender;
    private String avatarUrl;
    private String phone;
    private String email;
    private Integer userStatus;
    private String planetCode;
}
