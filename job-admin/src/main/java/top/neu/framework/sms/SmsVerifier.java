package top.neu.framework.sms;

import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

/**
 * 阿里云 Dypnsapi SMS 验证组件。
 * 按照与TurnstileVerifier相同的模式实现。
 *
 * <p>客户端认证模式：
 * <ol>
 *   <li>aliyun.sms.access-key-id / access-key-secret 都已设置 → 显式AK模式</li>
 *   <li>其中任何一个未设置 → 环境变量链（默认凭证链）回退</li>
 * </ol>
 */
@Component
public class SmsVerifier {

    private static final Logger log = LoggerFactory.getLogger(SmsVerifier.class);

    /** 阿里云 Dypnsapi 服务的端点 */
    private static final String ENDPOINT = "dypnsapi.aliyuncs.com";

    private static final String SIGN_NAME = "速通互联验证码"; // 签名名称，必须与阿里云控制台中创建的签名名称完全匹配


    // -------------------------------------------------------
    // YML
    // -------------------------------------------------------

    /** SMS功能的启用/禁用开关。设置为false时始终返回成功（用于本地开发） */
    @Value("${aliyun.sms.enabled:true}")
    private boolean enabled;

    /** 阿里云 AccessKeyId（可选。未设置时使用环境变量链） */
    @Value("${aliyun.sms.access-key-id:}")
    private String accessKeyId;

    /** 阿里云 AccessKeySecret（可选。未设置时使用环境变量链） */
    @Value("${aliyun.sms.access-key-secret:}")
    private String accessKeySecret;

    /** 详细日志输出标志（生产环境建议设为false） */
    @Value("${aliyun.sms.log-details:false}")
    private boolean logDetails;

    // -------------------------------------------------------
    // 内部状态
    // -------------------------------------------------------

    /** 已初始化的 Dypnsapi 客户端 */
    private com.aliyun.dypnsapi20170525.Client client;

    // -------------------------------------------------------
    // 初期化
    // -------------------------------------------------------

