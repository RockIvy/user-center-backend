package com.ivy.usercenter.service;

import com.ivy.usercenter.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;

/**
* @author ivy
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2024-04-10 16:57:51
*/
public interface UserService extends IService<User> {


    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param planetCode 星球编号
     * @return 用户id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode);

    /**
     * 用户登录
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param request 请求
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);


    /**
     * 用户脱敏
     * @param originUser 起始用户
     * @return 安全用户
     */
    User getSafetyUser(User originUser);

    /**
     * 用户注销（退出登录）
     * @param request 请求
     * @return 1
     */
    int userLogout(HttpServletRequest request);
}
