package com.brotherc.aquant.model.vo.auth;

import lombok.Data;

@Data
public class LoginRespVO {

    private String token;
    private String nickname;
    private String username;

}
