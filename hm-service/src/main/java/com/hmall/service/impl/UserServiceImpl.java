package com.hmall.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.common.exception.BadRequestException;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.exception.ForbiddenException;
import com.hmall.common.utils.UserContext;
import com.hmall.config.JwtProperties;
import com.hmall.domain.dto.LoginFormDTO;
import com.hmall.domain.po.User;
import com.hmall.domain.vo.UserLoginVO;
import com.hmall.enums.UserStatus;
import com.hmall.mapper.UserMapper;
import com.hmall.service.IUserService;
import com.hmall.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final PasswordEncoder passwordEncoder;

    private final JwtTool jwtTool;

    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;

    @Override
    public UserLoginVO login(LoginFormDTO loginDTO) {
        //1.判断用户名、密码是否为空
        if(loginDTO.getUsername() == null || loginDTO.getPassword() == null){
            throw new BadRequestException("用户名或密码不能为空");
        }
        String pas = passwordEncoder.encode("123");
        log.info("pas={}", pas);
        //2.判断用户名密码是否正确
        User user = lambdaQuery()
                .eq(User::getUsername, loginDTO.getUsername())
                .one();
        if(user == null){
            throw new BadRequestException("用户名或密码错误");
        }
        //3.判断用户是否激活
        if(user.getStatus() == UserStatus.FROZEN){
            throw new BadRequestException("用户被禁止使用");
        }
        //4.密码校验
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())){
            throw new BadRequestException("用户名或密码错误");
        }

        //4.生成token
        String token = jwtTool.createToken(user.getId(), jwtProperties.getTokenTTL());

        UserLoginVO vo = new UserLoginVO();
        vo.setUserId(user.getId());
        vo.setToken(token);
        vo.setUsername(user.getUsername());
        vo.setBalance(user.getBalance());
        return vo;
    }

    @Override
    public void deductMoney(String pw, Integer totalFee) {
        log.info("开始扣款");
        // 1.校验密码
        User user = getById(UserContext.getUser());
        if(user == null || !passwordEncoder.matches(pw, user.getPassword())){
            // 密码错误
            throw new BizIllegalException("用户密码错误");
        }

        // 2.尝试扣款
        try {
            baseMapper.updateMoney(UserContext.getUser(), totalFee);
        } catch (Exception e) {
            throw new RuntimeException("扣款失败，可能是余额不足！", e);
        }
        log.info("扣款成功");
    }
}
