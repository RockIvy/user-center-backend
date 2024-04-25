package com.ivy.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ivy.usercenter.common.ErrorCode;
import com.ivy.usercenter.contant.UserConstant;
import com.ivy.usercenter.exception.BusinessException;
import com.ivy.usercenter.mapper.UserMapper;
import com.ivy.usercenter.model.domain.User;
import com.ivy.usercenter.model.domain.request.UserCreateRequest;
import com.ivy.usercenter.model.domain.request.UserUpdateRequest;
import com.ivy.usercenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
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
        implements UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 盐值， 混淆密码
     */
    private static final String SALT = "yupi";


    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode, String avatarUrl, String username) {
        //1.校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "数据为空");
        }
        //账户不小于4位
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号多短");
        }
        //密码不小于 8 位
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短");
        }
        // 星球编号长度不大于五
        if (planetCode.length() > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号过长");
        }
        //账户不包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\\\\\[\\\\\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？\\s]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号包含特殊字符");
        }
        //校验密码和密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入密码不一致");
        }
        //账户不能重复
        if(checkUserAccountExists(userAccount)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }
        //星球编号不能重复
        if(checkPlanetCodeExists(planetCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号已存在");
        }
        //2.加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        //3.插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        user.setAvatarUrl(avatarUrl);
        user.setUsername(username);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SAVE_USER_ERROR);
        }
        return user.getId();
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "数据为空");
        }
        //账户不小于4位
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号过短");
        }
        //密码不小于 8 位
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短");
        }
        //账户不包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\\\\\[\\\\\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？\\s]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
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
        if (user == null) {
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
     *
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
        return 1;
    }



    /**
     * 根据提供的用户信息更新用户。
     * 此方法会根据 userUpdateRequest 中提供的信息更新数据库中相应的用户记录。
     * 具体校验逻辑如下：
     * - 用户ID不能为空，确保能正确定位需要更新的用户记录。
     * - 用户名如果提供，其长度不能少于4位。
     * - 账号如果提供，不能包含特殊字符。
     * - 密码如果提供，其长度不能少于8位，并对密码进行MD5加密处理。
     * - 星球编号如果提供，其长度不能超过5位。
     * - 账号和星球编号如果提供，需要检查它们的唯一性，以避免与其他用户的账号或星球编号冲突。
     *
     * @param userUpdateRequest 包含要更新用户信息的请求体。
     * @return 更新成功返回true，否则返回false。
     */
    @Override
    public boolean updateUser(UserUpdateRequest userUpdateRequest) {
        // 检查更新请求中的用户ID是否为空
        if (userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "用户ID不能为空");
        }
        // 若更新请求中包含用户名，则进行长度校验
        if (StringUtils.isNotBlank(userUpdateRequest.getUsername()) && userUpdateRequest.getUsername().length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名长度过短，至少需要4位。");
        }
        // 账号特殊字符校验
        if (StringUtils.isNotBlank(userUpdateRequest.getUserAccount())) {
            String validPattern = "[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
            Matcher matcher = Pattern.compile(validPattern).matcher(userUpdateRequest.getUserAccount());
            if (matcher.find()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号包含非法字符。");
            }
        }
        // 若更新请求中包含用户密码，则进行长度校验
        if (StringUtils.isNotBlank(userUpdateRequest.getUserPassword()) && userUpdateRequest.getUserPassword().length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短，至少需要8位。");
        }
        // 若更新请求中包含星球编号，则进行长度校验
        if (StringUtils.isNotBlank(userUpdateRequest.getPlanetCode()) && userUpdateRequest.getPlanetCode().length() > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号过长，不得超过5位。");
        }
        // 账户是否已存在
        if(checkUserAccountExists(userUpdateRequest.getUserAccount())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }
        // 星球编号是否已存在
        if(checkPlanetCodeExists(userUpdateRequest.getPlanetCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号已存在");
        }
        // 构造更新条件
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", userUpdateRequest.getId());
        // 准备更新的用户实体
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        // 如果提供了密码，则进行加密处理
        if (StringUtils.isNotBlank(user.getUserPassword())) {
            String encryptedPassword = DigestUtils.md5DigestAsHex((SALT + user.getUserPassword()).getBytes());
            user.setUserPassword(encryptedPassword);
        }
        // 执行更新操作
        int updateCount = userMapper.update(user, updateWrapper);
        return updateCount > 0;
    }

    /**
     * 根据用户ID删除用户。
     * 调用MyBatis Plus的deleteById方法，根据提供的用户ID执行删除操作。
     * 如果删除操作影响的行数大于0，则返回true表示删除成功；否则返回false表示删除失败。
     * @param id 用户的ID。
     * @return 删除成功返回true，失败返回false。
     */
    @Override
    public boolean deleteUserById(Long id) {
        // 这里简单地调用了MyBatis Plus的内置方法进行删除，实际情况可能需要更复杂的逻辑
        int rows = userMapper.deleteById(id);
        return rows > 0;
    }

    /**
     * 创建用户操作
     * @param createRequest
     * @return
     */
    @Override
    public boolean createUser(UserCreateRequest createRequest) {
        // 校验账户、密码、用户名、星球编号不能为空
        if(StringUtils.isAnyBlank(createRequest.getUserAccount(),createRequest.getUsername(),createRequest.getPlanetCode())){
            throw new BusinessException(ErrorCode.NULL_ERROR, "账户、用户名、星球编号不能为空");
        }
        // 账户是否已存在
        if(checkUserAccountExists(createRequest.getUserAccount())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }
        // 星球编号是否已存在
        if(checkPlanetCodeExists(createRequest.getPlanetCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号已存在");
        }
        // 创建用户对象并设置字段
        User user = new User();
        user.setUserAccount(createRequest.getUserAccount());
        user.setUsername(createRequest.getUsername());
        user.setPlanetCode(createRequest.getPlanetCode());
        user.setAvatarUrl(createRequest.getAvatarUrl());
        user.setGender(createRequest.getGender());
        user.setPhone(createRequest.getPhone());
        user.setEmail(createRequest.getEmail());
        user.setUserRole(createRequest.getUserRole() != null ? createRequest.getUserRole() : UserConstant.DEFAULT_ROLE); // 默认为普通用户，如果未指定角色
        // 密码加密
        String encryptedPassword = DigestUtils.md5DigestAsHex((SALT + createRequest.getUserPassword()).getBytes());
        user.setUserPassword(encryptedPassword);
        return save(user);
    }




    /**
     * 判断账户是否已经存在
     * @param userAccount
     * @return
     */
    private boolean checkUserAccountExists(String userAccount) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        return count(queryWrapper) > 0;
    }

    /**
     * 判断星球编号是否已经存在
     * @param planetCode
     * @return
     */
    private boolean checkPlanetCodeExists(String planetCode) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode);
        return count(queryWrapper) > 0;
    }
}





