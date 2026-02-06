package top.neu.framework.turnstile;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class TurnstileSiteverifyResponse {

    private boolean success;

    @JsonProperty("error-codes")
    private List<String> errorCodes;

    @JsonProperty("challenge_ts")
    private String challengeTs;

    private String hostname;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<String> getErrorCodes() {
        return errorCodes;
    }

    public void setErrorCodes(List<String> errorCodes) {
        this.errorCodes = errorCodes;
    }

    public String getChallengeTs() {
        return challengeTs;
    }

    public void setChallengeTs(String challengeTs) {
        this.challengeTs = challengeTs;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}