    /**
     * 在Spring Bean初始化时生成SDK客户端。
     * 根据AK配置的有无自动切换认证模式。
     */
    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("SmsVerifier: SMS功能已禁用（aliyun.sms.enabled=false）");
            return;
        }
        try {
            Config config = buildConfig();
            config.endpoint = ENDPOINT;
            this.client = new com.aliyun.dypnsapi20170525.Client(config);
            log.info("SmsVerifier: 客户端初始化完成。模式={}",
                    isExplicitAkConfigured() ? "显式AK" : "环境变量链");
        } catch (Exception e) {
            log.error("SmsVerifier: クライアント初期化失敗: {}", e.getMessage(), e);
        }
    }

    /**
     * 根据AK配置的有无构建认证Config。
     *
     * @return 已配置的 {@link Config}
     * @throws Exception SDK配置生成过程中的异常
     */
    private Config buildConfig() throws Exception {
        if (isExplicitAkConfigured()) {
            log.info("SmsVerifier: 以显式AK模式初始化（accessKeyId={}****）",
                    accessKeyId.substring(0, Math.min(4, accessKeyId.length())));
            return new Config()
                    .setAccessKeyId(accessKeyId)
                    .setAccessKeySecret(accessKeySecret);
        } else {
            log.info("SmsVerifier: 以环境变量链（默认凭证链）模式初始化");
            com.aliyun.credentials.Client credential = new com.aliyun.credentials.Client();
            return new Config().setCredential(credential);
        }
    }

    /**
     * 判定YML中是否设置了显式的AK。
     */
    private boolean isExplicitAkConfigured() {
        return StringUtils.hasText(accessKeyId) && StringUtils.hasText(accessKeySecret);
    }

    // -------------------------------------------------------
    // 公共API
    // -------------------------------------------------------

    /**
     * templateCode说明
     * 
     * 100001: 登录/注册模板(您的验证码为${code}。尊敬的客户，以上验证码${min}分钟内有效，请注意保密，切勿告知他人。)
     * 100002: 修改绑定手机号模板(尊敬的客户，您正在进行修改手机号操作，您的验证码为${code}。以上验证码${min}分钟内有效，请注意保密，切勿告知他人。)
     * 100003: 重置密码模板(尊敬的客户，您正在进行重置密码操作，您的验证码为${code}。以上验证码${min}分钟内有效，请注意保密，切勿告知他人。)
     */

    /**
     * 向指定的电话号码发送SMS验证码。
     *
     * @param phoneNumber 发送目标的手机号码
     * @return 表示发送结果的 {@link SmsResult}
     */
    public SmsResult sendCode(String phoneNumber, String templateCode) {
        if (!enabled) {
            log.info("SmsVerifier.sendCode: SMS禁用模式，返回phoneNumber={}的虚拟成功", phoneNumber);
            return SmsResult.ok();
        }
        if (client == null) {
            log.error("SmsVerifier.sendCode: 客户端未初始化。请检查配置");
            return SmsResult.fail("SMS_CLIENT_INIT_FAILED", "SMS客户端初始化失败");
        }

        if (logDetails) {
            log.info("SmsVerifier.sendCode: 发送开始 phoneNumber={}", phoneNumber);
        }

        try {
            com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest req =
                    new com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest()
                            .setPhoneNumber(phoneNumber);
            // 必填参数
            req.setSignName(SIGN_NAME); // 签名名称，必须与阿里云控制台中创建的签名名称完全匹配
            req.setTemplateCode(templateCode);
            req.setTemplateParam("{\"code\":\"##code##\",\"min\":\"5\"}");
            req.setCodeType(1L); // 验证码类型，1表示数字验证码
            req.setCodeLength(4L);// 验证码长度，4表示4位验证码

            client.sendSmsVerifyCodeWithOptions(req, new RuntimeOptions());

            if (logDetails) {
                log.info("SmsVerifier.sendCode: 发送成功 phoneNumber={}", phoneNumber);
            }
            return SmsResult.ok();

        } catch (TeaException e) {
            log.warn("SmsVerifier.sendCode: 阿里云API错误 code={}, msg={}", e.getCode(), e.getMessage());
            return SmsResult.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.warn("SmsVerifier.sendCode: 意外错误: {}", e.getMessage(), e);
            return SmsResult.fail("UNKNOWN_ERROR", e.getMessage());
        }
    }

    /**
     * 对指定的电话号码和验证码进行效验（验证）。
     *
     * @param phoneNumber 电话号码
     * @param code        用户输入的验证码
     * @return 表示验证结果的 {@link SmsResult}
     */
    public SmsResult checkCode(String phoneNumber, String code) {
        if (!enabled) {
            log.info("SmsVerifier.checkCode: SMS禁用模式，返回phoneNumber={}的虚拟成功", phoneNumber);
            return SmsResult.ok();
        }
        if (client == null) {
            log.error("SmsVerifier.checkCode: 客户端未初始化");
            return SmsResult.fail("SMS_CLIENT_INIT_FAILED", "SMS客户端初始化失败");
        }

        if (logDetails) {
            log.info("SmsVerifier.checkCode: 验证开始 phoneNumber={}", phoneNumber);
        }

        try {
            com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest req =
                    new com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest()
                            .setPhoneNumber(phoneNumber)
                            .setVerifyCode(code);

            client.checkSmsVerifyCodeWithOptions(req, new RuntimeOptions());

            if (logDetails) {
                log.info("SmsVerifier.checkCode: 验证成功 phoneNumber={}", phoneNumber);
            }
            return SmsResult.ok();

        } catch (TeaException e) {
            log.warn("SmsVerifier.checkCode: 验证失败 code={}, msg={}", e.getCode(), e.getMessage());
            return SmsResult.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.warn("SmsVerifier.checkCode: 意外错误: {}", e.getMessage(), e);
            return SmsResult.fail("UNKNOWN_ERROR", e.getMessage());
        }
    }

    // -------------------------------------------------------
    // 结果类
    // -------------------------------------------------------

    /**
     * 表示SMS操作结果的不可变值对象。
     */
    public static class SmsResult {

        private final boolean success;
        private final String errorCode;
        private final String errorMessage;

        private SmsResult(boolean success, String errorCode, String errorMessage) {
            this.success = success;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        /** 生成成功结果 */
        public static SmsResult ok() {
            return new SmsResult(true, null, null);
        }

        /** 生成失败结果 */
        public static SmsResult fail(String errorCode, String errorMessage) {
            return new SmsResult(false, errorCode, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
