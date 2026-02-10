package top.neu.front;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.neu.framework.turnstile.TurnstileVerifier;
import top.neu.entity.Company;
import top.neu.entity.Student;
import top.neu.service.CompanyService;
import top.neu.service.StudentService;
import top.neu.utils.Result;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/f/register/")
public class RegisterFrontController {

    private static final Logger log = LoggerFactory.getLogger(RegisterFrontController.class);

    @Value("${turnstile.log-details:false}")
    private boolean turnstileLogDetails;

    @Value("${turnstile.debug-error-codes:false}")
    private boolean debugErrorCodes;

    @Autowired
    private StudentService studentService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private TurnstileVerifier turnstileVerifier;

    @PostMapping("student_create")
    public Result student_create(@RequestBody Map<String, Object> requestData, HttpServletRequest request) {
        String turnstileToken = (String) requestData.get("turnstileToken");
        if (turnstileToken == null || turnstileToken.trim().isEmpty()) {
            turnstileToken = (String) requestData.get("cf-turnstile-response");
        }
        String clientIp = getClientIp(request);
        if (turnstileLogDetails) {
            log.info("Student Register Turnstile: ip={}, hasToken={}",
                    clientIp,
                    turnstileToken != null && !turnstileToken.trim().isEmpty());
        }

        TurnstileVerifier.VerificationResult verifyResult = turnstileVerifier.verify(turnstileToken, clientIp);
        if (!verifyResult.isSuccess()) {
            if (turnstileLogDetails) {
                log.info("Student Register Turnstile failed: ip={}, errorCodes={}", clientIp, verifyResult.getErrorCodes());
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

        Student student = new Student();
        student.setName((String) requestData.get("name"));
        student.setAccount((String) requestData.get("account"));
        student.setPassword((String) requestData.get("password"));
        Object bdObj = requestData.get("birthday");
        java.util.Date birthday = null;
        if (bdObj instanceof java.util.Date) {
            birthday = (java.util.Date) bdObj;
        } else if (bdObj instanceof Number) {
            birthday = new java.util.Date(((Number) bdObj).longValue());
        } else if (bdObj instanceof String) {
            String s = ((String) bdObj).trim();
            if (!s.isEmpty()) {
                Exception lastEx = null;
                String[] patterns = {"yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd"};
                for (String p : patterns) {
                    try {
                        birthday = new java.text.SimpleDateFormat(p).parse(s);
                        break;
                    } catch (java.text.ParseException e) {
                        lastEx = e;
                    }
                }
                if (birthday == null && turnstileLogDetails) {
                    log.info("Failed to parse birthday '{}' : {}", s, lastEx == null ? "unknown" : lastEx.getMessage());
                }
            }
        }
        student.setBirthday(birthday);
        student.setCollege((String) requestData.get("college"));
        student.setPhone((String) requestData.get("phone"));

        Student param = new Student();
        param.setAccount(student.getAccount());
        int count = studentService.count(param);
        if (count > 0) {
            return Result.error("注册失败，账号已存在！");
        } else {
            int flag = studentService.create(student);
            if (flag > 0) {
                return Result.success();
            } else {
                return Result.error();
            }
        }
    }

    @PostMapping("company_create")
    public Result company_create(@RequestBody Map<String, Object> requestData, HttpServletRequest request) {
        String turnstileToken = (String) requestData.get("turnstileToken");
        if (turnstileToken == null || turnstileToken.trim().isEmpty()) {
            turnstileToken = (String) requestData.get("cf-turnstile-response");
        }
        String clientIp = getClientIp(request);
        if (turnstileLogDetails) {
            log.info("Company Register Turnstile: ip={}, hasToken={}",
                    clientIp,
                    turnstileToken != null && !turnstileToken.trim().isEmpty());
        }

        TurnstileVerifier.VerificationResult verifyResult = turnstileVerifier.verify(turnstileToken, clientIp);
        if (!verifyResult.isSuccess()) {
            if (turnstileLogDetails) {
                log.info("Company Register Turnstile failed: ip={}, errorCodes={}", clientIp, verifyResult.getErrorCodes());
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

        Company company = new Company();
        company.setName((String) requestData.get("name"));
        company.setAccount((String) requestData.get("account"));
        company.setPassword((String) requestData.get("password"));
        company.setContact((String) requestData.get("contact"));
        company.setTelephone((String) requestData.get("telephone"));

        Company param = new Company();
        param.setAccount(company.getAccount());
        int count = companyService.count(param);
        if (count > 0) {
            return Result.error("注册失败，账号已存在！");
        } else {
            int flag = companyService.create(company);
            if (flag > 0) {
                return Result.success();
            } else {
                return Result.error();
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.trim().isEmpty()) {
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
