package com.brotherc.aquant.auth.model.vo;

import lombok.Data;

@Data
public class LoginRespVO {

    private String token;
    private String nickname;
    private String username;

}
