package top.neu.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import top.neu.framework.turnstile.TurnstileVerifier;
import top.neu.entity.Company;
import top.neu.entity.Student;
import top.neu.entity.User;
import top.neu.framework.redis.RedisUtil;
import top.neu.framework.role.Role;
import top.neu.service.CompanyService;
import top.neu.service.StudentService;
import top.neu.service.UserService;
import top.neu.utils.Result;
import top.neu.vo.UserData;

import javax.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.UUID;

@RestController
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @Value("${turnstile.log-details:false}")
    private boolean turnstileLogDetails;

    @Value("${turnstile.debug-error-codes:false}")
    private boolean debugErrorCodes;

    @Autowired
    private UserService userService;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private StudentService studentService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private TurnstileVerifier turnstileVerifier;

    @PostMapping("/login")
    public Result login(@RequestBody Map<String,String> map, HttpServletRequest request) {
        String turnstileToken = map.get("turnstileToken");
        if (turnstileToken == null || turnstileToken.trim().isEmpty()) {
            // also accept implicit rendering default field name
            turnstileToken = map.get("cf-turnstile-response");
        }
        String clientIp = getClientIp(request);
        if (turnstileLogDetails) {
            log.info("Login Turnstile: ip={}, host={}, hasToken={}, type={}",
                    clientIp,
                    request != null ? request.getHeader("Host") : null,
                    turnstileToken != null && !turnstileToken.trim().isEmpty(),
                    map.get("type"));
        }

        TurnstileVerifier.VerificationResult verifyResult = turnstileVerifier.verify(turnstileToken, clientIp);
        if (!verifyResult.isSuccess()) {
            if (turnstileLogDetails) {
                log.info("Login Turnstile failed: ip={}, errorCodes={}", clientIp, verifyResult.getErrorCodes());
            }
            if (verifyResult.getErrorCodes() != null && verifyResult.getErrorCodes().contains("missing-input-secret")) {
                return Result.error("Turnstile 未配置（缺少 secret）");
            }
            if (verifyResult.getErrorCodes() != null && verifyResult.getErrorCodes().contains("missing-input-response")) {
                return Result.error("请先完成人机验证");
            }
            if (debugErrorCodes) {
                return Result.error("人机验证失败: " + verifyResult.getErrorCodes());
            }
            return Result.error("人机验证失败");
        }

        String account = map.get("account");
        String password = map.get("password");
        String type = map.get("type");

        boolean flag = false;
        UserData userData = new UserData();

        if(Role.ADMIN.getCode().equals(Integer.parseInt(type))) {
            //管理员登录
            User user = userService.login(account, password);
            if(user != null) {
                flag = true;
                userData.setId(user.getId());
                userData.setAccount(user.getUserName());
                userData.setName(user.getName());
                userData.setType(Role.ADMIN.getCode());
            }
        }
        if(Role.COMPANY.getCode().equals(Integer.parseInt(type))) {
            //企业登录
            Company company = companyService.login(account, password);
            if(company != null) {
                flag = true;
                userData.setId(company.getId());
                userData.setAccount(company.getAccount());
                userData.setName(company.getName());
                userData.setType(Role.COMPANY.getCode());
            }
        }
        if(Role.STUDENT.getCode().equals(Integer.parseInt(type))) {
            //学生登录
            Student student = studentService.login(account, password);
            if(student != null) {
                flag = true;
                userData.setId(student.getId());
                userData.setAccount(student.getAccount());
                userData.setName(student.getName());
                userData.setType(Role.STUDENT.getCode());
            }
        }

        if(flag) {
            //登录成功
            //1、生成token
            String token = UUID.randomUUID().toString();
            //2、存入redis
            userData.setToken(token);
            redisUtil.set(token, userData, RedisUtil.EXPR);
            //3、响应数据
            return Result.success(userData);
        } else {
            return Result.error("用户名或密码错误");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.trim().isEmpty()) {
            // first IP in the list is the original client
            int commaIndex = xff.indexOf(',');
            return (commaIndex > 0 ? xff.substring(0, commaIndex) : xff).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.trim().isEmpty()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
