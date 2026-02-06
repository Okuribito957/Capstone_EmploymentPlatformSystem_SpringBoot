package top.neu.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.neu.framework.exception.TokenException;
import top.neu.framework.redis.RedisUtil;
import top.neu.utils.Status;
import top.neu.utils.UserThreadLocal;
import top.neu.vo.UserData;

@Service
public class UserDataService {

    @Autowired
    private RedisUtil redisUtil;

    public UserData getUser() {
        String token = UserThreadLocal.get();
        UserData userData = (UserData) redisUtil.get(token);
        if(userData != null) {
            return userData;
        } else {
            throw new TokenException(Status.TOKEN_ERROR.getMsg());
        }
    }
}
