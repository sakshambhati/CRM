package com.crm.dto;

public class TokenResponse {
    private String access_token;
    private String refresh_token;
    private String expires_in;
    private String refresh_expires_in;
    private String token_type;
    private String session_state;
    private String state;
    public String getAccess_token() {
        return access_token;
    }
    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }
    public String getRefresh_token() {
        return refresh_token;
    }
    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }
    public String getExpires_in() {
        return expires_in;
    }
    public void setExpires_in(String expires_in) {
        this.expires_in = expires_in;
    }
    public String getRefresh_expires_in() {
        return refresh_expires_in;
    }
    public void setRefresh_expires_in(String refresh_expires_in) {
        this.refresh_expires_in = refresh_expires_in;
    }
    public String getToken_type() {
        return token_type;
    }
    public void setToken_type(String token_type) {
        this.token_type = token_type;
    }
    public String getSession_state() {
        return session_state;
    }
    public void setSession_state(String session_state) {
        this.session_state = session_state;
    }
    public String getState() {
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }
    @Override
    public String toString() {
        return "TokenResponse [access_token=" + access_token + ", refresh_token=" + refresh_token + ", expires_in="
                + expires_in + ", refresh_expires_in=" + refresh_expires_in + ", token_type=" + token_type
                + ", session_state=" + session_state + ", state=" + state + "]";
    }


}