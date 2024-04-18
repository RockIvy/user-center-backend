package com.ivy.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ivy.usercenter.common.ErrorCode;
import com.ivy.usercenter.exception.BusinessException;
import com.ivy.usercenter.mapper.UserMapper;
import com.ivy.usercenter.model.domain.User;
import com.ivy.usercenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ivy.usercenter.contant.UserConstant.USER_LOGIN_STATE;

/**
* @author ivy
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2024-04-10 16:57:51
*/
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Autowired
    private UserMapper userMapper;

    /**
     * 盐值， 混淆密码
     */
    private static final String SALT = "yupi";


    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode) {
        //1.校验
        if(StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"数据为空");
        }
        //账户不小于4位
        if(userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号多短");
        }
        //密码不小于 8 位
        if(userPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短");
        }
        // 星球编号长度不大于五
        if (planetCode.length() > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号过长");
        }
        //账户不包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\\\\\[\\\\\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？\\s]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if(matcher.find()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号包含特殊字符");
        }
        //校验密码和密码相同
        if(!userPassword.equals(checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入密码不一致");
        }
        //账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if(count > 0){
            throw new BusinessException(ErrorCode.USER_DUPLICATE_ERROR);
        }
        //星球编号不能重复
        //账户不能重复
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode);
        count = userMapper.selectCount(queryWrapper);
        if(count > 0){
            throw new BusinessException(ErrorCode.USER_DUPLICATE_ERROR);
        }
        //2.加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        //3.插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        boolean saveResult = this.save(user);
        if (!saveResult){
            throw new BusinessException(ErrorCode.SAVE_USER_ERROR);
        }
        return user.getId();
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.校验
        if(StringUtils.isAnyBlank(userAccount, userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"数据为空");
        }
        //账户不小于4位
        if(userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号过短");
        }
        //密码不小于 8 位
        if(userPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短");
        }
        //账户不包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\\\\\[\\\\\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？\\s]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if(matcher.find()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号包含特殊字符");
        }
        //2.加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        //查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if(user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        //3. 用户脱敏
        User safetyUser = getSafetyUser(user);
        //4. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);

        return safetyUser;
    }

    /**
     * 用户脱敏
     * @param originUser 起始用户
     * @return 安全用户
     */
    @Override
    public User getSafetyUser(User originUser) {
        // 判空
        if (originUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        safetyUser.setCreateTime(originUser.getCreateTime());
        return safetyUser;
    }

    @Override
    public int userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return  1;
    }
}




