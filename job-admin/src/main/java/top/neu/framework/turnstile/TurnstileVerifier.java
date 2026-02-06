package top.neu.framework.turnstile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Component
public class TurnstileVerifier {

    private static final Logger log = LoggerFactory.getLogger(TurnstileVerifier.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${turnstile.enabled:true}")
    private boolean enabled;

    @Value("${turnstile.secret:}")
    private String secret;

    @Value("${turnstile.siteverify-url:https://challenges.cloudflare.com/turnstile/v0/siteverify}")
    private String siteverifyUrl;

    @Value("${turnstile.log-details:false}")
    private boolean logDetails;

    @Value("${turnstile.send-remoteip:true}")
    private boolean sendRemoteIp;

    public VerificationResult verify(String token, String remoteIp) {
        if (!enabled) {
            return VerificationResult.ok();
        }

        if (logDetails) {
            log.info("Turnstile verify start: siteverifyUrl={}, remoteIp={}, hasSecret={}, tokenLength={}",
                    siteverifyUrl,
                    remoteIp,
                    secret != null && !secret.trim().isEmpty(),
                    token == null ? 0 : token.length());
        }

        if (secret == null || secret.trim().isEmpty()) {
            return VerificationResult.fail(Collections.singletonList("missing-input-secret"));
        }

        if (token == null || token.trim().isEmpty()) {
            return VerificationResult.fail(Collections.singletonList("missing-input-response"));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secret);
        form.add("response", token);
        if (sendRemoteIp && remoteIp != null && !remoteIp.trim().isEmpty()) {
            form.add("remoteip", remoteIp);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<TurnstileSiteverifyResponse> response = restTemplate.postForEntity(
                    siteverifyUrl,
                    request,
                    TurnstileSiteverifyResponse.class
            );
            TurnstileSiteverifyResponse body = response.getBody();
            if (body != null && body.isSuccess()) {
                if (logDetails) {
                    log.info("Turnstile verify success: hostname={}, challengeTs={}", body.getHostname(), body.getChallengeTs());
                }
                return VerificationResult.ok();
            }
            List<String> errorCodes = body != null ? body.getErrorCodes() : Collections.singletonList("invalid-response");
            if (logDetails) {
                log.info("Turnstile verify failed: httpStatus={}, errorCodes={}, hostname={}, challengeTs={}",
                        response.getStatusCodeValue(),
                        errorCodes,
                        body != null ? body.getHostname() : null,
                        body != null ? body.getChallengeTs() : null);
            }
            return VerificationResult.fail(errorCodes);
        } catch (RestClientException e) {
            log.warn("Turnstile siteverify call failed: {}", e.getMessage(), e);
            return VerificationResult.fail(Collections.singletonList("siteverify-unreachable"));
        }
    }

    public static class VerificationResult {
        private final boolean success;
        private final List<String> errorCodes;

        private VerificationResult(boolean success, List<String> errorCodes) {
            this.success = success;
            this.errorCodes = errorCodes;
        }

        public static VerificationResult ok() {
            return new VerificationResult(true, Collections.emptyList());
        }

        public static VerificationResult fail(List<String> errorCodes) {
            return new VerificationResult(false, errorCodes == null ? Collections.emptyList() : errorCodes);
        }

        public boolean isSuccess() {
            return success;
        }

        public List<String> getErrorCodes() {
            return errorCodes;
        }
    }
}
