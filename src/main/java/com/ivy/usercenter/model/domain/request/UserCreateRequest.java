package com.ivy.usercenter.model.domain.request;

import lombok.Data;

@Data
public class UserCreateRequest {
    private String userAccount;
    private String userPassword;
    private String checkPassword;
    private String username;
    private String avatarUrl; // 可选
    private Integer gender; // 可选
    private String phone; // 可选
    private String email; // 可选
    private Integer userRole; // 可选，管理员在创建用户时指定角色
    private String planetCode;
}