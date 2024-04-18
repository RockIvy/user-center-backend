package com.ivy.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ivy.usercenter.common.BaseResponse;
import com.ivy.usercenter.common.ErrorCode;
import com.ivy.usercenter.common.ResultUtils;
import com.ivy.usercenter.exception.BusinessException;
import com.ivy.usercenter.model.domain.User;
import com.ivy.usercenter.model.domain.request.UserLoginrequest;
import com.ivy.usercenter.model.domain.request.UserRegisterRequest;
import com.ivy.usercenter.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

import static com.ivy.usercenter.contant.UserConstant.ADMIN_ROLE;
import static com.ivy.usercenter.contant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户控制器
 *
 * @author ivy
 * @date 2024/4/11 17:11
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();

        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            // 这里应该是抛异常
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }

        long id = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        return ResultUtils.success(id);
    }

    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginrequest userLoginrequest, HttpServletRequest request) {
        if (userLoginrequest == null) {
            return ResultUtils.error(ErrorCode.NULL_ERROR);
        }
        String userAccount = userLoginrequest.getUserAccount();
        String userPassword = userLoginrequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }

        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            return ResultUtils.error(ErrorCode.NULL_ERROR);
        }
        int i = userService.userLogout(request);
        return ResultUtils.success(i);
    }

    /**
     * 获取当前用户
     *
     * @param request 前端请求
     * @return 当前用户
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR);
        }
        long id = currentUser.getId();
        // to do 校验用户是否合法
        User user = userService.getById(id);
        User safetyUser = userService.getSafetyUser(user);
        return ResultUtils.success(safetyUser);
    }


    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResultUtils.error(ErrorCode.NO_AUTH_ERROR);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }
        List<User> userList = userService.list();
        List<User> collect = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(collect);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody Long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResultUtils.error(ErrorCode.NO_AUTH_ERROR);
        }
        if (id <= 0) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(id);
        return ResultUtils.success(b);
    }

    private boolean isAdmin(HttpServletRequest request) {
        // 鉴权，只有管理员可以查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        if (user == null || user.getUserRole() != ADMIN_ROLE) {
            return false;
        }
        return true;
    }
}
