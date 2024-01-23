package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;
    //微信服务接口地址
    public static final String WX_LOGIN ="https://api.weixin.qq.com/sns/jscode2session";
    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        String openid = getOpenid(userLoginDTO.getCode());
        //判断openid是否为空,空抛出异常
        if (openid==null){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        //判断是否为新用户
        User user = userMapper.getByOpenid(openid);
        //如果是新用户，自动完成注册
        if(user==null){
            user=User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }
        return user;
    }
    private String getOpenid(String code){
        //调用微信服务接口，获取openid
        HashMap<String,String> map = new HashMap<>();
        map.put("appid",weChatProperties.getAppid());
        map.put("secret",weChatProperties.getSecret());
        map.put("js_code",code);
        map.put("grant_type","authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, map);
        JSONObject jsonObject= JSON.parseObject(json);
        String openid=jsonObject.getString("openid");
        return openid;
    }
}
