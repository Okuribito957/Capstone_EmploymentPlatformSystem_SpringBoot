package top.neu.front;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.neu.framework.redis.RedisUtil;
import top.neu.framework.sms.SmsVerifier;
import top.neu.utils.Result;

import java.util.Map;

/**
 * 短信验证码前台控制器。
 *
 * <p>路径 /f/sms/* 属于 TokenInterceptor 的排除范围（/f/**），
 * 无需登录即可访问。
 *
 * <p>接口列表：
 * <ul>
 *   <li>POST /f/sms/send_code  - 发送验证码</li>
 *   <li>POST /f/sms/check_code - 核验验证码</li>
 * </ul>
 *
 * <p>templateCode 对应关系（详见 SmsVerifier）：
 * <ul>
 *   <li>100001 - 登录/注册</li>
 *   <li>100002 - 修改绑定手机号</li>
 *   <li>100003 - 重置密码</li>
 * </ul>
 */
@RestController
@RequestMapping("/f/sms")
public class SmsFrontController {

    private static final Logger log = LoggerFactory.getLogger(SmsFrontController.class);

    /** 同一手机号的重发冷却时间（秒） */
    private static final long SEND_COOLDOWN_SECONDS = 60L;

    /** Redis 键前缀：重发限流标志 */
    private static final String REDIS_KEY_PREFIX_COOLDOWN = "sms:cooldown:";

    /** 默认 templateCode（登录/注册） */
    private static final String DEFAULT_TEMPLATE_CODE = "100001";

    @Value("${aliyun.sms.log-details:false}")
    private boolean logDetails;

    @Autowired
    private SmsVerifier smsVerifier;

    @Autowired
    private RedisUtil redisUtil;

    // -------------------------------------------------------
    // 接口：发送验证码
    // -------------------------------------------------------

    /**
     * 向指定手机号发送 SMS 验证码。
     *
     * <p>请求体：
     * <pre>
     * {
     *   "phoneNumber": "13800138000",
     *   "templateCode": "100001"   // 可省略，默认 100001（登录/注册）
     * }
     * </pre>
     *
     * <p>同一手机号在 {SEND_COOLDOWN_SECONDS} 秒内不允许重复发送。
     */
    @PostMapping("/send_code")
    public Result sendCode(@RequestBody Map<String, String> body) {
        // --- 入参校验 ---
        String phoneNumber = body.get("phoneNumber");
        if (!StringUtils.hasText(phoneNumber)) {
            return Result.error("手机号不能为空");
        }
        if (!isValidPhoneNumber(phoneNumber)) {
            return Result.error("手机号格式不正确");
        }

        // templateCode 未传时使用默认值
        String templateCode = body.get("templateCode");
        if (!StringUtils.hasText(templateCode)) {
            templateCode = DEFAULT_TEMPLATE_CODE;
        }

        // --- Redis 限流检查 ---
        String cooldownKey = REDIS_KEY_PREFIX_COOLDOWN + phoneNumber;
        if (redisUtil.hasKey(cooldownKey)) {
            long remainSeconds = redisUtil.getExpire(cooldownKey);
            if (logDetails) {
                log.info("SmsFrontController.sendCode: 限流中 phoneNumber={}, remainSeconds={}",
                        phoneNumber, remainSeconds);
            }
            return Result.error("发送过于频繁，请 " + remainSeconds + " 秒后再试");
        }

        // --- 调用阿里云 SMS API ---
        SmsVerifier.SmsResult result = smsVerifier.sendCode(phoneNumber, templateCode);

        if (result.isSuccess()) {
            // 发送成功后写入 Redis 冷却键
            redisUtil.set(cooldownKey, "1", SEND_COOLDOWN_SECONDS);
            if (logDetails) {
                log.info("SmsFrontController.sendCode: 发送成功 phoneNumber={}", phoneNumber);
            }
            return Result.success();
        } else {
            log.warn("SmsFrontController.sendCode: 发送失败 phoneNumber={}, errorCode={}, msg={}",
                    phoneNumber, result.getErrorCode(), result.getErrorMessage());
            return Result.error("验证码发送失败：" + result.getErrorMessage());
        }
    }

    // -------------------------------------------------------
    // 接口：核验验证码
    // -------------------------------------------------------

    /**
     * 将用户输入的验证码提交至阿里云进行核验。
     *
     * <p>请求体：
     * <pre>
     * {
     *   "phoneNumber": "13800138000",
     *   "code": "1234"
     * }
     * </pre>
     */
    @PostMapping("/check_code")
    public Result checkCode(@RequestBody Map<String, String> body) {
        // --- 入参校验 ---
        String phoneNumber = body.get("phoneNumber");
        String code = body.get("code");

        if (!StringUtils.hasText(phoneNumber)) {
            return Result.error("手机号不能为空");
        }
        if (!StringUtils.hasText(code)) {
            return Result.error("验证码不能为空");
        }

        if (logDetails) {
            log.info("SmsFrontController.checkCode: 开始核验 phoneNumber={}", phoneNumber);
        }

        // --- 调用阿里云 SMS API ---
        SmsVerifier.SmsResult result = smsVerifier.checkCode(phoneNumber, code);

        if (result.isSuccess()) {
            if (logDetails) {
                log.info("SmsFrontController.checkCode: 核验成功 phoneNumber={}", phoneNumber);
            }
            return Result.success();
        } else {
            log.warn("SmsFrontController.checkCode: 核验失败 phoneNumber={}, errorCode={}, msg={}",
                    phoneNumber, result.getErrorCode(), result.getErrorMessage());
            return Result.error("验证码错误：" + result.getErrorMessage());
        }
    }

    // -------------------------------------------------------
    // 工具方法
    // -------------------------------------------------------

    /**
     * 手机号简单格式校验（中国大陆 11 位格式）。
     *
     * @param phoneNumber 待校验的手机号
     * @return 格式正确返回 true
     */
    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber.matches("^1[3-9]\\d{9}$");
    }
}
